package com.coshift.api.repository;

import com.coshift.api.entity.Trip;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Agrégats de mobilité d'une organisation.
 *
 * <h2>Ce qui distingue ces requêtes de celles du jeu de données ouvert</h2>
 *
 * <p>{@link OpenDataRepository} agrège <em>toute</em> la plateforme pour un
 * lecteur anonyme, et applique donc un seuil de k-anonymat. Ici, le lecteur est
 * membre de l'organisation qu'il consulte : les trajets comptés sont ceux qu'il
 * voit déjà un par un dans la recherche. Le seuil n'aurait pas de sens — il
 * masquerait des chiffres dont le détail est à portée de clic.</p>
 *
 * <p>Les définitions, en revanche, sont exactement les mêmes que celles du jeu
 * ouvert : « places partagées » compte les places occupées par une réservation
 * aboutie, « places restantes » celles encore libres, et les trajets annulés
 * sont comptés à part plutôt que fondus dans le volume. Deux chiffres portant
 * le même nom dans deux écrans du produit doivent se calculer pareil, faute de
 * quoi c'est le produit qui se contredit.</p>
 *
 * <p>Requêtes natives, comme celles du jeu ouvert et pour la même raison :
 * {@code DATE_FORMAT} n'a pas d'équivalent portable en JPQL. La somme des
 * places passe par une sous-requête corrélée plutôt que par une jointure, qui
 * multiplierait les lignes d'un trajet par son nombre de réservations.</p>
 */
public interface OrganizationStatsRepository extends Repository<Trip, Long> {

    /** Personnes rattachées à l'organisation, qu'elles aient roulé ou non. */
    @Query(value = """
            SELECT COUNT(*) FROM organization_members WHERE organization_id = :org
            """, nativeQuery = true)
    long compterMembres(@Param("org") Long org);

    /** Trajets ouverts à l'organisation, hors annulés. */
    @Query(value = """
            SELECT COUNT(*) FROM trips
            WHERE organization_id = :org AND status <> 'CANCELLED'
            """, nativeQuery = true)
    long compterTrajets(@Param("org") Long org);

    /** Trajets annulés, comptés à part : les fondre dans le volume le gonflerait. */
    @Query(value = """
            SELECT COUNT(*) FROM trips
            WHERE organization_id = :org AND status = 'CANCELLED'
            """, nativeQuery = true)
    long compterTrajetsAnnules(@Param("org") Long org);

    /** Trajets dont l'heure de départ est passée sans annulation. */
    @Query(value = """
            SELECT COUNT(*) FROM trips
            WHERE organization_id = :org AND status = 'COMPLETED'
            """, nativeQuery = true)
    long compterTrajetsRealises(@Param("org") Long org);

    /** Places effectivement occupées par un passager sur un trajet non annulé. */
    @Query(value = """
            SELECT COALESCE(SUM(b.seats_booked), 0)
            FROM bookings b
            JOIN trips t ON t.id = b.trip_id
            WHERE t.organization_id = :org
              AND b.status IN ('CONFIRMED', 'COMPLETED')
              AND t.status <> 'CANCELLED'
            """, nativeQuery = true)
    long compterPlacesPartagees(@Param("org") Long org);

    /** Places encore libres sur les trajets non annulés. */
    @Query(value = """
            SELECT COALESCE(SUM(available_seats), 0)
            FROM trips
            WHERE organization_id = :org AND status <> 'CANCELLED'
            """, nativeQuery = true)
    long compterPlacesRestantes(@Param("org") Long org);

    /** Membres ayant publié au moins un trajet non annulé. */
    @Query(value = """
            SELECT COUNT(DISTINCT driver_id) FROM trips
            WHERE organization_id = :org AND status <> 'CANCELLED'
            """, nativeQuery = true)
    long compterConducteurs(@Param("org") Long org);

    /** Membres ayant occupé au moins une place. */
    @Query(value = """
            SELECT COUNT(DISTINCT b.passenger_id)
            FROM bookings b
            JOIN trips t ON t.id = b.trip_id
            WHERE t.organization_id = :org
              AND b.status IN ('CONFIRMED', 'COMPLETED')
              AND t.status <> 'CANCELLED'
            """, nativeQuery = true)
    long compterPassagers(@Param("org") Long org);

    /**
     * Volume par mois de départ : mois (AAAA-MM), trajets, places partagées.
     *
     * <p>Le mois de <em>départ</em> et non celui de publication : c'est le
     * déplacement qui intéresse un employeur, pas le moment où quelqu'un a
     * rempli un formulaire.</p>
     */
    @Query(value = """
            SELECT DATE_FORMAT(t.departure_time, '%Y-%m') AS mois,
                   COUNT(*) AS trajets,
                   COALESCE(SUM((SELECT COALESCE(SUM(b.seats_booked), 0)
                                 FROM bookings b
                                 WHERE b.trip_id = t.id
                                   AND b.status IN ('CONFIRMED', 'COMPLETED'))), 0) AS places
            FROM trips t
            WHERE t.organization_id = :org AND t.status <> 'CANCELLED'
            GROUP BY mois
            ORDER BY mois
            """, nativeQuery = true)
    List<Object[]> volumeParMois(@Param("org") Long org);
}
