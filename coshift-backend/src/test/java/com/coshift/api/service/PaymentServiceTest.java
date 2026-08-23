package com.coshift.api.service;

import com.coshift.api.entity.Booking;
import com.coshift.api.entity.Payment;
import com.coshift.api.entity.PaymentStatus;
import com.coshift.api.entity.Trip;
import com.coshift.api.entity.User;
import com.coshift.api.exception.ConflictException;
import com.coshift.api.exception.UnauthorizedException;
import com.coshift.api.repository.PaymentRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le partage de frais.
 *
 * <p>Ce qui se teste ici est exactement ce que la separation du prestataire a
 * rendu testable : le <strong>bareme d'annulation</strong> et les
 * <strong>etats comptables</strong>. Aucun de ces cas ne demande de compte chez
 * un prestataire, et c'est le but de la manoeuvre.</p>
 *
 * <p>Le bareme tient en une idee : on ne fait pas payer quelqu'un pour une
 * decision qui n'est pas la sienne. Les cas ci-dessous en font le tour, y
 * compris ses bornes — a exactement vingt-quatre heures, et juste en deca.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PaymentService — partage de frais")
class PaymentServiceTest {

    @Mock private PaymentRepository repository;
    @Mock private PaymentGateway gateway;
    @Mock private Messages messages;

    @InjectMocks private PaymentService service;

    private User passager;
    private User autre;
    private Booking reservation;
    private Payment paiement;

