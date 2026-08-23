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
import com.coshift.api.repository.ReviewRepository;
import com.coshift.api.repository.TripRepository;
import com.coshift.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private final Messages messages;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final EmailService emailService;
    private final OrganizationService organizationService;
    private final PaymentService paymentService;

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
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("trajet.introuvable")));

        /* Dernier verrou du cercle fermé, et le seul qui compte vraiment : la
           recherche et la fiche ne font que ne pas montrer, celui-ci refuse
           d'écrire. Sans lui, il suffirait de connaître un identifiant de
           trajet pour monter dans la voiture d'une autre organisation.
           « Introuvable » plutôt qu'« interdit », pour la même raison qu'à la
           lecture : ne pas confirmer l'existence de ce qui n'est pas de son
           cercle. */
        if (!organizationService.partageLeCercle(passenger, trip.getOrganization())) {
            throw new ResourceNotFoundException(messages.get("trajet.introuvable"));
        }
        if (trip.getDriver().getId().equals(passenger.getId())) {
            throw new BadRequestException(messages.get("reservation.proprePropreTrajet"));
        }
        if (trip.getStatus() != TripStatus.PLANNED) {
            throw new ConflictException(messages.get("reservation.trajetFerme"));
        }
        if (trip.getDepartureTime().isBefore(LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_BOOKING))) {
            throw new ConflictException(messages.get("reservation.delai", MIN_HOURS_BEFORE_BOOKING * 60));
        }
        if (bookingRepository.existsByTripIdAndPassengerIdAndStatusIn(
                trip.getId(), passenger.getId(), ACTIVE_STATUSES)) {
            throw new ConflictException(messages.get("reservation.dejaEnCours"));
        }
        if (request.getSeatsBooked() > trip.getAvailableSeats()) {
            throw new NoSeatsAvailableException(messages.get("reservation.placesRestantes", trip.getAvailableSeats()));
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

        /* Sans cet envoi, le conducteur devait ouvrir son tableau de bord et
           regarder pour découvrir qu'on l'avait sollicité. */
        emailService.notifierDemandeRecue(trip.getDriver(), passenger.getFirstname(),
                resume(trip), booking.getSeatsBooked());
        Booking enregistree = bookingRepository.save(booking);

        /* Le montant devient du des la demande, mais rien n'est preleve : le
           conducteur peut encore refuser, et faire payer une place qu'on
           n'aura peut-etre pas obligerait a rembourser des gens qui n'ont
           jamais voyage. */
        paymentService.ouvrir(enregistree);

        return BookingResponse.forPassenger(enregistree);
    }

    // ────────────────────────── F30 — Mes réservations ───────────────────────────

    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(String passengerEmail) {
        User passenger = findUser(passengerEmail);
        Set<Long> dejaNotees = reservationsDejaNoteesPar(passenger);

        return bookingRepository.findByPassengerIdOrderByCreatedAtDesc(passenger.getId())
                .stream()
                .map(b -> marquerSiNotee(BookingResponse.forPassenger(b), b, dejaNotees))
                .toList();
    }

    /**
     * Réservations que ce membre a déjà notées.
     *
     * <p>Une seule requête pour toute la liste. La solution évidente — demander
     * pour chaque ligne si un avis existe — coûterait autant d'allers-retours
     * que de réservations affichées, pour une information que la base rend
     * d'un coup.</p>
     */
    private Set<Long> reservationsDejaNoteesPar(User membre) {
        return new HashSet<>(reviewRepository.reservationsDejaNoteesPar(membre.getId()));
    }

    private BookingResponse marquerSiNotee(BookingResponse reponse, Booking booking,
                                           Set<Long> dejaNotees) {
        reponse.setReviewed(dejaNotees.contains(booking.getId()));
        return reponse;
    }

    // ─────────────────── F19 — Les demandes reçues par le conducteur ──────────────

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsReceived(String driverEmail) {
        User driver = findUser(driverEmail);
        Set<Long> dejaNotees = reservationsDejaNoteesPar(driver);

        return bookingRepository.findAllForDriver(driver.getId())
                .stream()
                .map(b -> marquerSiNotee(BookingResponse.forDriver(b), b, dejaNotees))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsForTrip(String driverEmail, String tripUuid) {
        User driver = findUser(driverEmail);
        Trip trip = tripRepository.findByUuid(tripUuid)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("trajet.introuvable")));

        if (!trip.getDriver().getId().equals(driver.getId())) {
            throw new UnauthorizedException(messages.get("trajet.pasConducteur"));
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
            throw new ConflictException(messages.get("reservation.trajetPlusOuvert"));
        }
        // Contrôle décisif : plusieurs demandes en attente peuvent totaliser plus
        // de places qu'il n'en reste réellement.
        if (booking.getSeatsBooked() > trip.getAvailableSeats()) {
            throw new NoSeatsAvailableException(messages.get("reservation.placesInsuffisantes",
                    trip.getAvailableSeats(), booking.getSeatsBooked()));
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

        emailService.notifierReservationAcceptee(booking.getPassenger(),
                trip.getDriver().getFirstname(), resume(trip));

        return BookingResponse.forDriver(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse reject(String driverEmail, String bookingUuid, String reason) {
        Booking booking = findPendingBookingOfDriver(driverEmail, bookingUuid);

        booking.setStatus(BookingStatus.REJECTED);
        booking.setStatusReason(blankToNull(reason));

        /* Refus du conducteur : le passager n'a rien decide, il recupere tout.
           Si rien n'avait ete regle, le du est simplement clos. */
        paymentService.rembourser(booking, true, messages.get("paiement.motifRefus"));

        emailService.notifierReservationRefusee(booking.getPassenger(),
                resume(booking.getTrip()), booking.getStatusReason());

        return BookingResponse.forDriver(bookingRepository.save(booking));
    }

    // ───────────────────── F29 — Annulation par le passager ──────────────────────

    @Transactional
    public BookingResponse cancel(String passengerEmail, String bookingUuid, String reason) {
        User passenger = findUser(passengerEmail);
        Booking booking = bookingRepository.findByUuid(bookingUuid)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("reservation.introuvable")));

        if (!booking.getPassenger().getId().equals(passenger.getId())) {
            throw new UnauthorizedException(messages.get("reservation.pasLaVotre"));
        }
        if (!ACTIVE_STATUSES.contains(booking.getStatus())) {
            throw new ConflictException(messages.get("reservation.plusAnnulable"));
        }

        Trip trip = booking.getTrip();
        if (trip.getDepartureTime().isBefore(LocalDateTime.now())) {
            throw new ConflictException(messages.get("reservation.trajetParti"));
        }

        // Seule une réservation confirmée avait consommé des places : les rendre
        // pour une demande simplement en attente créerait des places fantômes.
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            releaseSeats(trip, booking.getSeatsBooked());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setStatusReason(blankToNull(reason));

        /* Annulation du passager : le bareme s'applique. Au-dela de vingt-quatre
           heures il recupere tout, en deca la moitie — le siege ne se reloue
           plus, et rendre tout ferait de l'annulation de derniere minute une
           option gratuite. */
        paymentService.rembourser(booking, false, messages.get("paiement.motifAnnulationPassager"));

        emailService.notifierAnnulationParPassager(trip.getDriver(),
                passenger.getFirstname(), resume(trip));

        return BookingResponse.forPassenger(bookingRepository.save(booking));
    }

    // ──────────────────── F21 — Confirmation de prestation ───────────────────────

    /**
     * Le passager reconnaît que le trajet a bien eu lieu.
     *
     * <h2>Pourquoi le passager, et pas le conducteur</h2>
     *
     * <p>Le conducteur a un intérêt à déclarer la course effectuée — elle
     * alimente son compteur de trajets et, demain, sa rémunération. Le passager
     * n'en a pas : il confirme ce qu'il a constaté. Confier la confirmation à
     * la partie qui n'y gagne rien est la seule façon d'en faire une
     * information fiable.</p>
     *
     * <h2>Ce que la confirmation débloque</h2>
     *
     * <p>Le statut {@link BookingStatus#COMPLETED} était déclaré depuis le
     * premier jour, interrogé par les requêtes de données ouvertes, et attribué
     * par personne : les statistiques publiques comptaient donc une catégorie
     * toujours vide. Le compteur {@code tripsCount} des deux participants
     * restait lui aussi à zéro, ce qui affichait « 0 trajet » à un conducteur
     * chevronné.</p>
     *
     * <p>Les deux participants sont incrémentés : un covoiturage effectué est
     * un trajet partagé, il compte pour celui qui conduit comme pour celui qui
     * monte. Compter le seul conducteur ferait du passager un éternel débutant.</p>
     */
    @Transactional
    public BookingResponse complete(String passengerEmail, String bookingUuid) {
        User passenger = findUser(passengerEmail);
        Booking booking = bookingRepository.findByUuid(bookingUuid)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("reservation.introuvable")));

        if (!booking.getPassenger().getId().equals(passenger.getId())) {
            throw new UnauthorizedException(messages.get("reservation.pasLaVotre"));
        }

        // Seule une réservation acceptée a pu donner lieu à une course. Une
        // demande restée en attente, refusée ou annulée n'a transporté personne.
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ConflictException(booking.getStatus() == BookingStatus.COMPLETED
                    ? messages.get("reservation.dejaConfirmee")
                    : messages.get("reservation.pasConfirmable"));
        }

        Trip trip = booking.getTrip();
        // On ne confirme pas une course qui n'a pas encore eu lieu : sans ce
        // contrôle, un passager gonflerait son compteur dès la réservation.
        if (trip.getDepartureTime() == null || trip.getDepartureTime().isAfter(LocalDateTime.now())) {
            throw new ConflictException(messages.get("reservation.trajetPasEncorePasse"));
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(LocalDateTime.now());
        booking.setStatusReason(null);

        crediterUnTrajet(passenger);
        crediterUnTrajet(trip.getDriver());

        log.info("Prestation confirmée pour la réservation {} : le trajet {} est compté pour ses deux participants",
                bookingUuid, trip.getUuid());

        return BookingResponse.forPassenger(bookingRepository.save(booking));
    }

    /** Ajoute un trajet au compteur d'un participant et le persiste. */
    private void crediterUnTrajet(User participant) {
        participant.setTripsCount(participant.getTripsCount() + 1);
        userRepository.save(participant);
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
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("reservation.introuvable")));

        if (!booking.getTrip().getDriver().getId().equals(driver.getId())) {
            throw new UnauthorizedException(messages.get("reservation.pasVotreTrajet"));
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ConflictException(messages.get("reservation.dejaTraitee"));
        }
        return booking;
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("auth.utilisateurIntrouvable")));
    }

    /**
     * Rappel du trajet, tel qu'il apparaît dans un courriel.
     *
     * <p>Villes, date et heure : de quoi reconnaître de quel trajet on parle
     * sans avoir à ouvrir l'application. L'adresse précise en est absente —
     * elle n'a rien à faire dans un courriel qui traverse des serveurs tiers.</p>
     */
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

    /**
     * Une réservation, vue par son passager.
     *
     * <p>Sert à renvoyer l'état à jour après un règlement, sans obliger le
     * client à recharger toute sa liste pour une ligne qui a changé.</p>
     */
    public BookingResponse parUuidPourPassager(String passengerEmail, String bookingUuid) {
        User passenger = findUser(passengerEmail);
        Booking booking = bookingRepository.findByUuid(bookingUuid)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("reservation.introuvable")));
        if (!booking.getPassenger().getId().equals(passenger.getId())) {
            throw new UnauthorizedException(messages.get("reservation.pasLaVotre"));
        }
        return BookingResponse.forPassenger(booking);
    }

}
