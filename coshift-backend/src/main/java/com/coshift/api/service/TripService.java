package com.coshift.api.service;

import com.coshift.api.dto.TripRequest;
import com.coshift.api.dto.TripResponse;
import com.coshift.api.entity.*;
import com.coshift.api.exception.BadRequestException;
import com.coshift.api.exception.ConflictException;
import com.coshift.api.exception.ResourceNotFoundException;
import com.coshift.api.exception.UnauthorizedException;
import com.coshift.api.repository.BookingRepository;
import com.coshift.api.repository.OrganizationRepository;
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
    private final OrganizationRepository organizationRepository;
    private final OrganizationService organizationService;

    /** Délai minimum entre la publication et le départ, imposé par F16. */
    private static final int MIN_HOURS_BEFORE_DEPARTURE = 2;

    /** Nombre maximum de trajets simultanément actifs par conducteur (F16). */
    private static final int MAX_ACTIVE_TRIPS = 5;

    /** Identifiant qu'aucune organisation ne porte : les clés sont auto-incrémentées. */
    private static final long AUCUNE_ORGANISATION = -1L;

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
                .organization(organisationDuTrajet(driver, request.getOrganizationUuid()))
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
                searcher.getId(),
                cercleDe(searcher))
                .stream()
                .map(TripResponse::from)
                .toList();
    }

    /**
     * Organisations dont l'appelant voit les trajets.
     *
     * <p>Jamais vide : une liste vide dans un {@code IN} n'est pas du SQL
     * valide, et la faire disparaître de la requête reviendrait à lever le
     * cercle pour ceux qui n'appartiennent à rien — exactement l'inverse de ce
     * qu'on veut. La valeur impossible laisse la clause bien formée et ne
     * correspond à aucune organisation, si bien que seuls les trajets sans
     * organisation remontent.</p>
     */
    private List<Long> cercleDe(User user) {
        List<Long> siennes = organizationService.identifiantsDesOrganisations(user);
        return siennes.isEmpty() ? List.of(AUCUNE_ORGANISATION) : siennes;
    }

    // F19 — Mes trajets proposés (conducteur)
    public List<TripResponse> getMyTrips(String driverEmail) {
        User driver = findUser(driverEmail);
        return tripRepository.findByDriverIdOrderByDepartureTimeDesc(driver.getId())
                .stream()
                .map(TripResponse::from)
                .toList();
    }

    /**
     * F26 — Détail d'un trajet.
     *
     * <p>Le cercle s'applique ici aussi. Ne filtrer que la recherche
     * n'apporterait rien : il suffirait d'un lien reçu d'ailleurs pour lire la
     * fiche complète d'un trajet d'une autre organisation, adresse de départ
     * comprise.</p>
     *
     * <p>Hors du cercle, la réponse est « introuvable » et non « interdit ».
     * Distinguer les deux confirmerait à l'appelant qu'un trajet existe bien
     * derrière cet identifiant, et lui apprendrait par la même occasion à
     * quelle organisation appartient son conducteur. Ce qui n'est pas de son
     * cercle n'existe pas pour lui.</p>
     */
    public TripResponse getTripByUuid(String viewerEmail, String uuid) {
        User viewer = findUser(viewerEmail);
        Trip trip = tripRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("trajet.introuvable")));

        /* Le conducteur voit toujours son propre trajet, y compris s'il a
           quitté depuis l'organisation à laquelle il l'avait ouvert. */
        boolean sien = trip.getDriver().getId().equals(viewer.getId());
        if (!sien && !organizationService.partageLeCercle(viewer, trip.getOrganization())) {
            throw new ResourceNotFoundException(messages.get("trajet.introuvable"));
        }
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

    /**
     * Organisation à laquelle ouvrir le trajet.
     *
     * <p>Le cercle du trajet est fixé à la publication et n'est pas déduit plus
     * tard de l'appartenance du conducteur : celle-ci peut changer, alors qu'un
     * trajet déjà publié a été proposé à un public donné. Le figer, c'est éviter
     * qu'un trajet passe silencieusement d'un cercle à un autre.</p>
     *
     * <p>Sans choix explicite, on retient l'organisation d'origine du
     * conducteur. Un conducteur qui n'appartient à rien publie un trajet sans
     * organisation : il sera visible de tous, faute de cercle à qui le
     * réserver.</p>
     *
     * <p>Un choix explicite doit désigner une organisation dont le conducteur
     * est membre. Sans cette vérification, n'importe qui publierait un trajet
     * dans le cercle de n'importe quelle entreprise à partir de son seul
     * identifiant public — la restriction de visibilité se contournerait par
     * l'écriture au lieu de la lecture.</p>
     */
    private Organization organisationDuTrajet(User driver, String organizationUuid) {
        if (organizationUuid == null || organizationUuid.isBlank()) {
            return organizationService.organisationParDefaut(driver).orElse(null);
        }

        Organization visee = organizationRepository.findByUuid(organizationUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messages.get("trajet.organisationIntrouvable")));

        boolean membre = organizationService.identifiantsDesOrganisations(driver)
                .contains(visee.getId());
        if (!membre) {
            throw new UnauthorizedException(messages.get("trajet.organisationAutrui"));
        }
        return visee;
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