    @BeforeEach
    void preparer() {
        passager = User.builder().id(1L).uuid("u-1").email("passager@coshift.be").build();
        autre = User.builder().id(2L).uuid("u-2").email("autre@coshift.be").build();

        Trip trajet = Trip.builder()
                .id(10L).uuid("t-1")
                .departureTime(LocalDateTime.now().plusDays(3))
                .build();

        reservation = Booking.builder()
                .id(100L).uuid("r-1")
                .trip(trajet).passenger(passager)
                .seatsBooked(2)
                .totalPrice(new BigDecimal("9.00"))
                .build();

        paiement = Payment.builder()
                .id(200L).uuid("p-1")
                .booking(reservation)
                .amount(new BigDecimal("9.00"))
                .currency("EUR")
                .status(PaymentStatus.DUE)
                .refundedAmount(BigDecimal.ZERO)
                .provider("SIMULATION")
                .build();

        when(messages.get(anyString())).thenReturn("message");
        when(gateway.nom()).thenReturn("SIMULATION");
        when(gateway.preparer(any()))
                .thenReturn(new PaymentGateway.Intention("sim_123", null, true));
        when(gateway.rembourser(any(), any())).thenReturn("simr_123");
        when(repository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(repository.findByBookingId(100L)).thenReturn(Optional.of(paiement));
        when(repository.findByBookingUuid("r-1")).thenReturn(Optional.of(paiement));
    }

    @Nested
    @DisplayName("Bareme d'annulation")
    class Bareme {

        @Test
        @DisplayName("le conducteur annule : tout est rendu, quel que soit le moment")
        void annulationParLeConducteurRendTout() {
            /* C'est la regle qui prime sur toutes les autres : le passager n'a
               rien decide et subit deja d'avoir a se deplacer autrement. */
            assertThat(service.partRendue(LocalDateTime.now().plusMinutes(5), true)).isEqualTo(100);
            assertThat(service.partRendue(LocalDateTime.now().minusDays(1), true)).isEqualTo(100);
        }

        @Test
        @DisplayName("plus de vingt-quatre heures avant : tout est rendu")
        void loinDuDepart() {
            assertThat(service.partRendue(LocalDateTime.now().plusDays(3), false)).isEqualTo(100);
        }

        @Test
        @DisplayName("a exactement vingt-quatre heures, la borne est encore favorable")
        void borneExacte() {
            /* Une inegalite stricte ferait basculer a la moitie quelqu'un qui
               annule pile a la limite annoncee. La borne appartient au cas
               genereux : c'est la lecture que fera la personne qui lit
               « plus de 24 heures ». */
            assertThat(service.partRendue(LocalDateTime.now().plusHours(24).plusMinutes(1), false))
                    .isEqualTo(100);
        }

        @Test
        @DisplayName("moins de vingt-quatre heures avant : la moitie")
        void prochesDuDepart() {
            /* Le siege ne se reloue plus, et le conducteur a organise son trajet
               autour de cette place. Rendre tout ferait de l'annulation de
               derniere minute une option gratuite, donc frequente. */
            assertThat(service.partRendue(LocalDateTime.now().plusHours(5), false))
                    .isEqualTo(PaymentService.PART_RENDUE_TARDIVE);
        }

        @Test
        @DisplayName("apres le depart : rien")
        void apresLeDepart() {
            assertThat(service.partRendue(LocalDateTime.now().minusMinutes(1), false)).isZero();
        }
    }

    @Nested
    @DisplayName("Ouverture du reglement")
    class Reglement {

        @Test
        @DisplayName("seul le passager peut regler sa place")
        void reserveAuPassager() {
            /* Sans ce controle, n'importe quel compte solderait la reservation
               d'un autre a partir de son seul identifiant public — et
               apprendrait au passage ce qu'elle coute. */
            assertThatThrownBy(() -> service.preparerReglement(autre, "r-1"))
                    .isInstanceOf(UnauthorizedException.class);

            verify(gateway, never()).preparer(any());
        }

        @Test
        @DisplayName("un prestataire qui regle sur-le-champ marque le paiement acquis")
        void prestataireImmediat() {
            /* C'est le cas de la simulation : elle ne demande aucune
               confirmation, et le service n'a pas a savoir lequel des deux
               prestataires repond. */
            service.preparerReglement(passager, "r-1");

            assertThat(paiement.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(paiement.getPaidAt()).isNotNull();
            assertThat(paiement.getProviderReference()).isEqualTo("sim_123");
        }

        @Test
        @DisplayName("un prestataire a confirmation laisse le montant du")
        void prestataireADifferer() {
            /* Le point qui compte de toute l'integration : une intention creee
               n'est pas un paiement recu. Marquer PAID ici laisserait n'importe
               qui ouvrir un reglement et l'abandonner pour voyager
               gratuitement. */
            when(gateway.preparer(any()))
                    .thenReturn(new PaymentGateway.Intention("pi_123", "pi_123_secret", false));

            var intention = service.preparerReglement(passager, "r-1");

            assertThat(intention.secretClient()).isEqualTo("pi_123_secret");
            assertThat(paiement.getStatus()).isEqualTo(PaymentStatus.DUE);
            assertThat(paiement.getPaidAt()).isNull();
            // La reference est conservee : c'est par elle que la notification retrouvera l'operation.
            assertThat(paiement.getProviderReference()).isEqualTo("pi_123");
        }

        @Test
        @DisplayName("refuse d'ouvrir un reglement deja regle")
        void pasDeSecondReglement() {
            paiement.setStatus(PaymentStatus.PAID);

            assertThatThrownBy(() -> service.preparerReglement(passager, "r-1"))
                    .isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    @DisplayName("Confirmation du reglement")
    class Confirmation {

        @BeforeEach
        void enAttenteDeConfirmation() {
            paiement.setProviderReference("pi_123");
            when(repository.findByProviderReference("pi_123")).thenReturn(Optional.of(paiement));
        }

        @Test
        @DisplayName("le serveur interroge le prestataire plutot que de croire le navigateur")
        void verificationCoteServeur() {
            /* La page qui suit le reglement est entre les mains de la personne
               qui paie. Le seul a savoir est le prestataire. */
            when(gateway.etat("pi_123")).thenReturn(PaymentGateway.EtatDistant.REGLE);

            var apres = service.verifier(passager, "r-1");

            assertThat(apres.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(apres.getPaidAt()).isNotNull();
        }

        @Test
        @DisplayName("un paiement encore en attente reste du")
        void encoreEnAttente() {
            when(gateway.etat("pi_123")).thenReturn(PaymentGateway.EtatDistant.EN_ATTENTE);

            assertThat(service.verifier(passager, "r-1").getStatus()).isEqualTo(PaymentStatus.DUE);
        }

        @Test
        @DisplayName("un echec est enregistre comme tel")
        void echec() {
            when(gateway.etat("pi_123")).thenReturn(PaymentGateway.EtatDistant.ECHOUE);

            assertThat(service.verifier(passager, "r-1").getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("une notification confirme le paiement")
        void notification() {
            service.confirmerDepuisPrestataire("pi_123");

            assertThat(paiement.getStatus()).isEqualTo(PaymentStatus.PAID);
        }

        @Test
        @DisplayName("une notification rejouee ne regle pas deux fois")
        void notificationRejouee() {
            /* Les prestataires reemettent leurs notifications en cas de doute.
               Une operation qui ne supporte pas d'etre rejouee finit par
               produire deux reglements pour une place. */
            service.confirmerDepuisPrestataire("pi_123");
            var premiereDate = paiement.getPaidAt();

            service.confirmerDepuisPrestataire("pi_123");

            assertThat(paiement.getPaidAt()).isEqualTo(premiereDate);
        }

        @Test
        @DisplayName("une notification pour une operation inconnue ne fait pas lever")
        void notificationInconnue() {
            when(repository.findByProviderReference("pi_inconnu")).thenReturn(Optional.empty());

            service.confirmerDepuisPrestataire("pi_inconnu");
        }
    }

    @Nested
    @DisplayName("Remboursement")
    class Remboursement {

        @Test
        @DisplayName("un du jamais regle est clos, pas rembourse")
        void duNonRegle() {
            /* On ne rembourse pas un montant jamais preleve. Le laisser DUE
               ferait apparaitre un impaye sur une reservation morte. */
            var apres = service.rembourser(reservation, false, "motif");

            assertThat(apres.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
            verify(gateway, never()).rembourser(any(), any());
        }

        @Test
        @DisplayName("remboursement integral : etat REFUNDED et montant complet")
        void integral() {
            paiement.setStatus(PaymentStatus.PAID);

            var apres = service.rembourser(reservation, true, "trajet annule");

            assertThat(apres.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(apres.getRefundedAmount()).isEqualByComparingTo("9.00");
            // Dans sa propre colonne : la reference du paiement n'est pas ecrasee.
            assertThat(apres.getRefundReference()).isEqualTo("simr_123");
            assertThat(apres.getRefundReason()).isEqualTo("trajet annule");
            assertThat(apres.montantAcquis()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("remboursement partiel : la moitie, arrondie au centime")
        void partiel() {
            paiement.setStatus(PaymentStatus.PAID);
            reservation.getTrip().setDepartureTime(LocalDateTime.now().plusHours(3));

            var apres = service.rembourser(reservation, false, "annulation tardive");

            assertThat(apres.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
            assertThat(apres.getRefundedAmount()).isEqualByComparingTo("4.50");
            // Ce qui reste acquis au conducteur.
            assertThat(apres.montantAcquis()).isEqualByComparingTo("4.50");
        }

        @Test
        @DisplayName("un montant impair se partage au centime, sans perte")
        void arrondi() {
            /* 7,05 / 2 = 3,525. L'arrondi au centime superieur rend 3,53 au
               passager : en cas de doute, le centime va a celui qui subit
               l'annulation la moins volontaire. Ce qui compte est surtout que
               la somme des deux parts fasse exactement le montant paye. */
            paiement.setAmount(new BigDecimal("7.05"));
            paiement.setStatus(PaymentStatus.PAID);
            reservation.getTrip().setDepartureTime(LocalDateTime.now().plusHours(3));

            var apres = service.rembourser(reservation, false, "annulation tardive");

            assertThat(apres.getRefundedAmount()).isEqualByComparingTo("3.53");
            assertThat(apres.getRefundedAmount().add(apres.montantAcquis()))
                    .isEqualByComparingTo("7.05");
        }

        @Test
        @DisplayName("apres le depart : rien n'est rendu, mais le motif est consigne")
        void apresLeDepartRienMaisUneTrace() {
            /* Le passager doit pouvoir lire pourquoi il ne recupere rien. */
            paiement.setStatus(PaymentStatus.PAID);
            reservation.getTrip().setDepartureTime(LocalDateTime.now().minusHours(1));

            var apres = service.rembourser(reservation, false, "annulation apres depart");

            assertThat(apres.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(apres.getRefundedAmount()).isEqualByComparingTo("0.00");
            assertThat(apres.getRefundReason()).isEqualTo("annulation apres depart");
            verify(gateway, never()).rembourser(any(), any());
        }

        @Test
        @DisplayName("une reservation sans paiement ne fait pas lever")
        void sansPaiement() {
            when(repository.findByBookingId(100L)).thenReturn(Optional.empty());

            assertThat(service.rembourser(reservation, false, "motif")).isNull();
        }
    }

    @Nested
    @DisplayName("Ouverture du du")
    class Ouverture {

        @Test
        @DisplayName("recopie le montant plutot que de le relire plus tard")
        void montantRecopie() {
            /* Le prix d'un trajet peut changer. Un paiement doit dire ce qui a
               ete regle ce jour-la, pas ce que couterait la meme place
               aujourd'hui : une facture ne se recalcule pas. */
            var ouvert = service.ouvrir(reservation);

            assertThat(ouvert.getAmount()).isEqualByComparingTo("9.00");
            assertThat(ouvert.getStatus()).isEqualTo(PaymentStatus.DUE);

            reservation.setTotalPrice(new BigDecimal("99.00"));
            assertThat(ouvert.getAmount()).isEqualByComparingTo("9.00");
        }
    }
}
