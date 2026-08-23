package com.coshift.api.service;

import com.coshift.api.dto.AdminMemberResponse;
import com.coshift.api.dto.AdminOverviewResponse;
import com.coshift.api.entity.BookingStatus;
import com.coshift.api.entity.Organization;
import com.coshift.api.entity.Role;
import com.coshift.api.entity.User;
import com.coshift.api.exception.BadRequestException;
import com.coshift.api.exception.ResourceNotFoundException;
import com.coshift.api.exception.UnauthorizedException;
import com.coshift.api.repository.AdminStatsRepository;
import com.coshift.api.repository.UserRepository;
import com.coshift.api.security.LoginAttemptService;
import com.coshift.api.security.SecurityAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Supervision et modération.
 *
 * <h2>Deux rôles, deux portées</h2>
 *
 * <p>Le schéma déclarait {@code ADMIN} et {@code SUPER_ADMIN} depuis le début
 * sans que ni l'un ni l'autre n'ouvre le moindre écran : le seul contrôle de
 * rôle du projet portait sur {@code /actuator/**}. Ils prennent ici le sens que
 * leur donnait déjà le commentaire de l'énumération.</p>
 *
 * <ul>
 *   <li>{@code SUPER_ADMIN} répond de la plateforme et voit tout.</li>
 *   <li>{@code ADMIN} répond de ses organisations et ne voit qu'elles.</li>
 * </ul>
 *
 * <p>Cette borne n'est pas un confort d'affichage. Sans elle, distribuer un
 * rôle d'administrateur à une entreprise cliente lui ouvrirait les trajets et
 * les membres de toutes les autres — c'est-à-dire que le cercle fermé se
 * contournerait par un rôle au lieu de se contourner par une requête.</p>
 *
 * <h2>Ce que cette classe ne fait pas</h2>
 *
 * <p>Elle ne supprime rien, ne modifie aucun profil, ne lit aucun mot de passe
 * et n'expose pas le journal de sécurité. Une console d'administration est, par
 * construction, le point le plus intéressant à compromettre d'une application :
 * chaque pouvoir qu'on lui donne doit être justifié par un usage réel, pas
 * ajouté « au cas où ».</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    /** Identifiant qu'aucune organisation ne porte : les clés sont auto-incrémentées. */
    private static final long AUCUNE_ORGANISATION = -1L;

    /** Longueur maximale d'un motif, alignée sur la colonne. */
    private static final int MOTIF_MAX = 255;

    /** Bornes de pagination : au-delà, la console demande la base entière. */
    private static final int TAILLE_PAGE_MAX = 100;

    private final UserRepository userRepository;
    private final AdminStatsRepository statsRepository;
    private final OrganizationService organizationService;
    private final LoginAttemptService loginAttemptService;
    private final SecurityAuditService audit;
    private final Messages messages;

    // ─────────────────────────────── Portée ──────────────────────────────────

    private boolean estPlateforme(User administrateur) {
        return administrateur.getRole() == Role.SUPER_ADMIN;
    }

    /**
     * Organisations que l'administrateur supervise.
     *
     * <p>Jamais vide, pour la même raison que dans la recherche de trajets : un
     * {@code IN} vide n'est pas du SQL valide, et faire disparaître la clause
     * ouvrirait la supervision à tout le monde au lieu de la fermer.</p>
     */
    private List<Long> perimetre(User administrateur) {
        List<Long> siennes = organizationService.identifiantsDesOrganisations(administrateur);
        return siennes.isEmpty() ? List.of(AUCUNE_ORGANISATION) : siennes;
    }

    // ─────────────────────────── Vue de supervision ──────────────────────────

    public AdminOverviewResponse apercu(User administrateur) {
        boolean plateforme = estPlateforme(administrateur);
        List<Long> orgs = perimetre(administrateur);

        return AdminOverviewResponse.builder()
                .portee(plateforme ? "PLATEFORME" : "ORGANISATIONS")
                .organisations(plateforme ? List.of() :
                        administrateur.getOrganizations().stream()
                                .map(Organization::getName)
                                .sorted(String.CASE_INSENSITIVE_ORDER)
                                .toList())
                .membres(AdminOverviewResponse.Membres.builder()
                        .total(statsRepository.compterMembres(plateforme, orgs))
                        .verifies(statsRepository.compterMembresVerifies(plateforme, orgs))
                        .suspendus(statsRepository.compterMembresSuspendus(plateforme, orgs))
                        /* Un compte anonymisé n'appartient plus à aucun cercle :
                           le compter par organisation donnerait toujours zéro et
                           laisserait croire qu'aucun effacement n'a eu lieu. */
                        .effaces(plateforme ? statsRepository.compterMembresEfface() : 0)
                        .build())
                .trajets(AdminOverviewResponse.Trajets.builder()
                        .aVenir(statsRepository.compterTrajetsAVenir(plateforme, orgs))
                        .realises(statsRepository.compterTrajetsRealises(plateforme, orgs))
                        .annules(statsRepository.compterTrajetsAnnules(plateforme, orgs))
                        .sansOrganisation(plateforme ? statsRepository.compterTrajetsSansOrganisation() : 0)
                        .build())
                .reservations(AdminOverviewResponse.Reservations.builder()
                        .enAttente(statsRepository.compterReservations(BookingStatus.PENDING, plateforme, orgs))
                        .confirmees(statsRepository.compterReservations(BookingStatus.CONFIRMED, plateforme, orgs))
                        .honorees(statsRepository.compterReservations(BookingStatus.COMPLETED, plateforme, orgs))
                        .annulees(statsRepository.compterReservations(BookingStatus.CANCELLED, plateforme, orgs))
                        .build())
                .build();
    }

    // ──────────────────────────────── Membres ────────────────────────────────

    /**
     * Membres du périmètre, du plus récemment inscrit au plus ancien.
     *
     * <p>Cet ordre n'est pas cosmétique : les comptes qui demandent une
     * attention sont ceux qui viennent d'arriver, pas ceux inscrits depuis deux
     * ans.</p>
     */
    public Page<AdminMemberResponse> membres(User administrateur, String recherche, int page, int taille) {
        String motif = (recherche == null || recherche.isBlank())
                ? null
                : "%" + recherche.trim().toLowerCase() + "%";

        var pageable = PageRequest.of(
                Math.max(0, page),
                Math.min(Math.max(1, taille), TAILLE_PAGE_MAX),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return userRepository
                .rechercherPourAdministration(estPlateforme(administrateur), perimetre(administrateur), motif, pageable)
                .map(AdminMemberResponse::from);
    }

    // ─────────────────────────────── Modération ──────────────────────────────

    /**
     * Suspend un compte.
     *
     * <h2>Pourquoi seul un SUPER_ADMIN peut le faire</h2>
     *
     * <p>Consulter n'engage rien ; suspendre engage la plateforme vis-à-vis de
     * la personne. La décision revient donc à celui qui répond de la
     * plateforme, pas à celui qui répond d'une organisation — sans quoi une
     * entreprise cliente pourrait fermer le compte d'un de ses employés à
     * travers un outil qui n'est pas le sien.</p>
     *
     * <h2>Trois refus</h2>
     *
     * <p>Un motif vide est refusé : une décision sans raison écrite ne peut
     * plus être expliquée trois mois plus tard, ni à la personne, ni à un juge.
     * Se suspendre soi-même est refusé, parce que plus personne ne pourrait
     * lever la mesure. Suspendre un autre administrateur de plateforme est
     * refusé pour la même raison, portée au rang au-dessus : deux comptes de
     * supervision qui se neutralisent laissent l'application sans pilote.</p>
     */
    @Transactional
    public AdminMemberResponse suspendre(User administrateur, String uuid, String motif) {
        exigerSuperAdmin(administrateur);

        if (motif == null || motif.isBlank()) {
            throw new BadRequestException(messages.get("admin.motifRequis"));
        }
        String motifNet = motif.trim();
        if (motifNet.length() > MOTIF_MAX) {
            motifNet = motifNet.substring(0, MOTIF_MAX);
        }

        User cible = trouver(uuid);

        if (cible.getId().equals(administrateur.getId())) {
            throw new BadRequestException(messages.get("admin.suspensionDeSoi"));
        }
        if (cible.getRole() == Role.SUPER_ADMIN) {
            throw new UnauthorizedException(messages.get("admin.suspensionAdministrateur"));
        }
        if (cible.getSuspendedAt() != null) {
            /* Réécrire la date effacerait le « depuis quand » de la première
               mesure, qui est justement ce qu'on doit pouvoir produire. */
            throw new BadRequestException(messages.get("admin.dejaSuspendu"));
        }

        cible.setSuspendedAt(LocalDateTime.now());
        cible.setSuspensionReason(motifNet);
        userRepository.save(cible);

        audit.consigner(SecurityAuditService.Evenement.COMPTE_SUSPENDU, cible.getEmail(), "-",
                "par=" + administrateur.getEmail() + " motif=" + motifNet);
        log.info("Compte {} suspendu par {}", cible.getEmail(), administrateur.getEmail());

        return AdminMemberResponse.from(cible);
    }

    /**
     * Lève la suspension.
     *
     * <p>Le motif est effacé en même temps que la date : le conserver
     * laisserait sur un compte redevenu actif la trace d'une accusation à
     * laquelle rien ne correspond plus. L'événement, lui, reste au journal.</p>
     */
    @Transactional
    public AdminMemberResponse reactiver(User administrateur, String uuid) {
        exigerSuperAdmin(administrateur);

        User cible = trouver(uuid);
        if (cible.getSuspendedAt() == null) {
            throw new BadRequestException(messages.get("admin.pasSuspendu"));
        }

        cible.setSuspendedAt(null);
        cible.setSuspensionReason(null);
        userRepository.save(cible);

        audit.consigner(SecurityAuditService.Evenement.COMPTE_REACTIVE, cible.getEmail(), "-",
                "par=" + administrateur.getEmail());
        log.info("Compte {} réactivé par {}", cible.getEmail(), administrateur.getEmail());
        return AdminMemberResponse.from(cible);
    }

    // ─────────────────────────── Freinages en cours ──────────────────────────

    /**
     * Freinages de connexion actuellement en vigueur.
     *
     * <p>Réservé au {@code SUPER_ADMIN} : la liste porte des adresses IP et des
     * adresses électroniques, y compris de comptes qui ne relèvent d'aucune
     * organisation de l'appelant.</p>
     */
    public List<LoginAttemptService.Blocage> blocages(User administrateur) {
        exigerSuperAdmin(administrateur);
        return loginAttemptService.blocagesEnCours();
    }

    // ──────────────────────────────── Communs ────────────────────────────────

    private void exigerSuperAdmin(User administrateur) {
        if (administrateur.getRole() != Role.SUPER_ADMIN) {
            throw new UnauthorizedException(messages.get("admin.reserveSuperAdmin"));
        }
    }

    /**
     * Retrouve un compte par son identifiant public.
     *
     * <p>Un compte effacé est « introuvable » : l'anonymisation lui a retiré
     * nom et adresse, il n'y a plus rien à modérer et rien à afficher.</p>
     */
    private User trouver(String uuid) {
        User u = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("auth.utilisateurIntrouvable")));
        if (u.getDeletedAt() != null) {
            throw new ResourceNotFoundException(messages.get("auth.utilisateurIntrouvable"));
        }
        return u;
    }
}
