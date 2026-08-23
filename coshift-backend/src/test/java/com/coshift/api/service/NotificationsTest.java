package com.coshift.api.service;

import com.coshift.api.dto.BookingRequest;
import com.coshift.api.entity.Booking;
import com.coshift.api.entity.BookingStatus;
import com.coshift.api.entity.Trip;
import com.coshift.api.entity.TripStatus;
import com.coshift.api.entity.User;
import com.coshift.api.entity.Vehicule;
import com.coshift.api.repository.BookingRepository;
import com.coshift.api.repository.OrganizationRepository;
import com.coshift.api.repository.ReviewRepository;
import com.coshift.api.repository.TripRepository;
import com.coshift.api.repository.UserRepository;
import com.coshift.api.repository.VehiculeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Notifications de réservation (F18, F19, F20, F29).
 *
 * <p>Ces envois ont une propriété que les deux courriels préexistants n'avaient
 * pas : <strong>le destinataire n'est pas celui qui agit</strong>. Quand un
 * passager réserve, le courriel part vers le conducteur. Quand un conducteur
 * accepte, il part vers le passager.</p>
 *
 * <p>Deux conséquences se testent, et elles se cassent toutes deux en silence :
 * un courriel envoyé à la mauvaise personne, et un courriel envoyé dans la
 * langue de l'expéditeur plutôt que dans celle du destinataire. Ni l'un ni
 * l'autre ne provoque d'erreur — l'application continue, quelqu'un reçoit
 * simplement quelque chose qu'il ne comprend pas, ou ne reçoit rien.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Notifications — qui reçoit quoi, et dans quelle langue")
class NotificationsTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private TripRepository tripRepository;
    @Mock private UserRepository userRepository;
    @Mock private VehiculeRepository vehiculeRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private EmailService emailService;
    @Mock private Messages messages;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private OrganizationService organizationService;
    @Mock private PaymentService paymentService;

    private BookingService bookingService;
    private TripService tripService;

    private User conducteur;
    private User passager;
    private Trip trajet;

    private static final String CONDUCTEUR = "conducteur@coshift.be";
    private static final String PASSAGER = "passager@coshift.be";

    @BeforeEach
    void preparer() {
        bookingService = new BookingService(bookingRepository, messages, tripRepository,
                userRepository, reviewRepository, emailService, organizationService, paymentService);
        tripService = new TripService(tripRepository, messages, userRepository,
                vehiculeRepository, bookingRepository, emailService,
                organizationRepository, organizationService, paymentService);

        /* Ces cas portent sur le destinataire et la langue des courriels, pas
           sur la visibilite : tout le monde partage le cercle. */
        when(organizationService.partageLeCercle(any(), any())).thenReturn(true);

        conducteur = utilisateur(1L, CONDUCTEUR, "Camille", "fr");
        // Anglophone : c'est tout l'enjeu des tests de langue ci-dessous.
        passager = utilisateur(2L, PASSAGER, "Sam", "en");

        Vehicule vehicule = Vehicule.builder()
                .id(20L).uuid("vehicule-uuid").brand("Renault").model("Clio")
                .seats(4).owner(conducteur).build();

        trajet = Trip.builder()
                .id(10L).uuid("trajet-uuid")
                .departureCity("Namur").arrivalCity("Bruxelles")
                .departureTime(LocalDateTime.now().plusDays(1))
                .availableSeats(3).pricePerSeat(new BigDecimal("5.00"))
                .status(TripStatus.PLANNED)
                .driver(conducteur).vehicule(vehicule)
                .build();

        when(userRepository.findByEmail(CONDUCTEUR)).thenReturn(Optional.of(conducteur));
        when(userRepository.findByEmail(PASSAGER)).thenReturn(Optional.of(passager));
        when(tripRepository.findByUuid("trajet-uuid")).thenReturn(Optional.of(trajet));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
        when(tripRepository.save(any(Trip.class))).thenAnswer(i -> i.getArgument(0));
        when(reviewRepository.reservationsDejaNoteesPar(anyLong())).thenReturn(List.of());
        when(messages.get(anyString())).thenReturn("message");
        when(messages.get(anyString(), any())).thenReturn("message");
    }

    // ═══════════════════════ Le bon destinataire ════════════════════════════════

    @Nested
    @DisplayName("Le courriel part vers l'autre partie")
    class BonDestinataire {

        @Test
        @DisplayName("une demande prévient le conducteur, pas le passager")
        void laDemandePrevientLeConducteur() {
            bookingService.book(PASSAGER, demande(2));

            verify(emailService).notifierDemandeRecue(eq(conducteur), eq("Sam"), anyString(), eq(2));
            verify(emailService, never()).notifierDemandeRecue(eq(passager), anyString(), anyString(), anyInt());
        }

        @Test
        @DisplayName("une acceptation prévient le passager")
        void lacceptationPrevientLePassager() {
            reservation(BookingStatus.PENDING, 1);

            bookingService.accept(CONDUCTEUR, "resa-uuid");

            verify(emailService).notifierReservationAcceptee(eq(passager), eq("Camille"), anyString());
        }

        @Test
        @DisplayName("un refus prévient le passager et transmet le motif")
        void leRefusPrevientLePassager() {
            // Refuser sans un mot laisse le passager sans explication ; taire le
            // motif dans le courriel l'obligerait à revenir le chercher.
            reservation(BookingStatus.PENDING, 1);

            bookingService.reject(CONDUCTEUR, "resa-uuid", "Voiture pleine");

            verify(emailService).notifierReservationRefusee(
                    eq(passager), anyString(), eq("Voiture pleine"));
        }

        @Test
        @DisplayName("un désistement prévient le conducteur")
        void leDesistementPrevientLeConducteur() {
            reservation(BookingStatus.CONFIRMED, 1);

            bookingService.cancel(PASSAGER, "resa-uuid", null);

            verify(emailService).notifierAnnulationParPassager(
                    eq(conducteur), eq("Sam"), anyString());
        }

        @Test
        @DisplayName("l'annulation d'un trajet prévient chaque passager concerné")
        void lannulationPrevientChaquePassager() {
            // Sans cet envoi, quelqu'un attend à un point de rendez-vous où
            // personne ne viendra. C'est la notification la plus importante.
            User autrePassager = utilisateur(3L, "autre@coshift.be", "Alex", "fr");
            when(bookingRepository.findByTripIdAndStatusIn(anyLong(), any()))
                    .thenReturn(new ArrayList<>(List.of(
                            reservationDe(passager, BookingStatus.CONFIRMED),
                            reservationDe(autrePassager, BookingStatus.PENDING))));

            tripService.cancelTrip(CONDUCTEUR, "trajet-uuid");

            verify(emailService).notifierTrajetAnnule(eq(passager), anyString());
            verify(emailService).notifierTrajetAnnule(eq(autrePassager), anyString());
        }
    }

    // ═══════════════════════ La bonne langue ════════════════════════════════════

    @Nested
    @DisplayName("La langue est celle du destinataire")
    class BonneLangue {

        @Test
        @DisplayName("le conducteur francophone reçoit du français, quel que soit le passager")
        void leConducteurRecoitSaLangue() {
            // Le passager est anglophone et c'est lui qui déclenche l'envoi.
            // Sans la colonne preferred_language, la langue de la requête aurait
            // fait foi et le conducteur aurait reçu de l'anglais.
            bookingService.book(PASSAGER, demande(1));

            ArgumentCaptor<User> destinataire = ArgumentCaptor.forClass(User.class);
            verify(emailService).notifierDemandeRecue(
                    destinataire.capture(), anyString(), anyString(), anyInt());
            assertThat(destinataire.getValue().langue()).isEqualTo(Locale.FRENCH);
        }

        @Test
        @DisplayName("le passager anglophone reçoit de l'anglais")
        void lePassagerRecoitSaLangue() {
            reservation(BookingStatus.PENDING, 1);

            bookingService.accept(CONDUCTEUR, "resa-uuid");

            ArgumentCaptor<User> destinataire = ArgumentCaptor.forClass(User.class);
            verify(emailService).notifierReservationAcceptee(
                    destinataire.capture(), anyString(), anyString());
            assertThat(destinataire.getValue().langue().getLanguage()).isEqualTo("en");
        }

        @Test
        @DisplayName("chaque passager d'un trajet annulé reçoit la sienne")
        void chacunDansSaLangue() {
            User francophone = utilisateur(3L, "fr@coshift.be", "Alex", "fr");
            when(bookingRepository.findByTripIdAndStatusIn(anyLong(), any()))
                    .thenReturn(new ArrayList<>(List.of(
                            reservationDe(passager, BookingStatus.CONFIRMED),
                            reservationDe(francophone, BookingStatus.CONFIRMED))));

            tripService.cancelTrip(CONDUCTEUR, "trajet-uuid");

            assertThat(passager.langue().getLanguage()).isEqualTo("en");
            assertThat(francophone.langue()).isEqualTo(Locale.FRENCH);
        }

        @Test
        @DisplayName("retombe sur le français quand la langue n'a jamais été relevée")
        void repliSurLeFrancais() {
            // NULL désigne les comptes créés avant la migration V9 : personne
            // n'a jamais relevé leur langue. Le repli est explicite ici, pas
            // écrit en base — la distinction se perdrait.
            User ancien = utilisateur(4L, "ancien@coshift.be", "Dominique", null);

            assertThat(ancien.getPreferredLanguage()).isNull();
            assertThat(ancien.langue()).isEqualTo(Locale.FRENCH);
        }
    }

    // ═══════════════════ Aucun envoi quand l'action échoue ══════════════════════

    @Nested
    @DisplayName("Rien n'est envoyé quand l'action est refusée")
    class AucunEnvoiSiRefus {

        @Test
        @DisplayName("une demande refusée par les règles n'écrit à personne")
        void demandeInvalideNecritPersonne() {
            // Réserver son propre trajet est refusé : prévenir le conducteur
            // qu'il s'est sollicité lui-même n'aurait aucun sens.
            assertThatThrownBy(() -> bookingService.book(CONDUCTEUR, demande(1)))
                    .isInstanceOf(RuntimeException.class);

            verify(emailService, never()).notifierDemandeRecue(any(), anyString(), anyString(), anyInt());
        }

        @Test
        @DisplayName("une acceptation impossible n'écrit à personne")
        void acceptationImpossibleNecritPersonne() {
            trajet.setAvailableSeats(1);
            reservation(BookingStatus.PENDING, 3);

            assertThatThrownBy(() -> bookingService.accept(CONDUCTEUR, "resa-uuid"))
                    .isInstanceOf(RuntimeException.class);

            verify(emailService, never()).notifierReservationAcceptee(any(), anyString(), anyString());
        }

        @Test
        @DisplayName("une annulation de trajet sans réservation n'écrit à personne")
        void annulationSansReservationNecritPersonne() {
            when(bookingRepository.findByTripIdAndStatusIn(anyLong(), any()))
                    .thenReturn(new ArrayList<>());

            tripService.cancelTrip(CONDUCTEUR, "trajet-uuid");

            verify(emailService, never()).notifierTrajetAnnule(any(), anyString());
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
                .id(100L).uuid("resa-uuid")
                .trip(trajet).passenger(passager)
                .seatsBooked(places).totalPrice(new BigDecimal("5.00"))
                .status(statut)
                .build();
        when(bookingRepository.findByUuid("resa-uuid")).thenReturn(Optional.of(b));
    }

    private Booking reservationDe(User qui, BookingStatus statut) {
        return Booking.builder()
                .id(200L).uuid("resa-" + qui.getId())
                .trip(trajet).passenger(qui)
                .seatsBooked(1).totalPrice(new BigDecimal("5.00"))
                .status(statut)
                .build();
    }

    private User utilisateur(Long id, String courriel, String prenom, String langue) {
        return User.builder()
                .id(id).uuid("uuid-" + id)
                .email(courriel)
                .firstname(prenom).lastname("Nom" + id)
                .password("peu-importe")
                .emailVerified(true)
                .preferredLanguage(langue)
                .build();
    }
}
