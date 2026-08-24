package com.coshift.api.service;

import com.coshift.api.entity.BookingStatus;
import com.coshift.api.entity.Organization;
import com.coshift.api.entity.Role;
import com.coshift.api.entity.User;
import com.coshift.api.exception.BadRequestException;
import com.coshift.api.exception.ResourceNotFoundException;
import com.coshift.api.exception.UnauthorizedException;
import com.coshift.api.repository.AdminStatsRepository;
import com.coshift.api.repository.UserRepository;
import com.coshift.api.security.LoginAttemptService;
import com.coshift.api.security.SecurityAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Supervision et modération.
 *
 * <p>Deux choses se jouent ici, et les tests portent sur elles seules : la
 * <strong>portée</strong> — un administrateur d'organisation ne doit pas voir
 * la plateforme entière — et les <strong>refus</strong> de la suspension, qui
 * sont ce qui empêche une console d'administration de devenir une arme.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminService — supervision et modération")
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AdminStatsRepository statsRepository;
    @Mock private OrganizationService organizationService;
    @Mock private LoginAttemptService loginAttemptService;
    @Mock private SecurityAuditService audit;
    @Mock private Messages messages;

    @InjectMocks private AdminService service;

    private User superAdmin;
    private User adminOrg;
    private User membre;
    private Organization solvantis;

    @BeforeEach
    void preparer() {
        solvantis = Organization.builder()
                .id(1L).uuid("org-solvantis").name("Solvantis Belgium")
                .slug("solvantis").emailDomain("solvantis.be").active(true).build();

        superAdmin = compte(1L, "fanny@solvantis.be", Role.SUPER_ADMIN, solvantis);
        adminOrg = compte(2L, "julien@he-condroz.be", Role.ADMIN, solvantis);
        membre = compte(3L, "marc@solvantis.be", Role.USER, solvantis);

        when(messages.get(anyString())).thenReturn("message");
        when(organizationService.identifiantsDesOrganisations(any())).thenReturn(List.of(1L));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findByUuid("uuid-3")).thenReturn(Optional.of(membre));
    }

    private static User compte(Long id, String email, Role role, Organization... orgs) {
        Set<Organization> ensemble = new LinkedHashSet<>();
        for (Organization o : orgs) ensemble.add(o);
        return User.builder()
                .id(id).uuid("uuid-" + id).email(email)
                .firstname("Prenom" + id).lastname("Nom" + id)
                .password("peu-importe").emailVerified(true)
                .role(role).organizations(ensemble)
                .build();
    }

    @Nested
    @DisplayName("Portee de la supervision")
    class Portee {

        @Test
        @DisplayName("un SUPER_ADMIN voit la plateforme entiere")
        void superAdminVoitTout() {
            service.apercu(superAdmin);

            ArgumentCaptor<Boolean> plateforme = ArgumentCaptor.forClass(Boolean.class);
            verify(statsRepository).compterMembres(plateforme.capture(), anyCollection());
            assertThat(plateforme.getValue()).isTrue();
        }

        @Test
        @DisplayName("un ADMIN est borne a ses organisations")
        void adminBorneASonCercle() {
            /* Sans cette borne, distribuer un role d'administrateur a une
               entreprise cliente lui ouvrirait les membres et les trajets de
               toutes les autres : le cercle ferme se contournerait par un role
               au lieu de se contourner par une requete. */
            service.apercu(adminOrg);

            ArgumentCaptor<Boolean> plateforme = ArgumentCaptor.forClass(Boolean.class);
            ArgumentCaptor<java.util.Collection<Long>> orgs = ArgumentCaptor.captor();
            verify(statsRepository).compterMembres(plateforme.capture(), orgs.capture());

            assertThat(plateforme.getValue()).isFalse();
            assertThat(orgs.getValue()).containsExactly(1L);
        }

        @Test
        @DisplayName("l'apercu annonce sa portee au lieu de la laisser deviner")
        void laPorteeEstAnnoncee() {
            assertThat(service.apercu(superAdmin).portee()).isEqualTo("PLATEFORME");
            assertThat(service.apercu(adminOrg).portee()).isEqualTo("ORGANISATIONS");
            assertThat(service.apercu(adminOrg).organisations()).containsExactly("Solvantis Belgium");
        }

        @Test
        @DisplayName("les comptes effaces ne sont comptes qu'a la portee plateforme")
        void effacesHorsPorteeBornee() {
            /* L'anonymisation vide le rattachement : un compte efface
               n'appartient plus a aucun cercle. Le compter par organisation
               donnerait toujours zero et laisserait croire qu'aucun effacement
               n'a eu lieu. */
            when(statsRepository.compterMembresEfface()).thenReturn(7L);

            assertThat(service.apercu(superAdmin).membres().effaces()).isEqualTo(7L);
            assertThat(service.apercu(adminOrg).membres().effaces()).isZero();
        }

        @Test
        @DisplayName("un administrateur sans organisation ne voit rien, au lieu de tout voir")
        void administrateurSansOrganisation() {
            /* Le piege serait de laisser tomber la clause quand la liste est
               vide : la supervision s'ouvrirait alors a toute la plateforme. */
            when(organizationService.identifiantsDesOrganisations(adminOrg)).thenReturn(List.of());

            service.apercu(adminOrg);

            ArgumentCaptor<java.util.Collection<Long>> orgs = ArgumentCaptor.captor();
            verify(statsRepository).compterMembres(anyBoolean(), orgs.capture());
            assertThat(orgs.getValue()).isNotEmpty().allMatch(id -> id < 0);
        }

        @Test
        @DisplayName("les reservations sont comptees statut par statut")
        void reservationsParStatut() {
            when(statsRepository.compterReservations(BookingStatus.PENDING, true, List.of(1L))).thenReturn(4L);
            when(statsRepository.compterReservations(BookingStatus.CANCELLED, true, List.of(1L))).thenReturn(2L);

            var apercu = service.apercu(superAdmin);
            assertThat(apercu.reservations().enAttente()).isEqualTo(4L);
            assertThat(apercu.reservations().annulees()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("Suspension d'un compte")
    class Suspension {

        @Test
        @DisplayName("un ADMIN d'organisation ne peut pas suspendre")
        void reserveAuSuperAdmin() {
            /* Consulter n'engage rien, suspendre engage la plateforme vis-a-vis
               de la personne. Sans ce refus, une entreprise cliente fermerait le
               compte d'un employe a travers un outil qui n'est pas le sien. */
            assertThatThrownBy(() -> service.suspendre(adminOrg, "uuid-3", "motif"))
                    .isInstanceOf(UnauthorizedException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("refuse une suspension sans motif")
        void motifObligatoire() {
            assertThatThrownBy(() -> service.suspendre(superAdmin, "uuid-3", "   "))
                    .isInstanceOf(BadRequestException.class);
            assertThatThrownBy(() -> service.suspendre(superAdmin, "uuid-3", null))
                    .isInstanceOf(BadRequestException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("refuse de se suspendre soi-meme")
        void pasDeSuspensionDeSoi() {
            /* Personne ne pourrait plus lever la mesure. */
            when(userRepository.findByUuid("uuid-1")).thenReturn(Optional.of(superAdmin));

            assertThatThrownBy(() -> service.suspendre(superAdmin, "uuid-1", "motif"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("refuse de suspendre un autre administrateur de plateforme")
        void pasDeSuspensionEntreSuperAdmins() {
            /* Deux comptes de supervision qui se neutralisent laissent
               l'application sans pilote. */
            User autre = compte(9L, "autre@coshift.be", Role.SUPER_ADMIN);
            when(userRepository.findByUuid("uuid-9")).thenReturn(Optional.of(autre));

            assertThatThrownBy(() -> service.suspendre(superAdmin, "uuid-9", "motif"))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("refuse de suspendre deux fois, pour ne pas ecraser la premiere date")
        void pasDeSecondeSuspension() {
            membre.setSuspendedAt(LocalDateTime.now().minusDays(3));

            assertThatThrownBy(() -> service.suspendre(superAdmin, "uuid-3", "motif"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("un compte efface est introuvable, pas moderable")
        void compteEffaceIntrouvable() {
            membre.setDeletedAt(LocalDateTime.now().minusMonths(2));

            assertThatThrownBy(() -> service.suspendre(superAdmin, "uuid-3", "motif"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("pose la date, le motif, et consigne qui a decide")
        void suspendEtConsigne() {
            var reponse = service.suspendre(superAdmin, "uuid-3", "  Annonces hors cadre  ");

            assertThat(membre.getSuspendedAt()).isNotNull();
            assertThat(membre.getSuspensionReason()).isEqualTo("Annonces hors cadre");
            assertThat(reponse.suspendedAt()).isNotNull();

            ArgumentCaptor<String> detail = ArgumentCaptor.forClass(String.class);
            verify(audit).consigner(
                    org.mockito.ArgumentMatchers.eq(SecurityAuditService.Evenement.COMPTE_SUSPENDU),
                    anyString(), anyString(), detail.capture());
            assertThat(detail.getValue()).contains("fanny@solvantis.be");
        }

        @Test
        @DisplayName("tronque un motif trop long plutot que de refuser")
        void motifTronque() {
            /* La colonne s'arrete a 255 caracteres. Refuser ferait perdre le
               texte deja saisi ; tronquer conserve l'essentiel, qui est en tete. */
            service.suspendre(superAdmin, "uuid-3", "x".repeat(400));

            assertThat(membre.getSuspensionReason()).hasSize(255);
        }
    }

    @Nested
    @DisplayName("Levee de la suspension")
    class Reactivation {

        @Test
        @DisplayName("efface la date et le motif")
        void effaceLaMesure() {
            /* Conserver le motif laisserait sur un compte redevenu actif la
               trace d'une accusation a laquelle rien ne correspond plus. */
            membre.setSuspendedAt(LocalDateTime.now().minusDays(1));
            membre.setSuspensionReason("Annonces hors cadre");

            service.reactiver(superAdmin, "uuid-3");

            assertThat(membre.getSuspendedAt()).isNull();
            assertThat(membre.getSuspensionReason()).isNull();
        }

        @Test
        @DisplayName("refuse de reactiver un compte qui ne l'etait pas")
        void refuseSiPasSuspendu() {
            assertThatThrownBy(() -> service.reactiver(superAdmin, "uuid-3"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("reservee au SUPER_ADMIN, comme la suspension")
        void reserveeAuSuperAdmin() {
            membre.setSuspendedAt(LocalDateTime.now());

            assertThatThrownBy(() -> service.reactiver(adminOrg, "uuid-3"))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    @Nested
    @DisplayName("Changement de role")
    class Roles {

        @Test
        @DisplayName("un ADMIN d'organisation ne peut pas distribuer de roles")
        void reserveAuSuperAdmin() {
            /* C'est le pouvoir qui contient tous les autres : un administrateur
               d'organisation qui pourrait nommer des administrateurs se
               nommerait lui-meme administrateur de plateforme. */
            assertThatThrownBy(() -> service.changerRole(adminOrg, "uuid-3", Role.ADMIN))
                    .isInstanceOf(UnauthorizedException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("promeut un membre verifie")
        void promotion() {
            var reponse = service.changerRole(superAdmin, "uuid-3", Role.ADMIN);

            assertThat(membre.getRole()).isEqualTo(Role.ADMIN);
            assertThat(reponse.role()).isEqualTo(Role.ADMIN);
            verify(audit).consigner(
                    org.mockito.ArgumentMatchers.eq(SecurityAuditService.Evenement.ROLE_MODIFIE),
                    anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("refuse de changer son propre role")
        void pasSurSoi() {
            /* Se retrograder par megarde fermerait la porte derriere soi, et il
               ne resterait qu'une migration pour la rouvrir. */
            when(userRepository.findByUuid("uuid-1")).thenReturn(Optional.of(superAdmin));

            assertThatThrownBy(() -> service.changerRole(superAdmin, "uuid-1", Role.USER))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("refuse un role d'administration a une adresse non confirmee")
        void pasSurUnCompteNonVerifie() {
            /* Donner un role a une adresse non prouvee, c'est le donner a qui la
               controle. */
            membre.setEmailVerified(false);

            assertThatThrownBy(() -> service.changerRole(superAdmin, "uuid-3", Role.ADMIN))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("retrograder vers USER reste possible sur un compte non verifie")
        void retrogradationToujoursPossible() {
            /* Le controle porte sur l'attribution d'un pouvoir, pas sur son
               retrait : refuser de retrograder un compte non verifie
               laisserait un role a quelqu'un dont l'adresse n'est pas prouvee. */
            membre.setEmailVerified(false);
            membre.setRole(Role.ADMIN);

            assertThatCode(() -> service.changerRole(superAdmin, "uuid-3", Role.USER))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("refuse de retrograder le dernier administrateur de plateforme")
        void dernierAdministrateur() {
            /* Une plateforme sans personne pour l'administrer ne se repare qu'en
               base. */
            membre.setRole(Role.SUPER_ADMIN);
            when(userRepository.countByRole(Role.SUPER_ADMIN)).thenReturn(1L);

            assertThatThrownBy(() -> service.changerRole(superAdmin, "uuid-3", Role.USER))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("retrograde un administrateur quand il en reste d'autres")
        void retrogradationPossible() {
            membre.setRole(Role.SUPER_ADMIN);
            when(userRepository.countByRole(Role.SUPER_ADMIN)).thenReturn(2L);

            service.changerRole(superAdmin, "uuid-3", Role.USER);

            assertThat(membre.getRole()).isEqualTo(Role.USER);
        }
    }

    @Nested
    @DisplayName("Freinages en cours")
    class Blocages {

        @Test
        @DisplayName("reserves au SUPER_ADMIN")
        void reservesAuSuperAdmin() {
            /* La liste porte des adresses IP et des adresses electroniques, y
               compris de comptes etrangers a toute organisation de l'appelant. */
            assertThatThrownBy(() -> service.blocages(adminOrg))
                    .isInstanceOf(UnauthorizedException.class);

            verify(loginAttemptService, never()).blocagesEnCours();
        }

        @Test
        @DisplayName("transmis tels quels au SUPER_ADMIN")
        void transmisAuSuperAdmin() {
            when(loginAttemptService.blocagesEnCours()).thenReturn(List.of());

            assertThat(service.blocages(superAdmin)).isEmpty();
            verify(loginAttemptService).blocagesEnCours();
        }
    }
}
