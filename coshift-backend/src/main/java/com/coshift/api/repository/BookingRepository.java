package com.coshift.api.repository;

import com.coshift.api.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Un passager veut voir son historique
    List<Booking> findByPassengerId(Long passengerId);

    // Un conducteur veut voir les réservations sur son trajet
    List<Booking> findByTripId(Long tripId);

    Optional<Booking> findByUuid(String uuid);
}