package com.coshift.api.repository;

import com.coshift.api.entity.Trip;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Requêtes d'agrégation servant le jeu de données ouvert.
 *
 * <p>Toutes renvoient des <strong>agrégats</strong> et jamais une ligne
 * identifiable : ni conducteur, ni passager, ni adresse précise, ni horaire
 * exact. Les trajets annulés sont exclus partout — publier un trajet qui n'a
 * pas eu lieu fausserait toute lecture des volumes.</p>
 *
 * <p>Requêtes natives et non JPQL : {@code DATE_FORMAT} et {@code HAVING} sur
 * un alias d'agrégat n'ont pas d'équivalent portable en JPQL. Le prix étant
 * moyenné par trajet, la somme des places passe par une sous-requête corrélée
 * plutôt que par une jointure — une jointure multiplierait les lignes d'un
 * trajet par son nombre de réservations et fausserait la moyenne du prix.</p>
 */
public interface OpenDataRepository extends Repository<Trip, Long> {

    /** Nombre de trajets publiés, hors annulés. */
    @Query(value = "SELECT COUNT(*) FROM trips WHERE status <> 'CANCELLED'", nativeQuery = true)
    long compterTrajets();

    /** Nombre de trajets annulés, publié séparément par honnêteté statistique. */
    @Query(value = "SELECT COUNT(*) FROM trips WHERE status = 'CANCELLED'", nativeQuery = true)
    long compterTrajetsAnnules();

    /** Réservations effectivement honorées, seules à témoigner d'un partage réel. */
    @Query(value = """
            SELECT COUNT(*) FROM bookings
            WHERE status IN ('CONFIRMED', 'COMPLETED')
            """, nativeQuery = true)
    long compterReservationsAbouties();

    /** Places occupées par un passager sur un trajet non annulé. */
    @Query(value = """
            SELECT COALESCE(SUM(b.seats_booked), 0)
            FROM bookings b
            JOIN trips t ON t.id = b.trip_id
            WHERE b.status IN ('CONFIRMED', 'COMPLETED') AND t.status <> 'CANCELLED'
            """, nativeQuery = true)
    long compterPlacesPartagees();

    /** Places encore libres sur les trajets non annulés. */
    @Query(value = """
            SELECT COALESCE(SUM(available_seats), 0)
            FROM trips WHERE status <> 'CANCELLED'
            """, nativeQuery = true)
    long compterPlacesRestantes();

    /** Nombre d'organisations ayant au moins un trajet publié. */
    @Query(value = """
            SELECT COUNT(DISTINCT organization_id)
            FROM trips WHERE organization_id IS NOT NULL AND status <> 'CANCELLED'
            """, nativeQuery = true)
    long compterOrganisations();

    /** Bornes temporelles du jeu de données : [0] = premier départ, [1] = dernier. */
    @Query(value = """
            SELECT MIN(departure_time), MAX(departure_time)
            FROM trips WHERE status <> 'CANCELLED'
            """, nativeQuery = true)
    List<Object[]> bornesTemporelles();

    /**
     * Volume par mois de départ : mois (AAAA-MM), trajets, places partagées.
     */
    @Query(value = """
            SELECT DATE_FORMAT(t.departure_time, '%Y-%m') AS mois,
                   COUNT(*) AS trajets,
                   COALESCE(SUM((SELECT COALESCE(SUM(b.seats_booked), 0)
                                 FROM bookings b
                                 WHERE b.trip_id = t.id
                                   AND b.status IN ('CONFIRMED', 'COMPLETED'))), 0) AS places
            FROM trips t
            WHERE t.status <> 'CANCELLED'
            GROUP BY DATE_FORMAT(t.departure_time, '%Y-%m')
            ORDER BY mois
            """, nativeQuery = true)
    List<Object[]> volumeParMois();

    /**
     * Villes desservies : ville, trajets au départ, trajets à l'arrivée, places
     * partagées au départ, prix moyen d'une place au départ.
     *
     * <p>L'agrégation porte sur la <strong>ville</strong> et non sur le couple
     * départ-arrivée. Ce n'est pas un choix de confort : au volume actuel, la
     * liaison la plus empruntée compte deux trajets. Publier « Nivelles →
     * Tournai, un trajet, une place » décrit un déplacement individuel, qu'un
     * annuaire d'entreprise suffirait à rattacher à quelqu'un. La ville, elle,
     * regroupe assez de trajets pour que personne n'y soit reconnaissable.</p>
     *
     * <p>La clause {@code HAVING} applique malgré tout le seuil de k-anonymat :
     * une ville trop peu desservie reste écartée.</p>
     */
    @Query(value = """
            SELECT v.ville                                  AS ville,
                   SUM(v.au_depart)                         AS auDepart,
                   SUM(v.a_l_arrivee)                       AS aLArrivee,
                   COALESCE(SUM(v.places), 0)               AS places,
                   (SELECT ROUND(AVG(t2.price_per_seat), 2)
                      FROM trips t2
                     WHERE t2.departure_city = v.ville
                       AND t2.status <> 'CANCELLED')        AS prixMoyen
            FROM (
                SELECT t.departure_city AS ville, 1 AS au_depart, 0 AS a_l_arrivee,
                       (SELECT COALESCE(SUM(b.seats_booked), 0)
                          FROM bookings b
                         WHERE b.trip_id = t.id
                           AND b.status IN ('CONFIRMED', 'COMPLETED')) AS places
                  FROM trips t WHERE t.status <> 'CANCELLED'
                UNION ALL
                SELECT t.arrival_city, 0, 1, 0
                  FROM trips t WHERE t.status <> 'CANCELLED'
            ) v
            GROUP BY v.ville
            HAVING (SUM(v.au_depart) + SUM(v.a_l_arrivee)) >= :seuil
            ORDER BY (SUM(v.au_depart) + SUM(v.a_l_arrivee)) DESC, v.ville ASC
            """, nativeQuery = true)
    List<Object[]> villesDesservies(@Param("seuil") int seuil);

    /** Villes écartées par le seuil, publiées pour que le tri reste lisible. */
    @Query(value = """
            SELECT COUNT(*) FROM (
                SELECT v.ville
                FROM (
                    SELECT departure_city AS ville FROM trips WHERE status <> 'CANCELLED'
                    UNION ALL
                    SELECT arrival_city   AS ville FROM trips WHERE status <> 'CANCELLED'
                ) v
                GROUP BY v.ville
                HAVING COUNT(*) < :seuil
            ) AS ecartees
            """, nativeQuery = true)
    long compterVillesEcartees(@Param("seuil") int seuil);

    /** Nombre de couples départ-arrivée distincts recensés. */
    @Query(value = """
            SELECT COUNT(*) FROM (
                SELECT 1 FROM trips WHERE status <> 'CANCELLED'
                GROUP BY departure_city, arrival_city
            ) AS couples
            """, nativeQuery = true)
    long compterLiaisonsRecensees();

    /** Couples départ-arrivée atteignant le seuil, et donc publiables un jour. */
    @Query(value = """
            SELECT COUNT(*) FROM (
                SELECT 1 FROM trips WHERE status <> 'CANCELLED'
                GROUP BY departure_city, arrival_city
                HAVING COUNT(*) >= :seuil
            ) AS publiables
            """, nativeQuery = true)
    long compterLiaisonsPubliables(@Param("seuil") int seuil);
}
