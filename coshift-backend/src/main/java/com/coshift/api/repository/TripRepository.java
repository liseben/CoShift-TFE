package com.coshift.api.repository;

import com.coshift.api.entity.Trip;
import com.coshift.api.entity.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    // F19 — Trajets proposés par un conducteur
    List<Trip> findByDriverIdOrderByDepartureTimeDesc(Long driverId);

    // F30 — Trajets via réservations (voir BookingRepository)

    // Trajet public par UUID
    Optional<Trip> findByUuid(String uuid);

    /**
     * F25 — Recherche de trajets.
     *
     * <p>Trois garde-fous s'appliquent systématiquement, quels que soient les
     * filtres saisis : seuls les trajets à venir remontent (auparavant, une
     * recherche sans date renvoyait aussi les trajets du mois dernier), le
     * conducteur ne voit jamais ses propres trajets dans les résultats, et un
     * trajet rattaché à une organisation n'apparaît qu'aux membres de
     * celle-ci.</p>
     *
     * <p>Ce troisième garde-fou est le cercle fermé, qui est la raison d'être
     * du produit : on ne monte pas dans la voiture d'un inconnu. La
     * documentation OpenAPI de {@code /api/trips/search} l'annonçait déjà
     * pendant que la requête renvoyait les trajets de tout le monde.</p>
     *
     * <p>Un trajet sans organisation reste visible de tous : son conducteur
     * n'appartenait à aucun cercle, il n'y a personne à qui le réserver.
     * Symétriquement, {@code organizationIds} ne doit jamais être vide —
     * l'appelant y met une valeur impossible plutôt qu'une liste vide, pour
     * que la clause reste un {@code IN} bien formé et que quelqu'un sans
     * organisation ne voie que les trajets sans organisation.</p>
     */
    @Query("""
            SELECT t FROM Trip t
            WHERE t.status = :status
            AND t.departureTime >= :now
            AND t.availableSeats > 0
            AND t.driver.id <> :currentUserId
            AND (t.organization IS NULL OR t.organization.id IN :organizationIds)
            AND (:departure IS NULL OR LOWER(t.departureCity) LIKE LOWER(CONCAT('%', :departure, '%')))
            AND (:arrival  IS NULL OR LOWER(t.arrivalCity)   LIKE LOWER(CONCAT('%', :arrival,  '%')))
            AND (:dateFrom IS NULL OR t.departureTime >= :dateFrom)
            AND (:dateTo   IS NULL OR t.departureTime < :dateTo)
            AND (:seats    IS NULL OR t.availableSeats >= :seats)
            ORDER BY t.departureTime ASC
            """)
    List<Trip> searchTrips(
            @Param("departure")     String departure,
            @Param("arrival")       String arrival,
            @Param("dateFrom")      LocalDateTime dateFrom,
            @Param("dateTo")        LocalDateTime dateTo,
            @Param("seats")         Integer seats,
            @Param("status")        TripStatus status,
            @Param("now")           LocalDateTime now,
            @Param("currentUserId") Long currentUserId,
            @Param("organizationIds") Collection<Long> organizationIds);

    // Multi-tenant
    List<Trip> findByOrganizationId(Long organizationId);

    /** Trajets encore ouverts alors que l'heure de départ est passée (tâche de clôture). */
    List<Trip> findByStatusInAndDepartureTimeBefore(Collection<TripStatus> statuses, LocalDateTime moment);

    /**
     * Trajets anciens portant encore une donnée à anonymiser.
     *
     * <p>La seconde condition évite de reprendre à chaque passage des milliers
     * de trajets déjà traités : sans elle, la tâche de rétention réécrirait
     * indéfiniment les mêmes lignes avec les mêmes valeurs nulles.</p>
     */
    @Query("""
            SELECT t FROM Trip t
            WHERE t.departureTime < :limite
              AND (t.departureAddress IS NOT NULL
                OR t.arrivalAddress   IS NOT NULL
                OR t.description      IS NOT NULL)
            """)
    List<Trip> findAnonymisables(@Param("limite") LocalDateTime limite);
}
