package com.coshift.api.service;

import com.coshift.api.dto.TripRequest;
import com.coshift.api.dto.TripResponse;
import com.coshift.api.entity.*;
import com.coshift.api.exception.BadRequestException;
import com.coshift.api.exception.ConflictException;
import com.coshift.api.exception.ResourceNotFoundException;
import com.coshift.api.exception.UnauthorizedException;
import com.coshift.api.repository.BookingRepository;
import com.coshift.api.repository.TripRepository;
import com.coshift.api.repository.UserRepository;
import com.coshift.api.repository.VehiculeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {

    private final TripRepository tripRepository;
    private final Messages messages;
    private final UserRepository userRepository;
    private final VehiculeRepository vehiculeRepository;
    private final BookingRepository bookingRepository;
    private final EmailService emailService;

    /** Délai minimum entre la publication et le départ, imposé par F16. */
    private static final int MIN_HOURS_BEFORE_DEPARTURE = 2;

    /** Nombre maximum de trajets simultanément actifs par conducteur (F16). */
    private static final int MAX_ACTIVE_TRIPS = 5;

    // F16 — Publier un trajet
    @Transactional
    public TripResponse publishTrip(String driverEmail, TripRequest request) {
        User driver = findUser(driverEmail);

        // La règle des 2 h n'était appliquée que par l'attribut `min` du champ
        // datetime-local côté React, donc contournable par un simple appel API.
        LocalDateTime earliest = LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_DEPARTURE);
        if (request.getDepartureTime().isBefore(earliest)) {
            throw new BadRequestException(
                    messages.get("trajet.delaiPublication", MIN_HOURS_BEFORE_DEPARTURE));
        }

        // Max 5 trajets actifs simultanément (règle cahier des charges)
        long activeTrips = tripRepository.findByDriverIdOrderByDepartureTimeDesc(driver.getId())
                .stream()
                .filter(t -> t.getStatus() == TripStatus.PLANNED || t.getStatus() == TripStatus.FULL)
                .count();
        if (activeTrips >= MAX_ACTIVE_TRIPS) {
            throw new ConflictException(messages.get("trajet.quotaAtteint", MAX_ACTIVE_TRIPS));
        }

        Vehicule vehicule = vehiculeRepository.findByUuid(request.getVehiculeUuid())
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("trajet.vehiculeIntrouvable")));

        if (!vehicule.getOwner().getId().equals(driver.getId())) {
            throw new UnauthorizedException(messages.get("trajet.vehiculeAutrui"));
        }

        // Le conducteur occupe une place : on ne peut pas proposer plus de sièges
        // qu'il n'en reste réellement. Rien ne l'empêchait jusqu'ici de publier
        // 8 places dans une voiture qui en déclare 2.
        int maxPassengers = vehicule.getSeats() - 1;
        if (request.getAvailableSeats() > maxPassengers) {
            throw new BadRequestException(messages.get("trajet.placesSuperieuresAuVehicule",
                    vehicule.getSeats(), maxPassengers));
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
    public List<TripResponse> searchTrips(String searcherEmail, String departure, String arrival,
                                          LocalDate date, Integer seats) {
        User searcher = findUser(searcherEmail);

        LocalDateTime dateFrom = (date != null) ? date.atStartOfDay() : null;
        LocalDateTime dateTo   = (date != null) ? date.plusDays(1).atStartOfDay() : null;

        return tripRepository.searchTrips(
                blankToNull(departure), blankToNull(arrival),
                dateFrom, dateTo, seats,
                TripStatus.PLANNED,
                LocalDateTime.now(),
                searcher.getId())
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
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("trajet.introuvable")));
        return TripResponse.from(trip);
    }

    // F18 — Annuler un trajet
    @Transactional
    public TripResponse cancelTrip(String driverEmail, String uuid) {
        User driver = findUser(driverEmail);
        Trip trip = tripRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("trajet.introuvable")));

        if (!trip.getDriver().getId().equals(driver.getId())) {
            throw new UnauthorizedException(messages.get("trajet.pasConducteur"));
        }
        if (trip.getDepartureTime().isBefore(LocalDateTime.now())) {
            throw new ConflictException(messages.get("trajet.dejaPasse"));
        }

        // Les réservations en cours doivent suivre le sort du trajet : sans ça,
        // un passager gardait une réservation « confirmée » sur un trajet annulé.
        var impacted = bookingRepository.findByTripIdAndStatusIn(
                trip.getId(), List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));

        impacted.forEach(booking -> {
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setStatusReason(messages.get("trajet.annuleParConducteur"));
        });
        bookingRepository.saveAll(impacted);

        if (!impacted.isEmpty()) {
            log.info("{} réservation(s) annulée(s) suite à l'annulation du trajet {}",
                    impacted.size(), trip.getUuid());

            /* La plus importante des notifications : sans elle, quelqu'un
               attend à un point de rendez-vous où personne ne viendra. Chaque
               passager est prévenu dans SA langue — celle du conducteur qui
               annule n'a rien à voir ici. */
            String resume = resume(trip);
            impacted.forEach(booking ->
                    emailService.notifierTrajetAnnule(booking.getPassenger(), resume));
        }

        trip.setStatus(TripStatus.CANCELLED);
        return TripResponse.from(tripRepository.save(trip));
    }

    /**
     * Clôture automatiquement les trajets dont l'heure de départ est passée.
     *
     * <p>Sans cette tâche, un trajet gardait le statut PLANNED indéfiniment :
     * il continuait de compter dans le quota de 5 trajets actifs du conducteur
     * et restait éligible à la réservation. La bascule en COMPLETED est purement
     * technique — la confirmation de prestation par le passager relève de F21.</p>
     */
    @Scheduled(cron = "${app.trips.closing-cron:0 */15 * * * *}")
    @Transactional
    public void closePastTrips() {
        List<Trip> expired = tripRepository.findByStatusInAndDepartureTimeBefore(
                List.of(TripStatus.PLANNED, TripStatus.FULL), LocalDateTime.now());

        if (expired.isEmpty()) return;

        expired.forEach(trip -> trip.setStatus(TripStatus.COMPLETED));
        tripRepository.saveAll(expired);
        log.info("{} trajet(s) dont l'heure de départ est passée ont été clôturés.", expired.size());
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("auth.utilisateurIntrouvable")));
    }

    /** Rappel du trajet, tel qu'il apparaît dans un courriel. */
    private String resume(Trip trip) {
        String quand = (trip.getDepartureTime() == null) ? "" :
                trip.getDepartureTime().format(
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH'h'mm"));
        return trip.getDepartureCity() + " → " + trip.getArrivalCity()
                + (quand.isEmpty() ? "" : "<br>" + quand);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
