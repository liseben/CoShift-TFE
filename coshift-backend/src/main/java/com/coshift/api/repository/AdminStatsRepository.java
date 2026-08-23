package com.coshift.api.repository;

import com.coshift.api.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

/**
 * Comptages de supervision, bornés au périmètre de l'administrateur.
 *
 * <h2>Le paramètre {@code plateforme}</h2>
 *
 * <p>Chaque requête accepte un drapeau et une liste d'organisations. Le drapeau
 * lève la restriction pour un {@code SUPER_ADMIN}, qui répond de tout ; sinon
 * seules les organisations passées comptent. Écrire deux jeux de requêtes,
 * l'un global et l'autre borné, aurait doublé la surface à maintenir et ouvert
 * la possibilité qu'ils divergent — c'est-à-dire qu'un chiffre ne veuille pas
 * dire la même chose selon qui le regarde.</p>
 *
 * <p>{@code organisations} n'est jamais vide : l'appelant y met une valeur
 * impossible plutôt qu'une liste vide, sans quoi le {@code IN} ne serait pas du
 * SQL valide. Un administrateur sans organisation compte alors zéro, ce qui est
 * la réponse juste.</p>
 *
 * <p>Les comptes effacés au titre de l'article 17 sont exclus partout, sauf du
 * comptage qui leur est propre. Un compte anonymisé n'est plus une personne à
 * modérer : il n'a plus de nom, plus d'adresse, et le lister serait sans objet
 * autant que sans droit.</p>
 */
public interface AdminStatsRepository extends Repository<User, Long> {

    @Query("""
            SELECT COUNT(u) FROM User u
            WHERE u.deletedAt IS NULL
              AND (:plateforme = TRUE
                   OR EXISTS (SELECT 1 FROM User m JOIN m.organizations o
                              WHERE m.id = u.id AND o.id IN :organisations))
            """)
    long compterMembres(@Param("plateforme") boolean plateforme,
                        @Param("organisations") Collection<Long> organisations);

    @Query("""
            SELECT COUNT(u) FROM User u
            WHERE u.deletedAt IS NULL AND u.emailVerified = TRUE
              AND (:plateforme = TRUE
                   OR EXISTS (SELECT 1 FROM User m JOIN m.organizations o
                              WHERE m.id = u.id AND o.id IN :organisations))
            """)
    long compterMembresVerifies(@Param("plateforme") boolean plateforme,
                                @Param("organisations") Collection<Long> organisations);

    @Query("""
            SELECT COUNT(u) FROM User u
            WHERE u.deletedAt IS NULL AND u.suspendedAt IS NOT NULL
              AND (:plateforme = TRUE
                   OR EXISTS (SELECT 1 FROM User m JOIN m.organizations o
                              WHERE m.id = u.id AND o.id IN :organisations))
            """)
    long compterMembresSuspendus(@Param("plateforme") boolean plateforme,
                                 @Param("organisations") Collection<Long> organisations);

    /**
     * Comptes anonymisés.
     *
     * <p>Non borné aux organisations : l'anonymisation vide justement le
     * rattachement, si bien qu'un compte effacé n'appartient plus à aucun
     * cercle et disparaîtrait de tout comptage borné. Le chiffre n'est donc
     * exposé qu'à la portée plateforme.</p>
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.deletedAt IS NOT NULL")
    long compterMembresEfface();

    @Query("""
            SELECT COUNT(t) FROM Trip t
            WHERE t.status = com.coshift.api.entity.TripStatus.PLANNED
              AND t.departureTime >= CURRENT_TIMESTAMP
              AND (:plateforme = TRUE OR t.organization.id IN :organisations)
            """)
    long compterTrajetsAVenir(@Param("plateforme") boolean plateforme,
                              @Param("organisations") Collection<Long> organisations);

    @Query("""
            SELECT COUNT(t) FROM Trip t
            WHERE t.status = com.coshift.api.entity.TripStatus.COMPLETED
              AND (:plateforme = TRUE OR t.organization.id IN :organisations)
            """)
    long compterTrajetsRealises(@Param("plateforme") boolean plateforme,
                                @Param("organisations") Collection<Long> organisations);

    @Query("""
            SELECT COUNT(t) FROM Trip t
            WHERE t.status = com.coshift.api.entity.TripStatus.CANCELLED
              AND (:plateforme = TRUE OR t.organization.id IN :organisations)
            """)
    long compterTrajetsAnnules(@Param("plateforme") boolean plateforme,
                               @Param("organisations") Collection<Long> organisations);

    /**
     * Trajets ouverts à personne en particulier.
     *
     * <p>Visible seulement à la portée plateforme : par définition, ces trajets
     * n'appartiennent à aucune organisation, donc à aucun périmètre borné. Le
     * chiffre intéresse la supervision — il compte les publications faites par
     * des comptes dont l'adresse ne relève d'aucune organisation inscrite.</p>
     */
    @Query("SELECT COUNT(t) FROM Trip t WHERE t.organization IS NULL AND t.status <> com.coshift.api.entity.TripStatus.CANCELLED")
    long compterTrajetsSansOrganisation();

    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.status = :statut
              AND (:plateforme = TRUE OR b.trip.organization.id IN :organisations)
            """)
    long compterReservations(@Param("statut") com.coshift.api.entity.BookingStatus statut,
                             @Param("plateforme") boolean plateforme,
                             @Param("organisations") Collection<Long> organisations);
}
