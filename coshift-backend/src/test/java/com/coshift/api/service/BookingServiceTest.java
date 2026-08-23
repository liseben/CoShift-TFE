package com.coshift.api.service;

import com.coshift.api.dto.BookingRequest;
import com.coshift.api.entity.Booking;
import com.coshift.api.entity.BookingStatus;
import com.coshift.api.entity.Trip;
import com.coshift.api.entity.TripStatus;
import com.coshift.api.entity.User;
import com.coshift.api.entity.Vehicule;
import com.coshift.api.exception.BadRequestException;
import com.coshift.api.exception.ConflictException;
import com.coshift.api.exception.NoSeatsAvailableException;
import com.coshift.api.exception.ResourceNotFoundException;
import com.coshift.api.exception.UnauthorizedException;
import com.coshift.api.repository.BookingRepository;
import com.coshift.api.repository.ReviewRepository;
import com.coshift.api.repository.TripRepository;
import com.coshift.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Règles du cycle de vie des réservations.
 *
 * <p>Le point sensible de ce service est le décompte des places. Une place est
 * une ressource finie que deux personnes peuvent réclamer en même temps : la
 * demande ne consomme rien, seule l'acceptation débite, et l'annulation
 * recrédite — mais seulement si la réservation avait effectivement débité.
 * Chacune de ces trois assertions est vérifiée ci-dessous, parce qu'une erreur
 * sur l'une d'elles produit soit des places fantômes, soit des passagers sans
 * siège.</p>
 *
 * <p>Aucun contexte Spring n'est démarré : les dépôts sont simulés. La suite
 * complète s'exécute en moins d'une seconde, contre soixante-dix secondes pour
 * un seul {@code @SpringBootTest}.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BookingService — réservations")
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private TripRepository tripRepository;
    @Mock private UserRepository userRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private EmailService emailService;
    @Mock private Messages messages;
    @Mock private OrganizationService organizationService;
    @Mock private PaymentService paymentService;

    @InjectMocks private BookingService service;

    private User conducteur;
    private User passager;
    private Trip trajet;

    private static final String COURRIEL_CONDUCTEUR = "conducteur@coshift.be";
    private static final String COURRIEL_PASSAGER = "passager@coshift.be";

    @BeforeEach
    void preparer() {
        conducteur = utilisateur(1L, COURRIEL_CONDUCTEUR);
        passager = utilisateur(2L, COURRIEL_PASSAGER);

        /* Par defaut, tout le monde partage le cercle : les cas de cette
           classe portent sur les regles de reservation, pas sur la visibilite.
           Le cercle a ses propres tests, ou cette reponse est renversee. */
        when(organizationService.partageLeCercle(any(), any())).thenReturn(true);

        /* Le vehicule n'est pas decoratif : trips.vehicule_id est NOT NULL et
           BookingResponse lit sa marque et son modele. Un trajet sans vehicule
           ne peut pas exister en base, le jeu d'essai doit donc en porter un. */
        Vehicule vehicule = Vehicule.builder()
                .id(20L)
                .uuid("vehicule-uuid")
                .brand("Renault")
                .model("Clio")
                .seats(4)
                .owner(conducteur)
                .build();

        trajet = Trip.builder()
                .id(10L)
                .uuid("trajet-uuid")
                .departureCity("Namur")
                .arrivalCity("Bruxelles")
                .departureTime(LocalDateTime.now().plusDays(1))
                .availableSeats(3)
                .pricePerSeat(new BigDecimal("5.00"))
                .status(TripStatus.PLANNED)
                .driver(conducteur)
                .vehicule(vehicule)
                .build();

        when(userRepository.findByEmail(COURRIEL_CONDUCTEUR)).thenReturn(Optional.of(conducteur));
        when(userRepository.findByEmail(COURRIEL_PASSAGER)).thenReturn(Optional.of(passager));
        when(tripRepository.findByUuid("trajet-uuid")).thenReturn(Optional.of(trajet));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
        when(tripRepository.save(any(Trip.class))).thenAnswer(i -> i.getArgument(0));
        when(messages.get(anyString())).thenReturn("message");
        when(reviewRepository.reservationsDejaNoteesPar(anyLong())).thenReturn(java.util.List.of());
    }

    // ─────────────────────────── F27 — Demander une place ───────────────────────

    @Nested
    @DisplayName("Demander une réservation")
    class Demander {

        @Test
        @DisplayName("refuse de réserver son propre trajet")
        void refuseSonPropreTrajet() {
            assertThatThrownBy(() -> service.book(COURRIEL_CONDUCTEUR, demande(1)))
                    .isInstanceOf(BadRequestException.class);

            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("refuse un trajet qui n'est plus ouvert")
        void refuseTrajetFerme() {
            trajet.setStatus(TripStatus.CANCELLED);

            assertThatThrownBy(() -> service.book(COURRIEL_PASSAGER, demande(1)))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("refuse à moins d'une heure du départ")
        void refuseTropPresDuDepart() {
            trajet.setDepartureTime(LocalDateTime.now().plusMinutes(30));

            assertThatThrownBy(() -> service.book(COURRIEL_PASSAGER, demande(1)))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("refuse une seconde demande sur le même trajet")
        void refuseDoublon() {
            when(bookingRepository.existsByTripIdAndPassengerIdAndStatusIn(
                    anyLong(), anyLong(), anyList())).thenReturn(true);

            assertThatThrownBy(() -> service.book(COURRIEL_PASSAGER, demande(1)))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("refuse plus de places qu'il n'en reste")
        void refuseSurReservation() {
            assertThatThrownBy(() -> service.book(COURRIEL_PASSAGER, demande(4)))
                    .isInstanceOf(NoSeatsAvailableException.class);
        }

        @Test
        @DisplayName("crée une demande en attente, sans consommer de place")
        void creeUneDemandeEnAttente() {
            var reponse = service.book(COURRIEL_PASSAGER, demande(2));

            assertThat(reponse.getStatus()).isEqualTo(BookingStatus.PENDING);
            // Le trajet n'est pas débité : une demande en attente ne doit pas
            // immobiliser une place qui pourrait profiter à quelqu'un d'autre.
            assertThat(trajet.getAvailableSeats()).isEqualTo(3);
            verify(tripRepository, never()).save(any());
        }

        @Test
        @DisplayName("calcule le total à partir du prix par place")
        void calculeLeTotal() {
            var reponse = service.book(COURRIEL_PASSAGER, demande(3));

            assertThat(reponse.getTotalPrice()).isEqualByComparingTo(new BigDecimal("15.00"));
        }

        @Test
        @DisplayName("refuse un trajet inconnu")
        void refuseTrajetInconnu() {
            when(tripRepository.findByUuid("inexistant")).thenReturn(Optional.empty());
            BookingRequest r = new BookingRequest();
            r.setTripUuid("inexistant");
            r.setSeatsBooked(1);

            assertThatThrownBy(() -> service.book(COURRIEL_PASSAGER, r))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ──────────────────────── F20 — Accepter ou refuser ─────────────────────────

    @Nested
    @DisplayName("Accepter une réservation")
    class Accepter {

        @Test
        @DisplayName("débite les places du trajet")
        void debiteLesPlaces() {
            reservation(BookingStatus.PENDING, 2);

            service.accept(COURRIEL_CONDUCTEUR, "resa-uuid");

            assertThat(trajet.getAvailableSeats()).isEqualTo(1);
            assertThat(trajet.getStatus()).isEqualTo(TripStatus.PLANNED);
        }

        @Test
        @DisplayName("bascule le trajet en COMPLET quand la dernière place part")
        void basculeEnComplet() {
            reservation(BookingStatus.PENDING, 3);

            service.accept(COURRIEL_CONDUCTEUR, "resa-uuid");

            assertThat(trajet.getAvailableSeats()).isZero();
            assertThat(trajet.getStatus()).isEqualTo(TripStatus.FULL);
        }

        @Test
        @DisplayName("refuse si les demandes en attente dépassent les places restantes")
        void refuseSiPlacesInsuffisantes() {
            // Deux demandes de 2 places sur un trajet qui n'en a plus qu'une :
            // la seconde acceptation doit échouer, sinon le trajet part en négatif.
            trajet.setAvailableSeats(1);
            reservation(BookingStatus.PENDING, 2);

            assertThatThrownBy(() -> service.accept(COURRIEL_CONDUCTEUR, "resa-uuid"))
                    .isInstanceOf(NoSeatsAvailableException.class);

            assertThat(trajet.getAvailableSeats()).isEqualTo(1);
        }

        @Test
        @DisplayName("refuse si l'appelant n'est pas le conducteur")
        void refuseUnTiers() {
            reservation(BookingStatus.PENDING, 1);

            assertThatThrownBy(() -> service.accept(COURRIEL_PASSAGER, "resa-uuid"))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("refuse une réservation déjà traitée")
        void refuseDoubleAcceptation() {
            reservation(BookingStatus.CONFIRMED, 1);

            assertThatThrownBy(() -> service.accept(COURRIEL_CONDUCTEUR, "resa-uuid"))
                    .isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    @DisplayName("Refuser une réservation")
    class Refuser {

        @Test
        @DisplayName("conserve le motif et ne touche pas aux places")
        void conserveLeMotif() {
            reservation(BookingStatus.PENDING, 2);

            var reponse = service.reject(COURRIEL_CONDUCTEUR, "resa-uuid", "  Voiture pleine  ");

            assertThat(reponse.getStatus()).isEqualTo(BookingStatus.REJECTED);
            assertThat(reponse.getStatusReason()).isEqualTo("Voiture pleine");
            assertThat(trajet.getAvailableSeats()).isEqualTo(3);
        }

        @Test
        @DisplayName("accepte un refus sans motif")
        void accepteUnRefusSansMotif() {
            reservation(BookingStatus.PENDING, 1);

            var reponse = service.reject(COURRIEL_CONDUCTEUR, "resa-uuid", "   ");

            assertThat(reponse.getStatusReason()).isNull();
        }
    }

    // ────────────────────────── F29 — Annulation passager ───────────────────────

    @Nested
    @DisplayName("Annuler une réservation")
    class Annuler {

        @Test
        @DisplayName("restitue les places d'une réservation confirmée")
        void restitueLesPlaces() {
            trajet.setAvailableSeats(1);
            reservation(BookingStatus.CONFIRMED, 2);

            service.cancel(COURRIEL_PASSAGER, "resa-uuid", null);

            assertThat(trajet.getAvailableSeats()).isEqualTo(3);
        }

        @Test
        @DisplayName("rouvre un trajet qui était complet")
        void rouvreUnTrajetComplet() {
            trajet.setAvailableSeats(0);
            trajet.setStatus(TripStatus.FULL);
            reservation(BookingStatus.CONFIRMED, 2);

            service.cancel(COURRIEL_PASSAGER, "resa-uuid", null);

            assertThat(trajet.getStatus()).isEqualTo(TripStatus.PLANNED);
            assertThat(trajet.getAvailableSeats()).isEqualTo(2);
        }

        @Test
        @DisplayName("ne restitue rien pour une demande restée en attente")
        void neRestituePasUneDemandeEnAttente() {
            // Une demande en attente n'avait rien débité : lui rendre des places
            // en créerait à partir de rien.
            reservation(BookingStatus.PENDING, 2);

            service.cancel(COURRIEL_PASSAGER, "resa-uuid", null);

            assertThat(trajet.getAvailableSeats()).isEqualTo(3);
        }

        @Test
        @DisplayName("refuse d'annuler la réservation d'un tiers")
        void refuseCelleDunTiers() {
            reservation(BookingStatus.CONFIRMED, 1);

            assertThatThrownBy(() -> service.cancel(COURRIEL_CONDUCTEUR, "resa-uuid", null))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("refuse d'annuler après le départ")
        void refuseApresLeDepart() {
            trajet.setDepartureTime(LocalDateTime.now().minusHours(1));
            reservation(BookingStatus.CONFIRMED, 1);

            assertThatThrownBy(() -> service.cancel(COURRIEL_PASSAGER, "resa-uuid", null))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("refuse d'annuler deux fois")
        void refuseDeuxAnnulations() {
            reservation(BookingStatus.CANCELLED, 1);

            assertThatThrownBy(() -> service.cancel(COURRIEL_PASSAGER, "resa-uuid", null))
                    .isInstanceOf(ConflictException.class);
        }
    }

    // ─────────────────── F21 — Confirmation de prestation ───────────────────────

    @Nested
    @DisplayName("Confirmer que le trajet a eu lieu")
    class Confirmer {

        @BeforeEach
        void trajetPasse() {
            trajet.setDepartureTime(LocalDateTime.now().minusHours(2));
        }

        @Test
        @DisplayName("fait passer la réservation en COMPLETED et horodate")
        void confirmeLaPrestation() {
            reservation(BookingStatus.CONFIRMED, 1);

            var reponse = service.complete(COURRIEL_PASSAGER, "resa-uuid");

            assertThat(reponse.getStatus()).isEqualTo(BookingStatus.COMPLETED);
            assertThat(reponse.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("compte le trajet pour les deux participants")
        void compteLesDeuxParticipants() {
            // Un covoiturage effectué est un trajet partagé : il compte pour
            // celui qui conduit comme pour celui qui monte. Ne créditer que le
            // conducteur ferait du passager un éternel débutant.
            reservation(BookingStatus.CONFIRMED, 1);
            int avantPassager = passager.getTripsCount();
            int avantConducteur = conducteur.getTripsCount();

            service.complete(COURRIEL_PASSAGER, "resa-uuid");

            assertThat(passager.getTripsCount()).isEqualTo(avantPassager + 1);
            assertThat(conducteur.getTripsCount()).isEqualTo(avantConducteur + 1);
            verify(userRepository).save(passager);
            verify(userRepository).save(conducteur);
        }

        @Test
        @DisplayName("refuse la confirmation par le conducteur")
        void refuseLeConducteur() {
            // La confirmation est confiée à la partie qui n'y gagne rien :
            // c'est ce qui en fait une information fiable.
            reservation(BookingStatus.CONFIRMED, 1);

            assertThatThrownBy(() -> service.complete(COURRIEL_CONDUCTEUR, "resa-uuid"))
                    .isInstanceOf(UnauthorizedException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("refuse une demande restée en attente")
        void refuseUneDemandeEnAttente() {
            reservation(BookingStatus.PENDING, 1);

            assertThatThrownBy(() -> service.complete(COURRIEL_PASSAGER, "resa-uuid"))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("refuse une réservation annulée")
        void refuseUneReservationAnnulee() {
            reservation(BookingStatus.CANCELLED, 1);

            assertThatThrownBy(() -> service.complete(COURRIEL_PASSAGER, "resa-uuid"))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("refuse une réservation refusée")
        void refuseUneReservationRefusee() {
            reservation(BookingStatus.REJECTED, 1);

            assertThatThrownBy(() -> service.complete(COURRIEL_PASSAGER, "resa-uuid"))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("refuse une seconde confirmation")
        void refuseUneSecondeConfirmation() {
            reservation(BookingStatus.COMPLETED, 1);

            assertThatThrownBy(() -> service.complete(COURRIEL_PASSAGER, "resa-uuid"))
                    .isInstanceOf(ConflictException.class);

            // Le compteur ne doit pas bouger : sinon confirmer en boucle
            // fabriquerait des trajets à volonté.
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("refuse de confirmer un trajet qui n'a pas encore eu lieu")
        void refuseUnTrajetAVenir() {
            trajet.setDepartureTime(LocalDateTime.now().plusDays(1));
            reservation(BookingStatus.CONFIRMED, 1);

            assertThatThrownBy(() -> service.complete(COURRIEL_PASSAGER, "resa-uuid"))
                    .isInstanceOf(ConflictException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("refuse une réservation inconnue")
        void refuseUneReservationInconnue() {
            when(bookingRepository.findByUuid("inexistante")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.complete(COURRIEL_PASSAGER, "inexistante"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("efface le motif hérité d'un changement de statut précédent")
        void effaceLeMotif() {
            reservation(BookingStatus.CONFIRMED, 1);

            var reponse = service.complete(COURRIEL_PASSAGER, "resa-uuid");

            assertThat(reponse.getStatusReason()).isNull();
        }
    }

    @Nested
    @DisplayName("Cercle ferme a la reservation")
    class Cercle {

        @Test
        @DisplayName("refuse de reserver un trajet d'une autre organisation")
        void refuseHorsDuCercle() {
            /* Dernier verrou, et le seul qui compte vraiment : la recherche et
               la fiche ne font que ne pas montrer. Sans ce refus, il suffirait
               de connaitre un identifiant de trajet pour monter dans la voiture
               d'une autre entreprise. La reponse est « introuvable » plutot
               qu'« interdit », pour ne pas confirmer l'existence du trajet. */
            when(tripRepository.findByUuid("trajet-uuid")).thenReturn(Optional.of(trajet));
            when(organizationService.partageLeCercle(any(), any())).thenReturn(false);

            assertThatThrownBy(() -> service.book(COURRIEL_PASSAGER, demande(1)))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(bookingRepository, never()).save(any());
        }
    }

    // ────────────────────────────────── Fabriques ───────────────────────────────

    private BookingRequest demande(int places) {
        BookingRequest r = new BookingRequest();
        r.setTripUuid("trajet-uuid");
        r.setSeatsBooked(places);
        return r;
    }

    private void reservation(BookingStatus statut, int places) {
        Booking b = Booking.builder()
                .id(100L)
                .uuid("resa-uuid")
                .trip(trajet)
                .passenger(passager)
                .seatsBooked(places)
                .totalPrice(new BigDecimal("5.00").multiply(BigDecimal.valueOf(places)))
                .status(statut)
                .build();
        when(bookingRepository.findByUuid("resa-uuid")).thenReturn(Optional.of(b));
    }

    private User utilisateur(Long id, String courriel) {
        return User.builder()
                .id(id)
                .uuid("uuid-" + id)
                .email(courriel)
                .firstname("Prenom" + id)
                .lastname("Nom" + id)
                .password("peu-importe")
                .emailVerified(true)
                .build();
    }

    /** Vérifie qu'aucune place n'a été créée ni perdue au fil des opérations. */
    @Test
    @DisplayName("le total des places reste constant après acceptation puis annulation")
    void lesPlacesSontConservees() {
        int avant = trajet.getAvailableSeats();
        reservation(BookingStatus.PENDING, 2);

        service.accept(COURRIEL_CONDUCTEUR, "resa-uuid");
        assertThat(trajet.getAvailableSeats()).isEqualTo(avant - 2);

        reservation(BookingStatus.CONFIRMED, 2);
        service.cancel(COURRIEL_PASSAGER, "resa-uuid", null);

        assertThat(trajet.getAvailableSeats()).isEqualTo(avant);
        assertThat(trajet.getStatus()).isEqualTo(TripStatus.PLANNED);
    }
}
