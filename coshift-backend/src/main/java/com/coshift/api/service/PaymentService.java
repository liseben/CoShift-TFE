package com.coshift.api.service;

import com.coshift.api.entity.Booking;
import com.coshift.api.entity.Payment;
import com.coshift.api.entity.PaymentStatus;
import com.coshift.api.entity.User;
import com.coshift.api.exception.ConflictException;
import com.coshift.api.exception.ResourceNotFoundException;
import com.coshift.api.exception.UnauthorizedException;
import com.coshift.api.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Le partage de frais : ce qui est dû, ce qui est réglé, ce qui est rendu.
 *
 * <h2>Ce que ce service décide, et ce qu'il délègue</h2>
 *
 * <p>Il décide les règles : quand un montant devient dû, ce qu'un
 * remboursement doit rendre selon le moment de l'annulation et selon qui
 * annule. Il délègue le mouvement de fonds à {@link PaymentGateway}, parce que
 * faire circuler l'argent d'un tiers relève d'un agrément que CoShift n'a pas.
 * Tout ce qui est ci-dessous se teste sans compte chez un prestataire ; c'est
 * précisément ce qui a motivé la séparation.</p>
 *
 * <h2>Le barème d'annulation</h2>
 *
 * <p>Il tient en une idée : <strong>on ne fait pas payer quelqu'un pour une
 * décision qui n'est pas la sienne</strong>.</p>
 *
 * <ul>
 *   <li>Le conducteur annule son trajet, ou refuse la demande → <strong>100 %
 *       rendus</strong>. Le passager n'a rien décidé et subit déjà d'avoir à se
 *       déplacer autrement.</li>
 *   <li>Le passager annule <strong>plus de {@value #HEURES_REMBOURSEMENT_INTEGRAL}
 *       heures</strong> avant le départ → 100 % rendus. Le conducteur a le temps
 *       de retrouver quelqu'un.</li>
 *   <li>Le passager annule <strong>moins de {@value #HEURES_REMBOURSEMENT_INTEGRAL}
 *       heures</strong> avant → {@value #PART_RENDUE_TARDIVE} % rendus. Le siège
 *       ne se reloue plus, et le conducteur a organisé son trajet autour de
 *       cette place ; rendre tout ferait de l'annulation de dernière minute une
 *       option gratuite, ce qui la rendrait fréquente.</li>
 *   <li>Le passager annule <strong>après le départ</strong> → rien. Le trajet a
 *       eu lieu, la place a été immobilisée.</li>
 * </ul>
 *
 * <p>Le seuil est <em>une</em> décision défendable, pas la seule. Ce qui compte
 * est qu'il soit écrit, appliqué au même endroit pour tout le monde, et que le
 * motif retenu accompagne chaque remboursement — un passager qui reçoit la
 * moitié doit pouvoir lire pourquoi.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    /** Au-delà, l'annulation est sans frais : le conducteur a le temps de reloger la place. */
    public static final int HEURES_REMBOURSEMENT_INTEGRAL = 24;

    /** Part rendue au passager qui annule tardivement, en pourcentage. */
    public static final int PART_RENDUE_TARDIVE = 50;

    private final PaymentRepository repository;
    private final PaymentGateway gateway;
    private final Messages messages;

    // ─────────────────────────────── Cycle de vie ────────────────────────────

    /**
     * Ouvre le dû d'une réservation.
     *
     * <p>Rien n'est prélevé : la demande peut encore être refusée, et faire
     * payer une place qu'on n'aura peut-être pas obligerait à rembourser des
     * gens qui n'ont jamais voyagé. Le montant est recopié ici, et non lu plus
     * tard sur la réservation : le prix d'un trajet peut changer, une facture
     * ne se recalcule pas.</p>
     */
    @Transactional
    public Payment ouvrir(Booking reservation) {
        return repository.save(Payment.builder()
                .booking(reservation)
                .amount(reservation.getTotalPrice())
                .status(PaymentStatus.DUE)
                .provider(gateway.nom())
                .build());
    }

    /**
     * Ouvre le règlement auprès du prestataire.
     *
     * <h2>Deux issues, selon le prestataire</h2>
     *
     * <p>La simulation déclare le paiement acquis sur-le-champ : l'état passe à
     * {@code PAID} et il n'y a rien à confirmer. Stripe rend un secret à usage
     * unique que le navigateur présentera avec les coordonnées bancaires — qui
     * ne transitent jamais par CoShift. Le paiement reste alors {@code DUE}
     * jusqu'à confirmation ; il n'est pas déclaré acquis parce qu'une intention
     * a été créée.</p>
     *
     * <p>Le passager seul peut ouvrir le règlement de sa place. Cela paraît
     * évident et ne l'est pas : sans ce contrôle, n'importe quel compte
     * pourrait solder la réservation d'un autre à partir de son seul
     * identifiant public — et, accessoirement, apprendre ce qu'elle coûte.</p>
     *
     * <p>Appelé deux fois, il rend une nouvelle intention. C'est voulu : un
     * secret client est à usage unique, et quelqu'un dont la carte a été
     * refusée doit pouvoir réessayer.</p>
     */
    @Transactional
    public PaymentGateway.Intention preparerReglement(User appelant, String bookingUuid) {
        Payment paiement = trouver(bookingUuid);

        if (!paiement.getBooking().getPassenger().getId().equals(appelant.getId())) {
            throw new UnauthorizedException(messages.get("paiement.pasLaVotre"));
        }
        if (paiement.getStatus() != PaymentStatus.DUE) {
            throw new ConflictException(messages.get("paiement.plusDu"));
        }

        PaymentGateway.Intention intention = gateway.preparer(paiement);

        paiement.setProvider(gateway.nom());
        paiement.setProviderReference(intention.reference());
        if (intention.regleImmediatement()) {
            paiement.setStatus(PaymentStatus.PAID);
            paiement.setPaidAt(LocalDateTime.now());
        }
        repository.save(paiement);

        return intention;
    }

    /**
     * Vérifie auprès du prestataire où en est le règlement, et met l'état à jour.
     *
     * <h2>Pourquoi ne pas croire le navigateur</h2>
     *
     * <p>Après avoir confirmé, la page annonce « c'est payé ». Cette page est
     * entre les mains de la personne qui paie : la croire reviendrait à laisser
     * qui le souhaite marquer sa réservation réglée avec l'outil de
     * développement de son navigateur. Le serveur interroge donc le
     * prestataire, seul à savoir.</p>
     *
     * <p>Ce chemin double celui des notifications, qui reste l'autorité en
     * production. Il existe parce qu'un poste de développement n'a pas
     * d'adresse publique où les recevoir, et parce qu'une notification peut se
     * perdre.</p>
     */
    @Transactional
    public Payment verifier(User appelant, String bookingUuid) {
        Payment paiement = trouver(bookingUuid);

        if (!paiement.getBooking().getPassenger().getId().equals(appelant.getId())) {
            throw new UnauthorizedException(messages.get("paiement.pasLaVotre"));
        }
        if (paiement.getStatus() != PaymentStatus.DUE || paiement.getProviderReference() == null) {
            return paiement;
        }

        return switch (gateway.etat(paiement.getProviderReference())) {
            case REGLE -> marquerRegle(paiement);
            case ECHOUE -> {
                paiement.setStatus(PaymentStatus.FAILED);
                yield repository.save(paiement);
            }
            case EN_ATTENTE -> paiement;
        };
    }

    /**
     * Confirme un règlement depuis une notification du prestataire.
     *
     * <p>Trouve le paiement par la référence de l'opération, et non par un
     * identifiant fourni dans le corps de la notification : c'est la référence
     * qui a été émise par le prestataire lui-même, donc la seule que
     * l'application puisse rapprocher de ce qu'elle a enregistré.</p>
     *
     * <p>Sans effet si le paiement est déjà réglé. Les prestataires réémettent
     * leurs notifications en cas de doute, et une opération qui ne supporte pas
     * d'être rejouée finit par produire deux règlements pour une place.</p>
     */
    @Transactional
    public void confirmerDepuisPrestataire(String reference) {
        repository.findByProviderReference(reference).ifPresentOrElse(paiement -> {
            if (paiement.getStatus() == PaymentStatus.DUE) {
                marquerRegle(paiement);
                log.info("Paiement {} confirmé par une notification du prestataire", reference);
            }
        }, () -> log.warn("Notification reçue pour une opération inconnue : {}", reference));
    }

    private Payment marquerRegle(Payment paiement) {
        paiement.setStatus(PaymentStatus.PAID);
        paiement.setPaidAt(LocalDateTime.now());
        return repository.save(paiement);
    }

    /**
     * Clôt un dû qui n'a jamais été réglé.
     *
     * <p>Une demande refusée, ou annulée avant paiement, ne laisse rien à
     * rendre. L'état passe à {@code CANCELLED} plutôt que de rester
     * indéfiniment {@code DUE} : un montant dû sur une réservation morte
     * apparaîtrait comme un impayé.</p>
     */
    @Transactional
    public void annulerSiNonRegle(Booking reservation) {
        repository.findByBookingId(reservation.getId()).ifPresent(paiement -> {
            if (paiement.getStatus() == PaymentStatus.DUE) {
                paiement.setStatus(PaymentStatus.CANCELLED);
                repository.save(paiement);
            }
        });
    }

    // ──────────────────────────────── Barème ─────────────────────────────────

    /**
     * Part à rendre au passager, en pourcentage.
     *
     * <p>Publique et sans effet de bord : l'interface l'appelle pour annoncer
     * ce qui sera rendu <em>avant</em> que la personne confirme son annulation.
     * Découvrir après coup qu'on ne récupère que la moitié est le genre de
     * surprise qui vaut une réclamation.</p>
     *
     * @param annuleParLeConducteur vrai si la décision ne vient pas du passager
     */
    public int partRendue(LocalDateTime depart, boolean annuleParLeConducteur) {
        /* Le passager n'a rien décidé : il ne supporte aucun frais, quel que
           soit le moment. C'est la règle qui prime sur toutes les autres. */
        if (annuleParLeConducteur) return 100;

        LocalDateTime maintenant = LocalDateTime.now();
        if (depart.isBefore(maintenant)) return 0;

        long heures = Duration.between(maintenant, depart).toHours();
        return heures >= HEURES_REMBOURSEMENT_INTEGRAL ? 100 : PART_RENDUE_TARDIVE;
    }

    /**
     * Applique le barème et rend ce qui est dû.
     *
     * <p>Sans effet si rien n'a été réglé : on ne rembourse pas un montant
     * jamais prélevé, et le dû est simplement clos.</p>
     *
     * @param motif ce qui a déclenché le remboursement, conservé et affiché
     */
    @Transactional
    public Payment rembourser(Booking reservation, boolean annuleParLeConducteur, String motif) {
        Payment paiement = repository.findByBookingId(reservation.getId()).orElse(null);
        if (paiement == null) return null;

        if (paiement.getStatus() == PaymentStatus.DUE) {
            paiement.setStatus(PaymentStatus.CANCELLED);
            return repository.save(paiement);
        }
        if (!paiement.estRegle()) return paiement;

        int part = partRendue(reservation.getTrip().getDepartureTime(), annuleParLeConducteur);
        if (part == 0) {
            /* Rien à rendre, mais le motif est consigné : le passager doit
               pouvoir lire pourquoi il ne récupère rien. */
            paiement.setRefundReason(motif);
            return repository.save(paiement);
        }

        BigDecimal aRendre = paiement.getAmount()
                .multiply(BigDecimal.valueOf(part))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        String reference = gateway.rembourser(paiement, aRendre);

        paiement.setRefundedAmount(aRendre);
        paiement.setRefundedAt(LocalDateTime.now());
        paiement.setRefundReason(motif);
        /* Dans sa propre colonne : ecraser `providerReference` perdrait
           l'identifiant de l'operation d'origine, sur laquelle un vrai
           prestataire adosse ses remboursements — et qu'une notification
           arrivant en retard cherchera encore. */
        paiement.setRefundReference(reference);
        paiement.setStatus(part == 100 ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);

        log.info("Remboursement de {} % ({} {}) sur la réservation {} — {}",
                part, aRendre, paiement.getCurrency(), reservation.getUuid(), motif);

        return repository.save(paiement);
    }

    // ──────────────────────────────── Lecture ────────────────────────────────

    public Payment trouver(String bookingUuid) {
        return repository.findByBookingUuid(bookingUuid)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("paiement.introuvable")));
    }

    public Payment parReservation(Booking reservation) {
        return repository.findByBookingId(reservation.getId()).orElse(null);
    }
}
