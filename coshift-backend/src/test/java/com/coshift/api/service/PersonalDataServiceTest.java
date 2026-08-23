package com.coshift.api.service;

import com.coshift.api.entity.Booking;
import com.coshift.api.entity.BookingStatus;
import com.coshift.api.entity.EnergyType;
import com.coshift.api.entity.Organization;
import com.coshift.api.entity.Review;
import com.coshift.api.entity.Role;
import com.coshift.api.entity.Trip;
import com.coshift.api.entity.TripStatus;
import com.coshift.api.entity.User;
import com.coshift.api.entity.Vehicule;
import com.coshift.api.exception.BadRequestException;
import com.coshift.api.exception.ResourceNotFoundException;
import com.coshift.api.repository.BookingRepository;
import com.coshift.api.repository.ReviewRepository;
import com.coshift.api.repository.TripRepository;
import com.coshift.api.repository.UserRepository;
import com.coshift.api.repository.VehiculeRepository;
import com.coshift.api.security.SecurityAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Droits d'accès, de portabilité et d'effacement — RGPD, articles 15, 17 et 20.
 *
 * <p>Ces règles sont exactement celles qui cassent en silence. Un export qui
 * laisse fuir le téléphone d'un tiers reste un export valide en apparence ; un
 * effacement qui oublie d'annuler les trajets futurs laisse des passagers à un
 * point de rendez-vous sans que rien ne le signale ; une anonymisation qui
 * garde l'adresse électronique passe tous les contrôles techniques et vide la
 * démarche de son objet.</p>
 *
 * <p>Chaque test ci-dessous verrouille une promesse écrite dans la politique de
 * confidentialité. C'est la différence entre annoncer un droit et l'appliquer.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PersonalDataService — droits des personnes")
class PersonalDataServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TripRepository tripRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private VehiculeRepository vehiculeRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SecurityAuditService audit;
    @Mock private Messages messages;

    @InjectMocks private PersonalDataService service;

    private User moi;
    private User autrui;
    private Vehicule voiture;

    private static final String MOI = "moi@coshift.be";
    private static final String IP = "203.0.113.7";

    @BeforeEach
    void preparer() {
        moi = User.builder()
                .id(1L).uuid("11111111-2222-3333-4444-555555555555")
                .email(MOI)
                .firstname("Camille").lastname("Dupont")
                .phoneNumber("+32470000000")
                .pictureUrl(null)
                .password("empreinte-initiale")
                .role(Role.USER)
                .emailVerified(true)
                .averageRating(4.5).tripsCount(7)
                .cguAcceptedAt(LocalDateTime.now().minusMonths(3))
                .cguVersion("1.0")
                .organizations(new HashSet<>(List.of(
                        Organization.builder().id(9L).uuid("org-uuid").name("Solvantis").build())))
                .build();

        autrui = User.builder()
                .id(2L).uuid("uuid-autrui")
                .email("autrui@coshift.be")
                .firstname("Sacha").lastname("Bernard")
                .phoneNumber("+32471111111")
                .password("peu-importe")
                .emailVerified(true)
                .build();

        voiture = Vehicule.builder()
                .id(20L).uuid("vehicule-uuid")
                .brand("Renault").model("Clio")
                .licensePlate("1-ABC-123")
                .photoUrl("http://localhost:8080/uploads/avatars/photo.jpg")
                .seats(4).energy(EnergyType.GASOLINE)
                .owner(moi)
                .build();

        when(userRepository.findByEmail(MOI)).thenReturn(Optional.of(moi));
        when(passwordEncoder.encode(anyString())).thenReturn("empreinte-aleatoire");
        when(messages.get(anyString())).thenReturn("message");
        when(tripRepository.findByDriverIdOrderByDepartureTimeDesc(anyLong())).thenReturn(List.of());
        when(bookingRepository.findByPassengerIdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of());
        when(bookingRepository.findByTripIdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of());
        when(vehiculeRepository.findByOwnerId(anyLong())).thenReturn(List.of());
        when(reviewRepository.findByAuthorIdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of());
        when(reviewRepository.findByTargetIdOrderByCreatedAtDesc(anyLong())).thenReturn(List.of());
    }

    // ══════════════ Articles 15 et 20 — accès et portabilité ════════════════════

    @Nested
    @DisplayName("Export des données personnelles")
    class Export {

        @Test
        @DisplayName("rassemble toutes les catégories de données détenues")
        void rassembleToutesLesCategories() {
            Map<String, Object> export = service.exporter(MOI);

            assertThat(export).containsKeys(
                    "compte", "organisations", "vehicules",
                    "trajetsProposes", "reservationsDemandees",
                    "avisEcrits", "avisRecus");
        }

        @Test
        @DisplayName("porte la mention de son fondement juridique et sa date")
        void porteSonFondement() {
            Map<String, Object> export = service.exporter(MOI);

            assertThat((String) export.get("_avertissement")).contains("2016/679");
            assertThat(export.get("_genereLe")).isNotNull();
        }

        @Test
        @DisplayName("énumère ce qui est volontairement absent")
        void enumereLesAbsences() {
            // L'article 15.1 impose d'informer sur les données traitées. Dire ce
            // qu'on ne communique pas, et pourquoi, fait partie de la réponse.
            @SuppressWarnings("unchecked")
            List<String> absences = (List<String>) service.exporter(MOI).get("_nonInclus");

            assertThat(absences).isNotEmpty();
            assertThat(String.join(" ", absences)).contains("mot de passe");
        }

        @Test
        @DisplayName("livre les données du compte")
        void livreLesDonneesDuCompte() {
            @SuppressWarnings("unchecked")
            Map<String, Object> compte = (Map<String, Object>) service.exporter(MOI).get("compte");

            assertThat(compte.get("email")).isEqualTo(MOI);
            assertThat(compte.get("prenom")).isEqualTo("Camille");
            assertThat(compte.get("telephone")).isEqualTo("+32470000000");
            assertThat(compte.get("versionDesConditions")).isEqualTo("1.0");
            assertThat(compte.get("conditionsAccepteesLe")).isNotNull();
        }

        @Test
        @DisplayName("n'expose jamais l'empreinte du mot de passe")
        void nExposePasLeMotDePasse() {
            // Elle n'est pas réversible et n'apprendrait rien ; la publier
            // n'ajouterait qu'une surface d'attaque.
            assertThat(service.exporter(MOI).toString())
                    .doesNotContain("empreinte-initiale");
        }

        @Test
        @DisplayName("livre un trajet proposé sans l'identité de ceux qui l'ont demandé")
        void masqueLidentiteDesDemandeurs() {
            // Le droit à la portabilité porte sur ses propres données. Le nombre
            // de places et la décision me concernent ; le nom du demandeur ne
            // m'appartient pas.
            Trip trajet = trajet(LocalDateTime.now().minusDays(3), TripStatus.COMPLETED);
            when(tripRepository.findByDriverIdOrderByDepartureTimeDesc(1L)).thenReturn(List.of(trajet));
            when(bookingRepository.findByTripIdOrderByCreatedAtDesc(10L))
                    .thenReturn(List.of(reservation(trajet, BookingStatus.COMPLETED)));

            String export = service.exporter(MOI).toString();

            assertThat(export).contains("Namur");
            assertThat(export).doesNotContain("Sacha");
            assertThat(export).doesNotContain("autrui@coshift.be");
            assertThat(export).doesNotContain("+32471111111");
        }

        @Test
        @DisplayName("livre une réservation sans les coordonnées du conducteur")
        void masqueLesCoordonneesDuConducteur() {
            Trip trajetDautrui = Trip.builder()
                    .id(11L).uuid("trajet-autrui")
                    .departureCity("Liège").arrivalCity("Anvers")
                    .departureTime(LocalDateTime.now().minusDays(1))
                    .availableSeats(2).pricePerSeat(new BigDecimal("7.00"))
                    .status(TripStatus.COMPLETED)
                    .driver(autrui).vehicule(voiture)
                    .build();
            Booking maDemande = Booking.builder()
                    .id(101L).uuid("ma-resa")
                    .trip(trajetDautrui).passenger(moi)
                    .seatsBooked(1).totalPrice(new BigDecimal("7.00"))
                    .status(BookingStatus.COMPLETED)
                    .build();
            when(bookingRepository.findByPassengerIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(maDemande));

            String export = service.exporter(MOI).toString();

            assertThat(export).contains("Liège");
            assertThat(export).doesNotContain("+32471111111");
            assertThat(export).doesNotContain("autrui@coshift.be");
        }

        @Test
        @DisplayName("livre les avis écrits sans nommer la personne notée")
        void avisEcritsSansLaCible() {
            when(reviewRepository.findByAuthorIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(avis(moi, autrui, 5, "Conduite souple")));

            String export = service.exporter(MOI).toString();

            assertThat(export).contains("Conduite souple");
            assertThat(export).doesNotContain("Sacha");
        }

        @Test
        @DisplayName("livre les avis reçus avec le prénom de leur auteur")
        void avisRecusAvecLePrenomDeLauteur() {
            // Sans le prénom, un avis devient un jugement anonyme sur lequel la
            // personne ne peut pas revenir. Rien d'autre de l'auteur n'y figure.
            when(reviewRepository.findByTargetIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(avis(autrui, moi, 4, "Ponctuel")));

            String export = service.exporter(MOI).toString();

            assertThat(export).contains("Ponctuel");
            assertThat(export).contains("Sacha");
            assertThat(export).doesNotContain("Bernard");
            assertThat(export).doesNotContain("autrui@coshift.be");
        }

        @Test
        @DisplayName("survit à une réservation sans date de création")
        void surviteAUneReservationSansDate() {
            /* Régression. `bookings.created_at` est déclarée nullable au schéma,
               et l'extracteur des demandes reçues utilisait `Map.of`, qui refuse
               les valeurs nulles. Une seule réservation sans date faisait donc
               échouer l'export entier en 500 — sur la fonctionnalité même qui
               matérialise l'article 15. Découvert par ce test. */
            Trip trajet = trajet(LocalDateTime.now().minusDays(3), TripStatus.COMPLETED);
            Booking sansDate = reservation(trajet, BookingStatus.COMPLETED);
            assertThat(sansDate.getCreatedAt()).isNull();

            when(tripRepository.findByDriverIdOrderByDepartureTimeDesc(1L)).thenReturn(List.of(trajet));
            when(bookingRepository.findByTripIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(sansDate));

            Map<String, Object> export = service.exporter(MOI);

            assertThat(export.get("trajetsProposes")).isNotNull();
            assertThat(export.toString()).contains("demandesRecues");
        }

        @Test
        @DisplayName("refuse un compte inconnu")
        void refuseUnCompteInconnu() {
            when(userRepository.findByEmail("fantome@coshift.be")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.exporter("fantome@coshift.be"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ═══════════════════ Article 17 — droit à l'effacement ══════════════════════

    @Nested
    @DisplayName("Confirmation avant effacement")
    class Confirmation {

        @ParameterizedTest(name = "refuse la confirmation « {0} »")
        @ValueSource(strings = {"", "   ", "autre@coshift.be", "moi@coshift", "moi"})
        @DisplayName("refuse toute confirmation qui ne reprend pas l'adresse")
        void refuseUneConfirmationIncorrecte(String saisie) {
            assertThatThrownBy(() -> service.effacer(MOI, saisie, IP))
                    .isInstanceOf(BadRequestException.class);

            assertThat(moi.getDeletedAt()).isNull();
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("refuse une confirmation absente")
        void refuseUneConfirmationAbsente() {
            assertThatThrownBy(() -> service.effacer(MOI, null, IP))
                    .isInstanceOf(BadRequestException.class);

            assertThat(moi.getDeletedAt()).isNull();
        }

        @Test
        @DisplayName("tolère la casse et les espaces autour de l'adresse")
        void tolereLaCasseEtLesEspaces() {
            // L'opération est irréversible, mais la barrière ne doit pas non plus
            // se transformer en piège pour qui a bien retapé son adresse.
            service.effacer(MOI, "  MOI@CoShift.BE  ", IP);

            assertThat(moi.getDeletedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Anonymisation du compte")
    class Anonymisation {

        @BeforeEach
        void effacer() {
            service.effacer(MOI, MOI, IP);
        }

        @Test
        @DisplayName("écrase les champs identifiants")
        void ecraseLesChampsIdentifiants() {
            assertThat(moi.getEmail()).doesNotContain("moi@coshift.be");
            assertThat(moi.getFirstname()).isEqualTo("Compte");
            assertThat(moi.getLastname()).isEqualTo("supprimé");
            assertThat(moi.getPhoneNumber()).isNull();
            assertThat(moi.getPictureUrl()).isNull();
        }

        @Test
        @DisplayName("forge une adresse unique sur un domaine qui ne peut désigner personne")
        void forgeUneAdresseInerte() {
            // RFC 2606 : le domaine .invalid ne résout jamais. Une adresse forgée
            // sur un domaine réel finirait par désigner la boîte de quelqu'un.
            // Et elle doit rester unique : la contrainte porte sur la colonne, et
            // deux comptes effacés se heurteraient sur une valeur constante.
            assertThat(moi.getEmail()).endsWith("@compte-supprime.invalid");
            assertThat(moi.getEmail()).startsWith(moi.getUuid().substring(0, 8));
        }

        @Test
        @DisplayName("remplace l'empreinte du mot de passe par une valeur sans antécédent")
        void remplaceLempreinte() {
            // Une chaîne vide se compare ; une empreinte tirée au hasard ne
            // correspond à aucune saisie possible.
            assertThat(moi.getPassword()).isEqualTo("empreinte-aleatoire");
            assertThat(moi.getPassword()).isNotEqualTo("empreinte-initiale");
        }

        @Test
        @DisplayName("efface les codes de vérification et de réinitialisation en cours")
        void effaceLesCodes() {
            assertThat(moi.getVerificationCode()).isNull();
            assertThat(moi.getVerificationCodeExpiry()).isNull();
            assertThat(moi.getPasswordResetCode()).isNull();
            assertThat(moi.getPasswordResetExpiry()).isNull();
        }

        @Test
        @DisplayName("rend le compte incapable de se connecter")
        void rendLeCompteInactif() {
            assertThat(moi.getDeletedAt()).isNotNull();
            assertThat(moi.isEmailVerified()).isFalse();
            assertThat(moi.isEnabled()).isFalse();
            assertThat(moi.isAccountNonExpired()).isFalse();
        }

        @Test
        @DisplayName("détache le compte de son organisation")
        void detacheDeLorganisation() {
            // Sans cela, le compte continuerait de compter dans les effectifs de
            // l'entreprise et d'apparaître dans son cercle.
            assertThat(moi.getOrganizations()).isEmpty();
        }

        @Test
        @DisplayName("consigne l'effacement sous l'identifiant technique, jamais sous l'adresse")
        void consigneSansLadresse() {
            // Garder l'adresse d'un compte effacé dans le journal viderait
            // l'effacement de son objet.
            verify(audit).consigner(
                    eq(SecurityAuditService.Evenement.COMPTE_EFFACE),
                    eq("11111111-2222-3333-4444-555555555555"),
                    eq(IP), anyString());
        }
    }

    @Nested
    @DisplayName("Engagements en cours au moment de l'effacement")
    class Engagements {

        @Test
        @DisplayName("annule les trajets à venir et prévient leurs passagers")
        void annuleLesTrajetsAVenir() {
            // Un trajet futur dont le conducteur a disparu laisserait des
            // passagers attendre à un point de rendez-vous.
            Trip futur = trajet(LocalDateTime.now().plusDays(2), TripStatus.PLANNED);
            Booking passagerEnAttente = reservation(futur, BookingStatus.CONFIRMED);
            when(tripRepository.findByDriverIdOrderByDepartureTimeDesc(1L)).thenReturn(List.of(futur));
            when(bookingRepository.findByTripIdAndStatusIn(eq(10L), any()))
                    .thenReturn(new ArrayList<>(List.of(passagerEnAttente)));

            service.effacer(MOI, MOI, IP);

            assertThat(futur.getStatus()).isEqualTo(TripStatus.CANCELLED);
            assertThat(passagerEnAttente.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(passagerEnAttente.getStatusReason()).isNotNull();
        }

        @Test
        @DisplayName("laisse les trajets passés intacts")
        void laisseLesTrajetsPasses() {
            // Annuler rétroactivement un trajet effectué falsifierait l'historique
            // du passager, qui n'a rien demandé.
            Trip passe = trajet(LocalDateTime.now().minusDays(5), TripStatus.COMPLETED);
            when(tripRepository.findByDriverIdOrderByDepartureTimeDesc(1L)).thenReturn(List.of(passe));

            service.effacer(MOI, MOI, IP);

            assertThat(passe.getStatus()).isEqualTo(TripStatus.COMPLETED);
        }

        @Test
        @DisplayName("annule mes propres réservations à venir")
        void annuleMesReservationsAVenir() {
            Trip futurDautrui = Trip.builder()
                    .id(12L).uuid("trajet-futur-autrui")
                    .departureCity("Gand").arrivalCity("Bruges")
                    .departureTime(LocalDateTime.now().plusDays(3))
                    .availableSeats(2).pricePerSeat(new BigDecimal("6.00"))
                    .status(TripStatus.PLANNED)
                    .driver(autrui).vehicule(voiture)
                    .build();
            Booking maDemande = Booking.builder()
                    .id(102L).uuid("ma-resa-future")
                    .trip(futurDautrui).passenger(moi)
                    .seatsBooked(1).totalPrice(new BigDecimal("6.00"))
                    .status(BookingStatus.CONFIRMED)
                    .build();
            when(bookingRepository.findByPassengerIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(maDemande));

            service.effacer(MOI, MOI, IP);

            assertThat(maDemande.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(maDemande.getStatusReason()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Traces laissées ailleurs")
    class Traces {

        @Test
        @DisplayName("retire la plaque d'immatriculation et la photo du véhicule")
        void anonymiseLesVehicules() {
            // La plaque mène au titulaire par le répertoire de la DIV : c'est une
            // donnée personnelle. La marque et le modèle, détachés de toute
            // personne, n'en sont pas.
            when(vehiculeRepository.findByOwnerId(1L)).thenReturn(List.of(voiture));

            service.effacer(MOI, MOI, IP);

            assertThat(voiture.getLicensePlate()).isNull();
            assertThat(voiture.getPhotoUrl()).isNull();
            assertThat(voiture.getBrand()).isEqualTo("Renault");
        }

        @Test
        @DisplayName("vide les commentaires que la personne a écrits")
        void videLesCommentairesEcrits() {
            Review ecrit = avis(moi, autrui, 5, "Trajet agréable");
            when(reviewRepository.findByAuthorIdOrderByCreatedAtDesc(1L))
                    .thenReturn(new ArrayList<>(List.of(ecrit)));

            service.effacer(MOI, MOI, IP);

            assertThat(ecrit.getComment()).isNull();
            // La note subsiste : détachée de tout nom, elle ne se rapporte plus à
            // personne, et la retirer fausserait la moyenne d'un tiers.
            assertThat(ecrit.getRating()).isEqualTo(5);
        }

        @Test
        @DisplayName("vide aussi les commentaires reçus, qui peuvent la nommer")
        void videLesCommentairesRecus() {
            // « Camille était très ponctuelle » nomme une personne censée avoir
            // disparu de la base. Ne traiter qu'un seul sens laisserait la trace.
            Review recu = avis(autrui, moi, 4, "Camille était très ponctuelle");
            when(reviewRepository.findByTargetIdOrderByCreatedAtDesc(1L))
                    .thenReturn(new ArrayList<>(List.of(recu)));

            service.effacer(MOI, MOI, IP);

            assertThat(recu.getComment()).isNull();
            assertThat(recu.getRating()).isEqualTo(4);
        }

        @Test
        @DisplayName("n'échoue pas si la photo de profil résiste à la suppression")
        void toleUnePhotoIntrouvable() {
            // Une photo qui résiste ne doit pas faire échouer l'effacement du
            // reste : l'incident est tracé pour être repris à la main.
            moi.setPictureUrl("http://localhost:8080/uploads/avatars/absente.jpg");

            service.effacer(MOI, MOI, IP);

            assertThat(moi.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("refuse de sortir du dossier des photos")
        void refuseDeRemonterLarborescence() {
            // Le nom vient de la base, mais une donnée en base reste une donnée
            // d'entrée : le contrôle interdit malgré tout la traversée.
            moi.setPictureUrl("http://localhost:8080/uploads/avatars/../../application.properties");

            service.effacer(MOI, MOI, IP);

            assertThat(moi.getDeletedAt()).isNotNull();
            assertThat(moi.getPictureUrl()).isNull();
        }
    }

    // ────────────────────────────────── Fabriques ───────────────────────────────

    private Trip trajet(LocalDateTime depart, TripStatus statut) {
        return Trip.builder()
                .id(10L).uuid("trajet-uuid")
                .departureCity("Namur").departureAddress("Place d'Armes 1")
                .arrivalCity("Bruxelles").arrivalAddress("Rue de la Loi 16")
                .departureTime(depart)
                .availableSeats(3).pricePerSeat(new BigDecimal("5.00"))
                .description("Départ ponctuel")
                .status(statut)
                .driver(moi).vehicule(voiture)
                .build();
    }

    private Booking reservation(Trip trajet, BookingStatus statut) {
        return Booking.builder()
                .id(100L).uuid("resa-uuid")
                .trip(trajet).passenger(autrui)
                .seatsBooked(1).totalPrice(new BigDecimal("5.00"))
                .status(statut)
                .build();
    }

    private Review avis(User auteur, User cible, int note, String commentaire) {
        return Review.builder()
                .id(200L).uuid("avis-uuid")
                .booking(reservation(trajet(LocalDateTime.now().minusDays(2), TripStatus.COMPLETED),
                                     BookingStatus.COMPLETED))
                .author(auteur).target(cible)
                .rating(note).comment(commentaire)
                .build();
    }
}
