package com.coshift.api.service;

import com.coshift.api.dto.BookingRequest;
import com.coshift.api.dto.BookingResponse;
import com.coshift.api.entity.Booking;
import com.coshift.api.entity.BookingStatus;
import com.coshift.api.entity.Trip;
import com.coshift.api.entity.TripStatus;
import com.coshift.api.entity.User;
import com.coshift.api.exception.BadRequestException;
import com.coshift.api.exception.ConflictException;
import com.coshift.api.exception.NoSeatsAvailableException;
import com.coshift.api.exception.ResourceNotFoundException;
import com.coshift.api.exception.UnauthorizedException;
import com.coshift.api.repository.BookingRepository;
import com.coshift.api.repository.TripRepository;
import com.coshift.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cycle de vie des réservations (F19, F20, F27, F29, F30).
 *
 * <p>Le nombre de places restantes est porté par {@code Trip.availableSeats} et
 * n'est décrémenté qu'à l'acceptation par le conducteur, jamais à la simple
 * demande : une demande en attente ne doit pas immobiliser une place qui
 * pourrait profiter à un autre passager. La disponibilité est donc vérifiée
 * deux fois — à la demande, pour éviter les demandes manifestement vaines, puis
 * à l'acceptation, qui fait seule autorité.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    /** Délai minimum entre la réservation et le départ, imposé par F27. */
    private static final int MIN_HOURS_BEFORE_BOOKING = 1;

    /** Statuts pour lesquels une réservation occupe encore le passager sur ce trajet. */
    private static final List<BookingStatus> ACTIVE_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    // ─────────────────────────────── F27 — Réserver ───────────────────────────────

    @Transactional
    public BookingResponse book(String passengerEmail, BookingRequest request) {
        User passenger = findUser(passengerEmail);
        Trip trip = tripRepository.findByUuid(request.getTripUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Trajet introuvable."));

        if (trip.getDriver().getId().equals(passenger.getId())) {
            throw new BadRequestException("Vous ne pouvez pas réserver une place dans votre propre trajet.");
        }
        if (trip.getStatus() != TripStatus.PLANNED) {
            throw new ConflictException("Ce trajet n'accepte plus de réservation.");
        }
        if (trip.getDepartureTime().isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_BOOKING))) {
            throw new ConflictException("Une réservation doit être faite au moins "
                    + MIN_HOURS_BEFORE_BOOKING + " heure avant le départ.");
        }
        if (bookingRepository.existsByTripIdAndPassengerIdAndStatusIn(
                trip.getId(), passenger.getId(), ACTIVE_STATUSES)) {
            throw new ConflictException("Vous avez déjà une réservation en cours sur ce trajet.");
        }
        if (request.getSeatsBooked() > trip.getAvailableSeats()) {
            throw new NoSeatsAvailableException("Il ne reste que " + trip.getAvailableSeats()
                    + " place(s) disponible(s) sur ce trajet.");
        }

        BigDecimal total = trip.getPricePerSeat()
                .multiply(BigDecimal.valueOf(request.getSeatsBooked()));

        Booking booking = Booking.builder()
                .trip(trip)
                .passenger(passenger)
                .seatsBooked(request.getSeatsBooked())
                .totalPrice(total)
                .status(BookingStatus.PENDING)
                .build();

        log.info("Nouvelle demande de réservation de {} place(s) sur le trajet {}",
                request.getSeatsBooked(), trip.getUuid());

        return BookingResponse.forPassenger(bookingRepository.save(booking));
    }

    // ────────────────────────── F30 — Mes réservations ───────────────────────────

    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(String passengerEmail) {
        User passenger = findUser(passengerEmail);
        return bookingRepository.findByPassengerIdOrderByCreatedAtDesc(passenger.getId())
                .stream()
                .map(BookingResponse::forPassenger)
                .toList();
    }

    // ─────────────────── F19 — Les demandes reçues par le conducteur ──────────────

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsReceived(String driverEmail) {
        User driver = findUser(driverEmail);
        return bookingRepository.findAllForDriver(driver.getId())
                .stream()
                .map(BookingResponse::forDriver)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsForTrip(String driverEmail, String tripUuid) {
        User driver = findUser(driverEmail);
        Trip trip = tripRepository.findByUuid(tripUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Trajet introuvable."));

        if (!trip.getDriver().getId().equals(driver.getId())) {
            throw new UnauthorizedException("Vous n'êtes pas le conducteur de ce trajet.");
        }
        return bookingRepository.findByTripIdOrderByCreatedAtDesc(trip.getId())
                .stream()
                .map(BookingResponse::forDriver)
                .toList();
    }

    // ────────────────── F20 — Accepter ou refuser une réservation ─────────────────

    @Transactional
    public BookingResponse accept(String driverEmail, String bookingUuid) {
        Booking booking = findPendingBookingOfDriver(driverEmail, bookingUuid);
        Trip trip = booking.getTrip();

        if (trip.getStatus() != TripStatus.PLANNED) {
            throw new ConflictException("Ce trajet n'est plus ouvert aux réservations.");
        }
        // Contrôle décisif : plusieurs demandes en attente peuvent totaliser plus
        // de places qu'il n'en reste réellement.
        if (booking.getSeatsBooked() > trip.getAvailableSeats()) {
            throw new NoSeatsAvailableException("Il ne reste que " + trip.getAvailableSeats()
                    + " place(s) : impossible d'accepter cette demande de "
                    + booking.getSeatsBooked() + " place(s).");
        }

        trip.setAvailableSeats(trip.getAvailableSeats() - booking.getSeatsBooked());
        if (trip.getAvailableSeats() == 0) {
            trip.setStatus(TripStatus.FULL);
        }
        tripRepository.save(trip);

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setStatusReason(null);

        log.info("Réservation {} acceptée : {} place(s) restante(s) sur le trajet {}",
                bookingUuid, trip.getAvailableSeats(), trip.getUuid());

        return BookingResponse.forDriver(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse reject(String driverEmail, String bookingUuid, String reason) {
        Booking booking = findPendingBookingOfDriver(driverEmail, bookingUuid);

        booking.setStatus(BookingStatus.REJECTED);
        booking.setStatusReason(blankToNull(reason));

        return BookingResponse.forDriver(bookingRepository.save(booking));
    }

    // ───────────────────── F29 — Annulation par le passager ──────────────────────

    @Transactional
    public BookingResponse cancel(String passengerEmail, String bookingUuid, String reason) {
        User passenger = findUser(passengerEmail);
        Booking booking = bookingRepository.findByUuid(bookingUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable."));

        if (!booking.getPassenger().getId().equals(passenger.getId())) {
            throw new UnauthorizedException("Cette réservation n'est pas la vôtre.");
        }
        if (!ACTIVE_STATUSES.contains(booking.getStatus())) {
            throw new ConflictException("Cette réservation ne peut plus être annulée.");
        }

        Trip trip = booking.getTrip();
        if (trip.getDepartureTime().isBefore(LocalDateTime.now())) {
            throw new ConflictException("Impossible d'annuler une réservation sur un trajet déjà parti.");
        }

        // Seule une réservation confirmée avait consommé des places : les rendre
        // pour une demande simplement en attente créerait des places fantômes.
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            releaseSeats(trip, booking.getSeatsBooked());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setStatusReason(blankToNull(reason));

        return BookingResponse.forPassenger(bookingRepository.save(booking));
    }

    // ────────────────────────────────── Interne ──────────────────────────────────

    /**
     * Rend les places d'une réservation au trajet et le rouvre s'il était complet.
     * Utilisé par l'annulation passager (F29) et par l'annulation du trajet (F18).
     */
    void releaseSeats(Trip trip, int seats) {
        trip.setAvailableSeats(trip.getAvailableSeats() + seats);
        if (trip.getStatus() == TripStatus.FULL) {
            trip.setStatus(TripStatus.PLANNED);
        }
        tripRepository.save(trip);
    }

    private Booking findPendingBookingOfDriver(String driverEmail, String bookingUuid) {
        User driver = findUser(driverEmail);
        Booking booking = bookingRepository.findByUuid(bookingUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable."));

        if (!booking.getTrip().getDriver().getId().equals(driver.getId())) {
            throw new UnauthorizedException("Cette réservation ne concerne pas l'un de vos trajets.");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ConflictException("Cette demande a déjà été traitée.");
        }
        return booking;
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
