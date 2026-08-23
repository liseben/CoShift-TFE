package com.coshift.api.service;

import com.coshift.api.dto.ReviewRequest;
import com.coshift.api.entity.Booking;
import com.coshift.api.entity.BookingStatus;
import com.coshift.api.entity.Review;
import com.coshift.api.entity.Trip;
import com.coshift.api.entity.TripStatus;
import com.coshift.api.entity.User;
import com.coshift.api.exception.ConflictException;
import com.coshift.api.exception.ResourceNotFoundException;
import com.coshift.api.exception.UnauthorizedException;
import com.coshift.api.repository.BookingRepository;
import com.coshift.api.repository.ReviewRepository;
import com.coshift.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Notation réciproque.
 *
 * <p>Trois barrières protègent la note, et chacune répond à un abus précis :
 * sans la première, réserver puis annuler donnerait le droit de noter ; sans la
 * deuxième, un tiers jugerait un trajet auquel il n'a pas participé ; sans la
 * troisième, noter en boucle suffirait à couler quelqu'un. Elles sont testées
 * une par une, parce qu'elles tombent indépendamment.</p>
 *
 * <p>Le dernier groupe porte sur le recalcul de la moyenne. C'est le genre de
 * code qui échoue en silence : une moyenne fausse reste une moyenne
 * plausible.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReviewService — notation")
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private UserRepository userRepository;
    @Mock private Messages messages;

    @InjectMocks private ReviewService service;

    private User conducteur;
    private User passager;
    private User etranger;
    private Trip trajet;
    private Booking reservation;

    private static final String CONDUCTEUR = "conducteur@coshift.be";
    private static final String PASSAGER = "passager@coshift.be";
    private static final String ETRANGER = "etranger@coshift.be";

    @BeforeEach
    void preparer() {
        conducteur = utilisateur(1L, CONDUCTEUR);
        passager = utilisateur(2L, PASSAGER);
        etranger = utilisateur(3L, ETRANGER);

        trajet = Trip.builder()
                .id(10L).uuid("trajet-uuid")
                .departureCity("Namur").arrivalCity("Bruxelles")
                .departureTime(LocalDateTime.now().minusDays(1))
                .availableSeats(2)
                .pricePerSeat(new BigDecimal("5.00"))
                .status(TripStatus.COMPLETED)
                .driver(conducteur)
                .build();

        reservation = Booking.builder()
                .id(100L).uuid("resa-uuid")
                .trip(trajet).passenger(passager)
                .seatsBooked(1)
                .totalPrice(new BigDecimal("5.00"))
                .status(BookingStatus.COMPLETED)
                .completedAt(LocalDateTime.now().minusHours(2))
                .build();

        when(userRepository.findByEmail(CONDUCTEUR)).thenReturn(Optional.of(conducteur));
        when(userRepository.findByEmail(PASSAGER)).thenReturn(Optional.of(passager));
        when(userRepository.findByEmail(ETRANGER)).thenReturn(Optional.of(etranger));
        when(bookingRepository.findByUuid("resa-uuid")).thenReturn(Optional.of(reservation));
        when(reviewRepository.save(any(Review.class))).thenAnswer(i -> i.getArgument(0));
        when(reviewRepository.existsByBookingIdAndAuthorId(anyLong(), anyLong())).thenReturn(false);
        when(reviewRepository.moyenneDesNotes(anyLong())).thenReturn(null);
        when(messages.get(anyString())).thenReturn("message");
    }

    // ───────────────── Barrière 1 — il faut avoir voyagé ────────────────────────

    @Nested
    @DisplayName("Il faut avoir effectué le trajet")
    class TrajetEffectue {

        @ParameterizedTest(name = "refuse une réservation {0}")
        @EnumSource(value = BookingStatus.class,
                    names = {"PENDING", "CONFIRMED", "CANCELLED", "REJECTED"})
        @DisplayName("refuse tout statut autre que COMPLETED")
        void refuseTouteReservationNonTerminee(BookingStatus statut) {
            // CONFIRMED est le cas important : la place était réservée, mais rien
            // ne prouve que la course a eu lieu tant que le passager ne l'a pas
            // confirmée. Sans ce refus, réserver puis ne pas venir donnerait le
            // droit de noter.
            reservation.setStatus(statut);

            assertThatThrownBy(() -> service.deposer(PASSAGER, "resa-uuid", avis(5)))
                    .isInstanceOf(ConflictException.class);

            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("accepte une réservation confirmée puis reconnue")
        void accepteUneReservationTerminee() {
            var reponse = service.deposer(PASSAGER, "resa-uuid", avis(4));

            assertThat(reponse.getRating()).isEqualTo(4);
            verify(reviewRepository).save(any(Review.class));
        }
    }

    // ──────────── Barrière 2 — il faut avoir voyagé avec la personne ────────────

    @Nested
    @DisplayName("Il faut avoir partagé ce trajet")
    class Participants {

        @Test
        @DisplayName("le passager note le conducteur")
        void lePassagerNoteLeConducteur() {
            service.deposer(PASSAGER, "resa-uuid", avis(5));

            ArgumentCaptor<Review> capture = ArgumentCaptor.forClass(Review.class);
            verify(reviewRepository).save(capture.capture());
            assertThat(capture.getValue().getAuthor()).isEqualTo(passager);
            assertThat(capture.getValue().getTarget()).isEqualTo(conducteur);
        }

        @Test
        @DisplayName("le conducteur note le passager")
        void leConducteurNoteLePassager() {
            service.deposer(CONDUCTEUR, "resa-uuid", avis(3));

            ArgumentCaptor<Review> capture = ArgumentCaptor.forClass(Review.class);
            verify(reviewRepository).save(capture.capture());
            assertThat(capture.getValue().getAuthor()).isEqualTo(conducteur);
            assertThat(capture.getValue().getTarget()).isEqualTo(passager);
        }

        @Test
        @DisplayName("refuse un tiers qui n'a pas partagé le trajet")
        void refuseUnTiers() {
            assertThatThrownBy(() -> service.deposer(ETRANGER, "resa-uuid", avis(1)))
                    .isInstanceOf(UnauthorizedException.class);

            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("personne ne se note soi-même")
        void personneNeSeNotePasSoiMeme() {
            // La cible se déduit de la place de l'auteur : il est structurellement
            // impossible qu'elles coïncident. Ce test verrouille cette propriété.
            service.deposer(PASSAGER, "resa-uuid", avis(5));

            ArgumentCaptor<Review> capture = ArgumentCaptor.forClass(Review.class);
            verify(reviewRepository).save(capture.capture());
            assertThat(capture.getValue().getAuthor())
                    .isNotEqualTo(capture.getValue().getTarget());
        }
    }

    // ─────────────────── Barrière 3 — une seule fois ────────────────────────────

    @Nested
    @DisplayName("Un trajet, un avis par participant")
    class Unicite {

        @Test
        @DisplayName("refuse un second avis du même auteur")
        void refuseUnSecondAvis() {
            when(reviewRepository.existsByBookingIdAndAuthorId(100L, passager.getId()))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.deposer(PASSAGER, "resa-uuid", avis(1)))
                    .isInstanceOf(ConflictException.class);

            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("l'avis du conducteur n'empêche pas celui du passager")
        void lesDeuxParticipantsPeuventSExprimer() {
            // La notation est réciproque : l'unicité porte sur le couple
            // (réservation, auteur), pas sur la réservation seule.
            when(reviewRepository.existsByBookingIdAndAuthorId(100L, conducteur.getId()))
                    .thenReturn(true);
            when(reviewRepository.existsByBookingIdAndAuthorId(100L, passager.getId()))
                    .thenReturn(false);

            service.deposer(PASSAGER, "resa-uuid", avis(4));

            verify(reviewRepository).save(any(Review.class));
        }
    }

    // ───────────────────────── Recalcul de la moyenne ───────────────────────────

    @Nested
    @DisplayName("Moyenne du profil noté")
    class Moyenne {

        @Test
        @DisplayName("recopie la moyenne relue en base sur le profil")
        void recopieLaMoyenne() {
            when(reviewRepository.moyenneDesNotes(conducteur.getId())).thenReturn(4.5);

            service.deposer(PASSAGER, "resa-uuid", avis(5));

            assertThat(conducteur.getAverageRating()).isEqualTo(4.5);
            verify(userRepository).save(conducteur);
        }

        @Test
        @DisplayName("arrondit au dixième")
        void arronditAuDixieme() {
            // 4 et 5 donnent 4.333333... : une moyenne à quinze décimales
            // n'apprend rien et s'affiche mal.
            when(reviewRepository.moyenneDesNotes(conducteur.getId()))
                    .thenReturn(13.0 / 3.0);

            service.deposer(PASSAGER, "resa-uuid", avis(4));

            assertThat(conducteur.getAverageRating()).isEqualTo(4.3);
        }

        @Test
        @DisplayName("traduit l'absence d'avis en zéro, pas en erreur")
        void absenceDavisVautZero() {
            // AVG sur un ensemble vide ne vaut pas zéro, il ne vaut rien. Le
            // service doit retomber sur la convention de la colonne sans lever.
            when(reviewRepository.moyenneDesNotes(anyLong())).thenReturn(null);

            service.deposer(PASSAGER, "resa-uuid", avis(5));

            assertThat(conducteur.getAverageRating()).isZero();
        }

        @Test
        @DisplayName("ne touche pas au profil de l'auteur")
        void neTouchePasALauteur() {
            when(reviewRepository.moyenneDesNotes(conducteur.getId())).thenReturn(5.0);

            service.deposer(PASSAGER, "resa-uuid", avis(5));

            assertThat(passager.getAverageRating()).isZero();
            verify(userRepository, never()).save(passager);
        }
    }

    // ─────────────────────────── Contenu de l'avis ──────────────────────────────

    @Nested
    @DisplayName("Contenu")
    class Contenu {

        @Test
        @DisplayName("conserve le commentaire, débarrassé de ses espaces")
        void conserveLeCommentaire() {
            var reponse = service.deposer(PASSAGER, "resa-uuid",
                    avis(5, "  Ponctuel et agréable  "));

            assertThat(reponse.getComment()).isEqualTo("Ponctuel et agréable");
        }

        @Test
        @DisplayName("accepte un avis sans commentaire")
        void accepteUnAvisSansCommentaire() {
            var reponse = service.deposer(PASSAGER, "resa-uuid", avis(5, "   "));

            assertThat(reponse.getComment()).isNull();
        }

        @Test
        @DisplayName("expose le prénom de l'auteur, jamais son nom complet")
        void exposeLePrenomSeul() {
            var reponse = service.deposer(PASSAGER, "resa-uuid", avis(5));

            assertThat(reponse.getAuthorFirstname()).isEqualTo(passager.getFirstname());
            assertThat(reponse.toString()).doesNotContain(passager.getLastname());
            assertThat(reponse.toString()).doesNotContain(passager.getEmail());
        }

        @Test
        @DisplayName("situe l'avis sur son trajet")
        void situeLeTrajet() {
            var reponse = service.deposer(PASSAGER, "resa-uuid", avis(5));

            assertThat(reponse.getDepartureCity()).isEqualTo("Namur");
            assertThat(reponse.getArrivalCity()).isEqualTo("Bruxelles");
        }
    }

    // ─────────────────────────────── Cas d'erreur ───────────────────────────────

    @Test
    @DisplayName("refuse une réservation inconnue")
    void refuseUneReservationInconnue() {
        when(bookingRepository.findByUuid("inexistante")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deposer(PASSAGER, "inexistante", avis(5)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("refuse un auteur inconnu")
    void refuseUnAuteurInconnu() {
        when(userRepository.findByEmail("fantome@coshift.be")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deposer("fantome@coshift.be", "resa-uuid", avis(5)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("refuse une réservation dont le trajet a disparu")
    void refuseUnTrajetAbsent() {
        reservation.setTrip(null);

        assertThatThrownBy(() -> service.deposer(PASSAGER, "resa-uuid", avis(5)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("liste les avis reçus, du plus récent au plus ancien")
    void listeLesAvisRecus() {
        Review a = Review.builder().uuid("a").booking(reservation)
                .author(passager).target(conducteur).rating(5).build();
        Review b = Review.builder().uuid("b").booking(reservation)
                .author(passager).target(conducteur).rating(3).comment("Correct").build();
        when(reviewRepository.findByTargetIdOrderByCreatedAtDesc(conducteur.getId()))
                .thenReturn(java.util.List.of(a, b));

        var recus = service.avisRecus(CONDUCTEUR);

        assertThat(recus).hasSize(2);
        assertThat(recus.get(0).getRating()).isEqualTo(5);
        assertThat(recus.get(1).getComment()).isEqualTo("Correct");
    }

    // ────────────────────────────────── Fabriques ───────────────────────────────

    private ReviewRequest avis(int note) {
        return avis(note, null);
    }

    private ReviewRequest avis(int note, String commentaire) {
        ReviewRequest r = new ReviewRequest();
        r.setRating(note);
        r.setComment(commentaire);
        return r;
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
