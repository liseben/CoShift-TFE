package com.coshift.api.service;

import com.coshift.api.dto.TripRequest;
import com.coshift.api.dto.TripResponse;
import com.coshift.api.entity.*;
import com.coshift.api.exception.ConflictException;
import com.coshift.api.exception.ResourceNotFoundException;
import com.coshift.api.exception.UnauthorizedException;
import com.coshift.api.repository.TripRepository;
import com.coshift.api.repository.UserRepository;
import com.coshift.api.repository.VehiculeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final VehiculeRepository vehiculeRepository;

    // F16 — Publier un trajet
    @Transactional
    public TripResponse publishTrip(String driverEmail, TripRequest request) {
        User driver = findUser(driverEmail);

        // Max 5 trajets actifs simultanément (règle cahier des charges)
        long activeTrips = tripRepository.findByDriverIdOrderByDepartureTimeDesc(driver.getId())
                .stream()
                .filter(t -> t.getStatus() == TripStatus.PLANNED || t.getStatus() == TripStatus.FULL)
                .count();
        if (activeTrips >= 5) {
            throw new ConflictException("Vous ne pouvez pas avoir plus de 5 trajets actifs simultanément.");
        }

        Vehicule vehicule = vehiculeRepository.findByUuid(request.getVehiculeUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule introuvable."));

        if (!vehicule.getOwner().getId().equals(driver.getId())) {
            throw new UnauthorizedException("Ce véhicule ne vous appartient pas.");
        }

        Trip trip = Trip.builder()
                .departureCity(request.getDepartureCity())
                .departureAddress(request.getDepartureAddress())
                .arrivalCity(request.getArrivalCity())
                .arrivalAddress(request.getArrivalAddress())
                .departureTime(request.getDepartureTime())
                .availableSeats(request.getAvailableSeats())
                .pricePerSeat(request.getPricePerSeat())
                .description(request.getDescription())
                .acceptsLuggage(request.isAcceptsLuggage())
                .acceptsPets(request.isAcceptsPets())
                .musicAllowed(request.isMusicAllowed())
                .talkingAllowed(request.isTalkingAllowed())
                .driver(driver)
                .vehicule(vehicule)
                .status(TripStatus.PLANNED)
                .build();

        return TripResponse.from(tripRepository.save(trip));
    }

    // F25 — Rechercher des trajets
    public List<TripResponse> searchTrips(String departure, String arrival, LocalDate date, Integer seats) {
        LocalDateTime dateFrom = (date != null) ? date.atStartOfDay() : null;
        LocalDateTime dateTo   = (date != null) ? date.plusDays(1).atStartOfDay() : null;

        return tripRepository.searchTrips(
                blankToNull(departure), blankToNull(arrival),
                dateFrom, dateTo, seats,
                TripStatus.PLANNED)
                .stream()
                .map(TripResponse::from)
                .toList();
    }

    // F19 — Mes trajets proposés (conducteur)
    public List<TripResponse> getMyTrips(String driverEmail) {
        User driver = findUser(driverEmail);
        return tripRepository.findByDriverIdOrderByDepartureTimeDesc(driver.getId())
                .stream()
                .map(TripResponse::from)
                .toList();
    }

    // F26 — Détail d'un trajet
    public TripResponse getTripByUuid(String uuid) {
        Trip trip = tripRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Trajet introuvable."));
        return TripResponse.from(trip);
    }

    // F18 — Annuler un trajet
    @Transactional
    public TripResponse cancelTrip(String driverEmail, String uuid) {
        User driver = findUser(driverEmail);
        Trip trip = tripRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Trajet introuvable."));

        if (!trip.getDriver().getId().equals(driver.getId())) {
            throw new UnauthorizedException("Vous n'êtes pas le conducteur de ce trajet.");
        }
        if (trip.getDepartureTime().isBefore(LocalDateTime.now())) {
            throw new ConflictException("Impossible d'annuler un trajet déjà passé.");
        }

        trip.setStatus(TripStatus.CANCELLED);
        return TripResponse.from(tripRepository.save(trip));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
