package com.coshift.api.service;

import com.coshift.api.entity.*;
import com.coshift.api.exception.BadRequestException;
import com.coshift.api.exception.ResourceNotFoundException;
import com.coshift.api.repository.BookingRepository;
import com.coshift.api.repository.TripRepository;
import com.coshift.api.repository.ReviewRepository;
import com.coshift.api.repository.UserRepository;
import com.coshift.api.repository.VehiculeRepository;
import com.coshift.api.security.SecurityAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Droit d'accès, droit à la portabilité et droit à l'effacement.
 *
 * <h2>Ce que le règlement demande, et ce que le code faisait</h2>
 *
 * <p>Les articles 15, 17 et 20 du RGPD ouvrent trois droits que la politique de
 * confidentialité peut annoncer sans effort. Les rendre effectifs est autre
 * chose : avant ce service, {@code /api/users} n'exposait que la consultation
 * et la modification du profil. Aucun point d'entrée ne permettait ni de
 * récupérer ses données, ni de faire supprimer son compte — les deux droits
 * étaient donc reconnus sur le papier et inexistants dans les faits.</p>
 *
 * <h2>Pourquoi l'effacement n'efface pas la ligne</h2>
 *
 * <p>{@code trips.driver_id} et {@code bookings.passenger_id} sont obligatoires.
 * Supprimer la ligne d'un membre supprimerait donc, en cascade ou en erreur,
 * les trajets auxquels d'autres personnes ont participé. Or un covoiturage
 * passé engage deux personnes : effacer le conducteur priverait le passager de
 * son propre historique, ce qui reviendrait à faire droit à une demande en
 * portant atteinte aux données d'un tiers.</p>
 *
 * <p>L'effacement procède donc par <strong>anonymisation sur place</strong> :
 * les champs identifiants sont écrasés, immédiatement et sans copie de
 * sauvegarde. Ce qui subsiste — un trajet Namur-Bruxelles rattaché à un
 * participant sans nom, sans adresse et sans téléphone — ne se rapporte plus à
 * une personne identifiable et sort du champ du règlement au sens de son
 * considérant 26.</p>
 *
 * <h2>Ce que l'opération ne fait pas</h2>
 *
 * <p>Un jeton d'authentification déjà émis reste valable jusqu'à son expiration :
 * CoShift ne tient aucune liste de révocation. La faiblesse est connue et
 * documentée ; elle est ici sans conséquence pratique, puisque toute requête
 * ultérieure retrouve un compte dont {@code deletedAt} est renseigné et se voit
 * refusée par {@link User#isEnabled()}.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalDataService {

    private final UserRepository userRepository;
    private final Messages messages;
    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final VehiculeRepository vehiculeRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityAuditService audit;

    @Value("${app.upload.dir:uploads/avatars}")
    private String uploadDir;

    /**
     * Domaine réservé par la RFC 2606, garanti sans résolution DNS.
     *
     * <p>Une adresse forgée sur un domaine réel finirait par désigner la boîte
     * de quelqu'un ; celle-ci ne peut désigner personne.</p>
     */
    private static final String DOMAINE_EFFACE = "@compte-supprime.invalid";

    /* ─────────────────────────────────────────────────────────────────────
       Article 15 et article 20 — accès et portabilité
       ───────────────────────────────────────────────────────────────────── */

    /**
     * Rassemble tout ce que CoShift détient sur une personne.
     *
     * <p>Le format retenu est JSON : l'article 20 exige un format
     * « structuré, couramment utilisé et lisible par machine », trois qualités
     * qu'un PDF n'a pas et qu'un tableur n'a qu'en partie.</p>
     *
     * <p>L'export contient les données <em>relatives à la personne</em>, non
     * celles des autres. Un trajet réservé chez un tiers apparaît donc avec son
     * itinéraire et son horaire, mais sans le téléphone ni l'adresse du
     * conducteur : ce sont ses données à lui, et le droit à la portabilité ne
     * porte pas dessus.</p>
     */
    @Transactional(readOnly = true)
    public Map<String, Object> exporter(String email) {
        User u = trouver(email);

        Map<String, Object> racine = new LinkedHashMap<>();
        racine.put("_avertissement", """
                Export réalisé au titre des articles 15 et 20 du règlement (UE) 2016/679. \
                Il contient les données à caractère personnel que CoShift détient sur vous. \
                Les informations relatives à d'autres membres en sont volontairement exclues.""");
        racine.put("_genereLe", LocalDateTime.now().toString());
        racine.put("compte", compte(u));
        racine.put("organisations", organisations(u));
        racine.put("vehicules", vehicules(u));
        racine.put("trajetsProposes", trajetsProposes(u));
        racine.put("reservationsDemandees", reservationsDemandees(u));
        racine.put("avisEcrits", avisEcrits(u));
        racine.put("avisRecus", avisRecus(u));
        racine.put("_nonInclus", List.of(
                "L'empreinte du mot de passe : elle n'est pas réversible et ne vous apprendrait rien.",
                "Les codes de vérification en cours : ils expirent en une heure.",
                "Le journal de sécurité : il repose sur l'intérêt légitime, hors du champ de la portabilité (article 20.1).",
                "Les coordonnées des autres membres : ce sont leurs données, pas les vôtres."));

        log.info("Export de données personnelles produit pour le compte {}", u.getUuid());
        return racine;
    }

    private Map<String, Object> compte(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("identifiant", u.getUuid());
        m.put("email", u.getEmail());
        m.put("prenom", u.getFirstname());
        m.put("nom", u.getLastname());
        m.put("telephone", u.getPhoneNumber());
        m.put("photo", u.getPictureUrl());
        m.put("role", u.getRole() == null ? null : u.getRole().name());
        m.put("adresseVerifiee", u.isEmailVerified());
        m.put("noteMoyenne", u.getAverageRating());
        m.put("nombreDeTrajets", u.getTripsCount());
        m.put("inscritLe", texte(u.getCreatedAt()));
        m.put("modifieLe", texte(u.getUpdatedAt()));
        m.put("conditionsAccepteesLe", texte(u.getCguAcceptedAt()));
        m.put("versionDesConditions", u.getCguVersion());
        return m;
    }

    private List<Map<String, Object>> organisations(User u) {
        return u.getOrganizations().stream()
                .map(o -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("nom", o.getName());
                    m.put("identifiant", o.getUuid());
                    return m;
                })
                .toList();
    }

    private List<Map<String, Object>> vehicules(User u) {
        return vehiculeRepository.findByOwnerId(u.getId()).stream()
                .map(v -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("identifiant", v.getUuid());
                    m.put("marque", v.getBrand());
                    m.put("modele", v.getModel());
                    m.put("immatriculation", v.getLicensePlate());
                    m.put("places", v.getSeats());
                    m.put("motorisation", v.getEnergy() == null ? null : v.getEnergy().name());
                    m.put("photo", v.getPhotoUrl());
                    return m;
                })
                .toList();
    }

    private List<Map<String, Object>> trajetsProposes(User u) {
        return tripRepository.findByDriverIdOrderByDepartureTimeDesc(u.getId()).stream()
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("identifiant", t.getUuid());
                    m.put("depart", t.getDepartureCity());
                    m.put("adresseDepart", t.getDepartureAddress());
                    m.put("arrivee", t.getArrivalCity());
                    m.put("adresseArrivee", t.getArrivalAddress());
                    m.put("dateHeure", texte(t.getDepartureTime()));
                    m.put("places", t.getAvailableSeats());
                    m.put("participationParPlace", t.getPricePerSeat());
                    m.put("description", t.getDescription());
                    m.put("statut", t.getStatus().name());
                    m.put("publieLe", texte(t.getCreatedAt()));
                    /* Les demandes reçues sur ce trajet figurent sans l'identité
                       de leurs auteurs : le nombre de places et la décision me
                       concernent, le nom du demandeur ne m'appartient pas. */
                    m.put("demandesRecues", bookingRepository
                            .findByTripIdOrderByCreatedAtDesc(t.getId()).stream()
                            .map(this::demandeRecue)
                            .toList());
                    return m;
                })
                .toList();
    }

    /**
     * Une demande reçue sur mon trajet, dépouillée de son auteur.
     *
     * <p>{@code LinkedHashMap} et non {@code Map.of} : la fabrique immuable
     * refuse les valeurs nulles, et {@code bookings.created_at} est déclarée
     * nullable au schéma. Une seule réservation sans date de création faisait
     * donc échouer l'export entier — sur la fonctionnalité même qui matérialise
     * l'article 15. Les autres extracteurs de cette classe utilisaient déjà une
     * carte tolérante ; celui-ci était le seul à s'en écarter.</p>
     */
    private Map<String, Object> demandeRecue(Booking b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("places", b.getSeatsBooked());
        m.put("statut", b.getStatus() == null ? null : b.getStatus().name());
        m.put("demandeeLe", texte(b.getCreatedAt()));
        return m;
    }

    private List<Map<String, Object>> reservationsDemandees(User u) {
        return bookingRepository.findByPassengerIdOrderByCreatedAtDesc(u.getId()).stream()
                .map(b -> {
                    Trip t = b.getTrip();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("identifiant", b.getUuid());
                    m.put("depart", t == null ? null : t.getDepartureCity());
                    m.put("arrivee", t == null ? null : t.getArrivalCity());
                    m.put("dateHeure", t == null ? null : texte(t.getDepartureTime()));
                    m.put("places", b.getSeatsBooked());
                    m.put("montantTotal", b.getTotalPrice());
                    m.put("statut", b.getStatus().name());
                    m.put("motif", b.getStatusReason());
                    m.put("demandeeLe", texte(b.getCreatedAt()));
                    return m;
                })
                .toList();
    }

    /**
     * Avis rédigés par la personne.
     *
     * <p>Ce sont ses mots : ils lui appartiennent et entrent pleinement dans la
     * portabilité. Le nom de la personne notée n'y figure pas — c'est sa donnée
     * à elle, pas celle de l'auteur.</p>
     */
    private List<Map<String, Object>> avisEcrits(User u) {
        return reviewRepository.findByAuthorIdOrderByCreatedAtDesc(u.getId()).stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("identifiant", r.getUuid());
                    m.put("note", r.getRating());
                    m.put("commentaire", r.getComment());
                    m.put("redigeLe", texte(r.getCreatedAt()));
                    return m;
                })
                .toList();
    }

    /**
     * Avis reçus par la personne.
     *
     * <p>Ils la concernent, donc l'article 15 impose de les communiquer. Le
     * prénom de l'auteur est conservé : sans lui, un avis devient un jugement
     * anonyme sur lequel on ne peut pas revenir. Rien d'autre de l'auteur n'y
     * figure.</p>
     */
    private List<Map<String, Object>> avisRecus(User u) {
        return reviewRepository.findByTargetIdOrderByCreatedAtDesc(u.getId()).stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("identifiant", r.getUuid());
                    m.put("note", r.getRating());
                    m.put("commentaire", r.getComment());
                    m.put("auteur", r.getAuthor() == null ? null : r.getAuthor().getFirstname());
                    m.put("recuLe", texte(r.getCreatedAt()));
                    return m;
                })
                .toList();
    }

    /* ─────────────────────────────────────────────────────────────────────
       Article 17 — effacement
       ───────────────────────────────────────────────────────────────────── */

    /**
     * Efface un compte après confirmation par saisie de son adresse.
     *
     * <p>La confirmation n'est pas une précaution de confort. L'opération est
     * irréversible et détruit l'historique de son auteur : un bouton qui
     * l'exécuterait au premier clic transformerait une erreur de manipulation
     * en perte définitive.</p>
     *
     * @param email          compte à effacer, déduit du jeton
     * @param confirmation   adresse retapée par la personne
     * @param ip             adresse de l'appelant, pour le journal de sécurité
     */
    @Transactional
    public void effacer(String email, String confirmation, String ip) {
        User u = trouver(email);

        if (confirmation == null || !confirmation.trim().equalsIgnoreCase(u.getEmail())) {
            throw new BadRequestException(
                    messages.get("profil.confirmationIncorrecte"));
        }

        String identifiant = u.getUuid();
        LocalDateTime maintenant = LocalDateTime.now();

        annulerEngagementsFuturs(u, maintenant);
        anonymiserAvis(u);
        anonymiserVehicules(u);
        supprimerPhoto(u);
        anonymiserCompte(u, maintenant);

        userRepository.save(u);

        /* Le journal conserve l'identifiant technique, jamais l'adresse : garder
           l'adresse d'un compte effacé viderait l'effacement de son objet. */
        audit.consigner(SecurityAuditService.Evenement.COMPTE_EFFACE, identifiant, ip,
                "effacement demande par le titulaire");
        log.info("Compte {} anonymisé au titre de l'article 17 du RGPD", identifiant);
    }

    /**
     * Annule ce qui n'a pas encore eu lieu.
     *
     * <p>Un trajet futur dont le conducteur a disparu laisserait des passagers
     * attendre à un point de rendez-vous. La suppression d'un compte doit donc
     * les prévenir, pas seulement se taire.</p>
     */
    private void annulerEngagementsFuturs(User u, LocalDateTime maintenant) {
        List<Trip> trajets = tripRepository.findByDriverIdOrderByDepartureTimeDesc(u.getId());
        List<Trip> aAnnuler = trajets.stream()
                .filter(t -> t.getDepartureTime() != null && t.getDepartureTime().isAfter(maintenant))
                .filter(t -> t.getStatus() == TripStatus.PLANNED || t.getStatus() == TripStatus.FULL)
                .toList();

        for (Trip t : aAnnuler) {
            List<Booking> touchees = bookingRepository.findByTripIdAndStatusIn(
                    t.getId(), List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));
            touchees.forEach(b -> {
                b.setStatus(BookingStatus.CANCELLED);
                b.setStatusReason(messages.get("reservation.conducteurCompteSupprime"));
            });
            bookingRepository.saveAll(touchees);
            t.setStatus(TripStatus.CANCELLED);
        }
        tripRepository.saveAll(aAnnuler);

        List<Booking> mesDemandes = bookingRepository.findByPassengerIdOrderByCreatedAtDesc(u.getId())
                .stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING
                          || b.getStatus() == BookingStatus.CONFIRMED)
                .filter(b -> b.getTrip() != null
                          && b.getTrip().getDepartureTime() != null
                          && b.getTrip().getDepartureTime().isAfter(maintenant))
                .toList();

        mesDemandes.forEach(b -> {
            b.setStatus(BookingStatus.CANCELLED);
            b.setStatusReason(messages.get("reservation.passagerCompteSupprime"));
        });
        bookingRepository.saveAll(mesDemandes);

        if (!aAnnuler.isEmpty() || !mesDemandes.isEmpty()) {
            log.info("Effacement : {} trajet(s) et {} réservation(s) annulés",
                    aAnnuler.size(), mesDemandes.size());
        }
    }

    /**
     * Retire des avis tout texte libre rattaché à la personne.
     *
     * <p>Deux sens à traiter, et non un seul. Les commentaires <em>écrits</em>
     * par la personne sont ses mots : ils disparaissent avec elle. Les
     * commentaires <em>reçus</em> sont ceux d'autrui, mais ils parlent d'elle —
     * et un texte libre nomme volontiers celui qu'il décrit. Les conserver
     * laisserait « Marie était très ponctuelle » dans une base d'où Marie est
     * censée avoir disparu.</p>
     *
     * <p>La note chiffrée subsiste dans les deux cas. Détachée de tout nom, elle
     * ne se rapporte plus à une personne identifiable, et la retirer fausserait
     * la moyenne d'un tiers qui, lui, n'a rien demandé.</p>
     */
    private void anonymiserAvis(User u) {
        List<Review> ecrits = reviewRepository.findByAuthorIdOrderByCreatedAtDesc(u.getId());
        List<Review> recus = reviewRepository.findByTargetIdOrderByCreatedAtDesc(u.getId());

        ecrits.forEach(r -> r.setComment(null));
        recus.forEach(r -> r.setComment(null));

        reviewRepository.saveAll(ecrits);
        reviewRepository.saveAll(recus);

        if (!ecrits.isEmpty() || !recus.isEmpty()) {
            log.info("Effacement : {} avis écrit(s) et {} avis reçu(s) vidés de leur commentaire",
                    ecrits.size(), recus.size());
        }
    }

    /**
     * Vide les véhicules de ce qui identifie leur propriétaire.
     *
     * <p>La plaque est une donnée personnelle : elle mène au titulaire par le
     * répertoire de la Direction pour l'immatriculation des véhicules. La marque
     * et le modèle, une fois détachés de toute personne, n'en sont pas.</p>
     *
     * <p>La ligne n'est pas supprimée pour la même raison que celle du
     * compte : {@code trips.vehicule_id} est obligatoire.</p>
     */
    private void anonymiserVehicules(User u) {
        List<Vehicule> vehicules = vehiculeRepository.findByOwnerId(u.getId());
        for (Vehicule v : vehicules) {
            v.setLicensePlate(null);
            v.setPhotoUrl(null);
        }
        vehiculeRepository.saveAll(vehicules);
    }

    /** Retire du disque la photo de profil : une donnée effacée en base mais servie par une URL ne l'est pas. */
    private void supprimerPhoto(User u) {
        String url = u.getPictureUrl();
        if (url == null || url.isBlank()) return;
        try {
            String nom = url.substring(url.lastIndexOf('/') + 1);
            /* Le nom vient de la base, mais il a été forgé par le serveur à
               partir d'un UUID. Le contrôle interdit malgré tout de sortir du
               dossier : une donnée en base reste une donnée d'entrée. */
            if (nom.contains("..") || nom.contains("/") || nom.contains("\\")) return;
            Path dossier = Paths.get(uploadDir);
            Files.deleteIfExists(dossier.resolve(nom));
        } catch (IOException | RuntimeException e) {
            /* Une photo qui résiste ne doit pas faire échouer l'effacement du
               reste. L'incident est tracé pour être repris à la main. */
            log.warn("Photo de profil non supprimée lors de l'effacement du compte {} : {}",
                    u.getUuid(), e.getMessage());
        }
    }

    /** Écrase les champs identifiants. Sans copie, sans journal de la valeur précédente. */
    private void anonymiserCompte(User u, LocalDateTime maintenant) {
        /* L'adresse doit rester unique : la contrainte porte sur la colonne, et
           deux comptes effacés se heurteraient sur une valeur constante. */
        u.setEmail(u.getUuid().substring(0, 8) + DOMAINE_EFFACE);
        u.setFirstname("Compte");
        u.setLastname("supprimé");
        u.setPhoneNumber(null);
        u.setPictureUrl(null);
        u.setVerificationCode(null);
        u.setVerificationCodeExpiry(null);
        u.setPasswordResetCode(null);
        u.setPasswordResetExpiry(null);
        u.setEmailVerified(false);
        u.setDeletedAt(maintenant);

        /* Une empreinte aléatoire plutôt qu'une chaîne vide : un mot de passe
           vide se compare, une empreinte sans antécédent ne correspond à
           aucune saisie possible. */
        u.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

        /* Le rattachement à l'organisation disparaît : sans lui, le compte ne
           compte plus dans ses effectifs et n'apparaît plus dans son cercle. */
        u.getOrganizations().clear();
    }

    /* ───────────────────────────────────────────────────────────────────── */

    private User trouver(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("auth.utilisateurIntrouvable")));
    }

    private String texte(LocalDateTime d) {
        return d == null ? null : d.toString();
    }
}
