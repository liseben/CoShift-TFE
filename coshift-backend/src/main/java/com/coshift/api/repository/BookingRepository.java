package com.coshift.api.repository;

import com.coshift.api.entity.Booking;
import com.coshift.api.entity.BookingStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByUuid(String uuid);

    // F30 — Un passager consulte l'historique de ses réservations
    List<Booking> findByPassengerIdOrderByCreatedAtDesc(Long passengerId);

    // F19 — Un conducteur consulte les réservations reçues sur un trajet donné
    List<Booking> findByTripIdOrderByCreatedAtDesc(Long tripId);

    /**
     * F19 — Toutes les demandes reçues par un conducteur, tous trajets confondus.
     * Le tri place les demandes en attente en tête : ce sont celles qui réclament
     * une action de sa part.
     */
    @Query("""
            SELECT b FROM Booking b
            WHERE b.trip.driver.id = :driverId
            ORDER BY CASE WHEN b.status = com.coshift.api.entity.BookingStatus.PENDING
                          THEN 0 ELSE 1 END,
                     b.trip.departureTime ASC
            """)
    List<Booking> findAllForDriver(@Param("driverId") Long driverId);

    /**
     * Empêche un passager de déposer deux demandes sur le même trajet (F27).
     * Seules les réservations encore vivantes comptent : après une annulation ou
     * un refus, il doit pouvoir retenter sa chance.
     */
    boolean existsByTripIdAndPassengerIdAndStatusIn(
            Long tripId, Long passengerId, Collection<BookingStatus> statuses);

    /** Réservations à traiter lorsqu'un conducteur annule son trajet (F18). */
    List<Booking> findByTripIdAndStatusIn(Long tripId, Collection<BookingStatus> statuses);
}
