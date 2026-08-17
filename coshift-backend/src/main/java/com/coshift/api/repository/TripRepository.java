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
     * <p>Deux garde-fous s'appliquent systématiquement, quels que soient les
     * filtres saisis : seuls les trajets à venir remontent (auparavant, une
     * recherche sans date renvoyait aussi les trajets du mois dernier), et le
     * conducteur ne voit jamais ses propres trajets dans les résultats.</p>
     */
    @Query("""
            SELECT t FROM Trip t
            WHERE t.status = :status
            AND t.departureTime >= :now
            AND t.availableSeats > 0
            AND t.driver.id <> :currentUserId
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
            @Param("currentUserId") Long currentUserId);

    // Multi-tenant
    List<Trip> findByOrganizationId(Long organizationId);

    /** Trajets encore ouverts alors que l'heure de départ est passée (tâche de clôture). */
    List<Trip> findByStatusInAndDepartureTimeBefore(Collection<TripStatus> statuses, LocalDateTime moment);
}
