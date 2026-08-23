package com.coshift.api.service;

import com.coshift.api.dto.TripRequest;
import com.coshift.api.entity.Booking;
import com.coshift.api.entity.BookingStatus;
import com.coshift.api.entity.EnergyType;
import com.coshift.api.entity.Trip;
import com.coshift.api.entity.TripStatus;
import com.coshift.api.entity.User;
import com.coshift.api.entity.Vehicule;
import com.coshift.api.exception.BadRequestException;
import com.coshift.api.exception.ConflictException;
import com.coshift.api.exception.ResourceNotFoundException;
import com.coshift.api.exception.UnauthorizedException;
import com.coshift.api.repository.BookingRepository;
import com.coshift.api.repository.TripRepository;
import com.coshift.api.repository.UserRepository;
import com.coshift.api.repository.VehiculeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Règles de publication et d'annulation des trajets.
 *
 * <p>Trois d'entre elles n'existaient auparavant que dans le navigateur — le
 * délai avant départ tenait à l'attribut {@code min} d'un champ de formulaire,
 * et le nombre de places à rien du tout. Elles étaient donc contournables par
 * un simple appel direct à l'API. Les tests ci-dessous vérifient qu'elles sont
 * bien appliquées côté serveur, là où elles ne se contournent pas.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TripService — trajets")
class TripServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private UserRepository userRepository;
    @Mock private VehiculeRepository vehiculeRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private Messages messages;

    @InjectMocks private TripService service;

    private User conducteur;
    private User autre;
    private Vehicule vehicule;

    private static final String CONDUCTEUR = "conducteur@coshift.be";
    private static final String AUTRE = "autre@coshift.be";

    @BeforeEach
    void preparer() {
        conducteur = utilisateur(1L, CONDUCTEUR);
        autre = utilisateur(2L, AUTRE);

        vehicule = Vehicule.builder()
                .id(20L).uuid("vehicule-uuid")
                .brand("Renault").model("Clio")
                .seats(4)                       // 4 places au total, conducteur compris
                .energy(EnergyType.GASOLINE)
                .owner(conducteur)
                .build();

        when(userRepository.findByEmail(CONDUCTEUR)).thenReturn(Optional.of(conducteur));
        when(userRepository.findByEmail(AUTRE)).thenReturn(Optional.of(autre));
        when(vehiculeRepository.findByUuid("vehicule-uuid")).thenReturn(Optional.of(vehicule));
        when(tripRepository.findByDriverIdOrderByDepartureTimeDesc(anyLong())).thenReturn(List.of());
        when(tripRepository.save(any(Trip.class))).thenAnswer(i -> i.getArgument(0));
        when(messages.get(anyString())).thenReturn("message");
    }

    // ───────────────────────────── F16 — Publier ────────────────────────────────

    @Nested
    @DisplayName("Publier un trajet")
    class Publier {

        @Test
        @DisplayName("refuse un départ dans moins de deux heures")
        void refuseDelaiTropCourt() {
            TripRequest r = demande(LocalDateTime.now().plusHours(1), 2);

            assertThatThrownBy(() -> service.publishTrip(CONDUCTEUR, r))
                    .isInstanceOf(BadRequestException.class);

            verify(tripRepository, never()).save(any());
        }

        @Test
        @DisplayName("accepte un départ au-delà de deux heures")
        void accepteDelaiSuffisant() {
            TripRequest r = demande(LocalDateTime.now().plusHours(3), 2);

            assertThatCode(() -> service.publishTrip(CONDUCTEUR, r)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("refuse au-delà de cinq trajets actifs")
        void refuseAuDelaDuQuota() {
            when(tripRepository.findByDriverIdOrderByDepartureTimeDesc(1L))
                    .thenReturn(trajetsActifs(5));

            assertThatThrownBy(() -> service.publishTrip(CONDUCTEUR, demande(2)))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("ne compte pas les trajets clôturés dans le quota")
        void neCompteQueLesTrajetsActifs() {
            // Cinq trajets, mais tous terminés : le quota ne doit pas s'appliquer.
            List<Trip> termines = trajetsActifs(5);
            termines.forEach(t -> t.setStatus(TripStatus.COMPLETED));
            when(tripRepository.findByDriverIdOrderByDepartureTimeDesc(1L)).thenReturn(termines);

            assertThatCode(() -> service.publishTrip(CONDUCTEUR, demande(2)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("refuse le véhicule de quelqu'un d'autre")
        void refuseVehiculeDautrui() {
            assertThatThrownBy(() -> service.publishTrip(AUTRE, demande(2)))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("refuse plus de places que le véhicule n'en a, conducteur déduit")
        void refusePlacesSuperieuresAuVehicule() {
            // Voiture de 4 places, conducteur compris : 3 passagers au maximum.
            assertThatThrownBy(() -> service.publishTrip(CONDUCTEUR, demande(4)))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("accepte exactement le nombre de places restantes")
        void accepteLaBornePrecise() {
            assertThatCode(() -> service.publishTrip(CONDUCTEUR, demande(3)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("publie en PLANNED avec les valeurs demandées")
        void publieEnPlanned() {
            var reponse = service.publishTrip(CONDUCTEUR, demande(2));

            assertThat(reponse.getStatus()).isEqualTo(TripStatus.PLANNED);
            assertThat(reponse.getAvailableSeats()).isEqualTo(2);
            assertThat(reponse.getDepartureCity()).isEqualTo("Namur");
            assertThat(reponse.getArrivalCity()).isEqualTo("Bruxelles");
        }

        @Test
        @DisplayName("refuse un véhicule inconnu")
        void refuseVehiculeInconnu() {
            when(vehiculeRepository.findByUuid("inexistant")).thenReturn(Optional.empty());
            TripRequest r = demande(2);
            r.setVehiculeUuid("inexistant");

            assertThatThrownBy(() -> service.publishTrip(CONDUCTEUR, r))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ──────────────────────────── F18 — Annuler ─────────────────────────────────

    @Nested
    @DisplayName("Annuler un trajet")
    class Annuler {

        @Test
        @DisplayName("annule les réservations en cours en cascade")
        void annuleEnCascade() {
            Trip t = trajet(TripStatus.PLANNED, LocalDateTime.now().plusDays(1));
            when(tripRepository.findByUuid("t-uuid")).thenReturn(Optional.of(t));

            List<Booking> touchees = List.of(
                    reservation(t, BookingStatus.PENDING),
                    reservation(t, BookingStatus.CONFIRMED));
            when(bookingRepository.findByTripIdAndStatusIn(anyLong(), anyList())).thenReturn(touchees);

            service.cancelTrip(CONDUCTEUR, "t-uuid");

            assertThat(t.getStatus()).isEqualTo(TripStatus.CANCELLED);
            assertThat(touchees).allSatisfy(b ->
                    assertThat(b.getStatus()).isEqualTo(BookingStatus.CANCELLED));
            // Chaque passager doit savoir pourquoi sa réservation est tombée.
            assertThat(touchees).allSatisfy(b ->
                    assertThat(b.getStatusReason()).isNotNull());
            verify(bookingRepository).saveAll(touchees);
        }

        @Test
        @DisplayName("refuse si l'appelant n'est pas le conducteur")
        void refuseUnTiers() {
            Trip t = trajet(TripStatus.PLANNED, LocalDateTime.now().plusDays(1));
            when(tripRepository.findByUuid("t-uuid")).thenReturn(Optional.of(t));

            assertThatThrownBy(() -> service.cancelTrip(AUTRE, "t-uuid"))
                    .isInstanceOf(UnauthorizedException.class);

            assertThat(t.getStatus()).isEqualTo(TripStatus.PLANNED);
        }

        @Test
        @DisplayName("refuse d'annuler un trajet déjà parti")
        void refuseTrajetPasse() {
            Trip t = trajet(TripStatus.PLANNED, LocalDateTime.now().minusHours(2));
            when(tripRepository.findByUuid("t-uuid")).thenReturn(Optional.of(t));

            assertThatThrownBy(() -> service.cancelTrip(CONDUCTEUR, "t-uuid"))
                    .isInstanceOf(ConflictException.class);
        }
    }

    // ───────────────────── Clôture automatique des trajets échus ────────────────

    @Nested
    @DisplayName("Clôture automatique")
    class Cloture {

        @Test
        @DisplayName("bascule en COMPLETED les trajets dont l'heure est passée")
        void clotureLesTrajetsEchus() {
            List<Trip> echus = List.of(
                    trajet(TripStatus.PLANNED, LocalDateTime.now().minusHours(3)),
                    trajet(TripStatus.FULL, LocalDateTime.now().minusHours(1)));
            when(tripRepository.findByStatusInAndDepartureTimeBefore(anyList(), any()))
                    .thenReturn(echus);

            service.closePastTrips();

            assertThat(echus).allSatisfy(t ->
                    assertThat(t.getStatus()).isEqualTo(TripStatus.COMPLETED));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Trip>> capture = ArgumentCaptor.forClass(List.class);
            verify(tripRepository).saveAll(capture.capture());
            assertThat(capture.getValue()).hasSize(2);
        }

        @Test
        @DisplayName("n'écrit rien quand aucun trajet n'est échu")
        void neFaitRienSansTrajetEchu() {
            when(tripRepository.findByStatusInAndDepartureTimeBefore(anyList(), any()))
                    .thenReturn(List.of());

            service.closePastTrips();

            verify(tripRepository, never()).saveAll(any());
        }
    }

    // ────────────────────────────────── Fabriques ───────────────────────────────

    private TripRequest demande(int places) {
        return demande(LocalDateTime.now().plusDays(1), places);
    }

    private TripRequest demande(LocalDateTime depart, int places) {
        TripRequest r = new TripRequest();
        r.setDepartureCity("Namur");
        r.setDepartureAddress("Place d'Armes 1");
        r.setArrivalCity("Bruxelles");
        r.setArrivalAddress("Rue de la Loi 16");
        r.setDepartureTime(depart);
        r.setAvailableSeats(places);
        r.setPricePerSeat(new BigDecimal("5.00"));
        r.setVehiculeUuid("vehicule-uuid");
        return r;
    }

    private Trip trajet(TripStatus statut, LocalDateTime depart) {
        return Trip.builder()
                .id(10L).uuid("t-uuid")
                .departureCity("Namur").arrivalCity("Bruxelles")
                .departureTime(depart)
                .availableSeats(3)
                .pricePerSeat(new BigDecimal("5.00"))
                .status(statut)
                .driver(conducteur)
                .vehicule(vehicule)
                .build();
    }

    private List<Trip> trajetsActifs(int combien) {
        return IntStream.range(0, combien)
                .mapToObj(i -> trajet(TripStatus.PLANNED, LocalDateTime.now().plusDays(i + 1L)))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
    }

    private Booking reservation(Trip t, BookingStatus statut) {
        return Booking.builder()
                .id(100L).uuid("r-uuid")
                .trip(t).passenger(autre)
                .seatsBooked(1)
                .totalPrice(new BigDecimal("5.00"))
                .status(statut)
                .build();
    }

    private User utilisateur(Long id, String courriel) {
        return User.builder()
                .id(id).uuid("uuid-" + id)
                .email(courriel)
                .firstname("Prenom" + id).lastname("Nom" + id)
                .password("peu-importe")
                .emailVerified(true)
                .build();
    }
}
