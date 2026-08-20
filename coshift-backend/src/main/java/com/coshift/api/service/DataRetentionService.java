package com.coshift.api.service;

import com.coshift.api.entity.Booking;
import com.coshift.api.entity.Trip;
import com.coshift.api.entity.User;
import com.coshift.api.repository.BookingRepository;
import com.coshift.api.repository.TripRepository;
import com.coshift.api.repository.UserRepository;
import com.coshift.api.security.SecurityAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Limitation de la durée de conservation — RGPD, article 5.1.e.
 *
 * <h2>Pourquoi une tâche plutôt qu'une phrase</h2>
 *
 * <p>Une politique de confidentialité peut annoncer n'importe quelle durée : ce
 * qui la rend vraie, c'est un mécanisme qui l'applique sans intervention. Avant
 * ce service, CoShift n'effaçait jamais rien. Une inscription abandonnée en
 * 2026 aurait gardé une adresse électronique indéfiniment, et un trajet de 2026
 * aurait dit encore en 2036 qui partait de quelle rue, à quelle heure, avec
 * qui.</p>
 *
 * <p>Le principe de l'article 5.1.e n'exige pas la suppression de tout : il
 * exige que les données ne soient pas conservées « sous une forme permettant
 * l'identification des personnes concernées pendant une durée excédant celle
 * nécessaire ». La formulation ouvre explicitement la voie à l'anonymisation
 * plutôt qu'à la destruction — c'est celle retenue pour les trajets.</p>
 *
 * <h2>Trois balayages</h2>
 *
 * <ol>
 *   <li><strong>Inscriptions jamais confirmées</strong>, au-delà de 30 jours :
 *       supprimées. Une adresse dont personne n'a prouvé qu'elle lui
 *       appartenait n'est pas un compte, et la garder revient à conserver la
 *       donnée d'une personne qui n'a peut-être rien demandé.</li>
 *   <li><strong>Codes expirés</strong> : effacés à chaque passage. Ils ne
 *       servent plus, et un code de réinitialisation qui traîne est un mot de
 *       passe secondaire oublié.</li>
 *   <li><strong>Trajets et réservations</strong> de plus de 24 mois :
 *       anonymisés. Les adresses précises et les motifs de refus disparaissent ;
 *       les villes, les dates et les comptages restent, pour les statistiques
 *       agrégées et les données ouvertes.</li>
 * </ol>
 *
 * <h2>Ce que la tâche ne fait pas</h2>
 *
 * <p>Elle ne touche pas aux comptes effacés à la demande de leur titulaire :
 * l'anonymisation y est déjà faite, immédiatement, par
 * {@link PersonalDataService}. Il n'y a pas de délai de grâce de trente jours,
 * et il ne peut pas y en avoir — un délai supposerait de conserver les données
 * pendant ce temps, ce qui contredirait la demande.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataRetentionService {

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final SecurityAuditService audit;

    /** Délai au-delà duquel une inscription non confirmée est supprimée. */
    @Value("${app.retention.unverified-days:30}")
    private int joursAvantPurgeInscription;

    /** Délai au-delà duquel un trajet passé est anonymisé. */
    @Value("${app.retention.trip-months:24}")
    private int moisAvantAnonymisationTrajet;

    /**
     * Passage quotidien, à 3 h 30.
     *
     * <p>L'heure est choisie creuse : la tâche parcourt trois tables et les
     * écrit, ce qui n'a pas sa place au milieu des connexions du matin.</p>
     */
    @Scheduled(cron = "${app.retention.cron:0 30 3 * * *}")
    @Transactional
    public void appliquerLesDurees() {
        log.info("Rétention : début du balayage");
        int inscriptions = purgerInscriptionsAbandonnees();
        int codes = effacerCodesExpires();
        int trajets = anonymiserTrajetsAnciens();
        log.info("Rétention : {} inscription(s) purgée(s), {} code(s) effacé(s), {} trajet(s) anonymisé(s)",
                inscriptions, codes, trajets);
    }

    /* ─────────────────────────────────────────────────────────────────────
       1. Inscriptions jamais confirmées
       ───────────────────────────────────────────────────────────────────── */

    private int purgerInscriptionsAbandonnees() {
        LocalDateTime limite = LocalDateTime.now().minusDays(joursAvantPurgeInscription);
        List<User> abandonnees = userRepository
                .findByEmailVerifiedFalseAndDeletedAtIsNullAndCreatedAtBefore(limite);

        /* Un compte jamais vérifié peut malgré tout avoir produit des données —
           le jeu de démonstration en contient. Supprimer sa ligne casserait les
           clés étrangères ; ces cas sont écartés du balayage et signalés. */
        List<User> supprimables = abandonnees.stream()
                .filter(u -> tripRepository.findByDriverIdOrderByDepartureTimeDesc(u.getId()).isEmpty())
                .filter(u -> bookingRepository.findByPassengerIdOrderByCreatedAtDesc(u.getId()).isEmpty())
                .toList();

        int ecartes = abandonnees.size() - supprimables.size();
        if (ecartes > 0) {
            log.warn("Rétention : {} inscription(s) non confirmée(s) conservée(s) faute de pouvoir "
                   + "supprimer la ligne — des trajets ou réservations y renvoient", ecartes);
        }

        for (User u : supprimables) {
            audit.consigner(SecurityAuditService.Evenement.COMPTE_PURGE, u.getUuid(), "-",
                    "inscription non confirmee depuis " + joursAvantPurgeInscription + " jours");
            /* Le rattachement doit tomber avant la ligne : la table de liaison
               porte une clé étrangère vers l'utilisateur. */
            u.getOrganizations().clear();
        }
        userRepository.deleteAll(supprimables);
        return supprimables.size();
    }

    /* ─────────────────────────────────────────────────────────────────────
       2. Codes de vérification et de réinitialisation expirés
       ───────────────────────────────────────────────────────────────────── */

    private int effacerCodesExpires() {
        LocalDateTime maintenant = LocalDateTime.now();
        List<User> porteurs = userRepository.findWithExpiredCodes(maintenant);

        for (User u : porteurs) {
            if (u.getVerificationCodeExpiry() != null
                    && u.getVerificationCodeExpiry().isBefore(maintenant)) {
                u.setVerificationCode(null);
                u.setVerificationCodeExpiry(null);
            }
            if (u.getPasswordResetExpiry() != null
                    && u.getPasswordResetExpiry().isBefore(maintenant)) {
                u.setPasswordResetCode(null);
                u.setPasswordResetExpiry(null);
            }
        }
        userRepository.saveAll(porteurs);
        return porteurs.size();
    }

    /* ─────────────────────────────────────────────────────────────────────
       3. Trajets et réservations de plus de 24 mois
       ───────────────────────────────────────────────────────────────────── */

    /**
     * Retire des trajets anciens ce qui désigne un lieu ou une personne.
     *
     * <p>Le conducteur reste rattaché : la relation est obligatoire, et elle ne
     * révèle plus rien une fois les adresses précises retirées et les motifs
     * effacés. Ce qui subsiste — une ville de départ, une ville d'arrivée, un
     * mois — est exactement ce qu'alimentent les données ouvertes.</p>
     */
    private int anonymiserTrajetsAnciens() {
        LocalDateTime limite = LocalDateTime.now().minusMonths(moisAvantAnonymisationTrajet);
        List<Trip> anciens = tripRepository.findAnonymisables(limite);

        for (Trip t : anciens) {
            /* Les adresses exactes désignent un domicile ou un lieu de travail ;
               la ville seule ne désigne personne. */
            t.setDepartureAddress(null);
            t.setArrivalAddress(null);
            /* Une description libre peut contenir n'importe quoi — un numéro de
               téléphone, un prénom, un point de rendez-vous précis. */
            t.setDescription(null);

            List<Booking> reservations = bookingRepository.findByTripIdOrderByCreatedAtDesc(t.getId());
            /* Le motif d'un refus est rédigé par un humain à propos d'un autre :
               c'est la donnée la plus sensible de la table. */
            reservations.forEach(b -> b.setStatusReason(null));
            bookingRepository.saveAll(reservations);
        }
        tripRepository.saveAll(anciens);
        return anciens.size();
    }
}
