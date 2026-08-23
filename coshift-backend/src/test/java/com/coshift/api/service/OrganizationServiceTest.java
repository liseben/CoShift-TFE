package com.coshift.api.service;

import com.coshift.api.entity.Organization;
import com.coshift.api.entity.User;
import com.coshift.api.repository.OrganizationRepository;
import com.coshift.api.repository.OrganizationStatsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Rattachement à une organisation.
 *
 * <p>C'est la classe qui décide qui appartient à quel cercle, donc qui voit
 * quoi. Les cas ci-dessous portent d'abord sur ses refus : un domaine inconnu
 * ne rattache à rien, une organisation désactivée ne rattache plus personne, et
 * l'appartenance prime sur le domaine dès que les deux divergent.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrganizationService — rattachement au cercle")
class OrganizationServiceTest {

    @Mock private OrganizationRepository organizationRepository;
    @Mock private OrganizationStatsRepository statsRepository;
    @Mock private Messages messages;

    @InjectMocks private OrganizationService service;

    private static Organization organisation(long id, String slug, String domaine) {
        return Organization.builder()
                .id(id).uuid("org-" + id).name(slug).slug(slug)
                .emailDomain(domaine).active(true)
                .build();
    }

    private static User personne(String email, Organization... siennes) {
        Set<Organization> ensemble = new LinkedHashSet<>();
        for (Organization o : siennes) ensemble.add(o);
        return User.builder().id(1L).email(email).organizations(ensemble).build();
    }

    @Nested
    @DisplayName("Lecture du domaine")
    class Domaine {

        @Test
        @DisplayName("retient ce qui suit l'arobase, en minuscules")
        void extraitLeDomaine() {
            assertThat(service.domaine("Prenom.Nom@Solvantis.BE")).isEqualTo("solvantis.be");
        }

        @Test
        @DisplayName("retient la derniere arobase, pas la premiere")
        void toleUneArobaseDansLaPartieLocale() {
            /* Une partie locale entre guillemets peut legalement contenir une
               arobase. Decouper sur la premiere donnerait un domaine errone,
               donc un rattachement a la mauvaise organisation. */
            assertThat(service.domaine("\"a@b\"@solvantis.be")).isEqualTo("solvantis.be");
        }

        @Test
        @DisplayName("ne leve pas sur une adresse absente ou tronquee")
        void resistanceAuxAdressesMalformees() {
            assertThat(service.domaine(null)).isNull();
            assertThat(service.domaine("sans-arobase")).isNull();
            assertThat(service.domaine("rien-apres@")).isNull();
        }
    }

    @Nested
    @DisplayName("Rattachement a l'inscription")
    class Rattachement {

        @Test
        @DisplayName("rejoint l'organisation qui revendique le domaine")
        void rattacheParLeDomaine() {
            Organization solvantis = organisation(1L, "solvantis", "solvantis.be");
            when(organizationRepository.findByEmailDomainIgnoreCaseAndActiveTrue("solvantis.be"))
                    .thenReturn(Optional.of(solvantis));

            User nouveau = personne("julie@solvantis.be");
            Optional<Organization> rejointe = service.rattacher(nouveau);

            assertThat(rejointe).contains(solvantis);
            assertThat(nouveau.getOrganizations()).containsExactly(solvantis);
        }

        @Test
        @DisplayName("un domaine inconnu ne rattache a rien et ne cree aucune organisation")
        void domaineInconnu() {
            when(organizationRepository.findByEmailDomainIgnoreCaseAndActiveTrue(anyString()))
                    .thenReturn(Optional.empty());

            User nouveau = personne("quelquun@gmail.com");
            assertThat(service.rattacher(nouveau)).isEmpty();
            assertThat(nouveau.getOrganizations()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Organisation par defaut d'un conducteur")
    class ParDefaut {

        @Test
        @DisplayName("celle du domaine, quand il en a plusieurs")
        void prefereLorigine() {
            Organization solvantis = organisation(1L, "solvantis", "solvantis.be");
            Organization festival = organisation(2L, "ardenn-son", "ardenn-son.be");

            User polyvalent = personne("julie@solvantis.be", festival, solvantis);

            assertThat(service.organisationParDefaut(polyvalent)).contains(solvantis);
        }

        @Test
        @DisplayName("l'unique organisation, quand le domaine ne correspond a aucune")
        void replieSurLuniqueOrganisation() {
            /* Cas d'un compte invite : l'adresse ne releve d'aucune organisation
               mais la personne a bien ete inscrite dans un cercle. */
            Organization festival = organisation(2L, "ardenn-son", "ardenn-son.be");
            User invite = personne("julie@gmail.com", festival);

            assertThat(service.organisationParDefaut(invite)).contains(festival);
        }

        @Test
        @DisplayName("aucune, quand deux organisations se valent")
        void refuseDeTrancherAlaPlaceDuConducteur() {
            Organization festival = organisation(2L, "ardenn-son", "ardenn-son.be");
            Organization salon = organisation(3L, "salon-mobilite", "salon-mobilite.be");
            User invite = personne("julie@gmail.com", festival, salon);

            assertThat(service.organisationParDefaut(invite)).isEmpty();
        }

        @Test
        @DisplayName("l'appartenance prime sur le domaine")
        void appartenanceAvantDomaine() {
            /* Quelqu'un dont l'adresse est @solvantis.be mais qui a ete retire
               de Solvantis ne publie plus chez Solvantis. Le domaine dit d'ou
               vient la personne, l'appartenance dit ce qui lui est ouvert. */
            Organization festival = organisation(2L, "ardenn-son", "ardenn-son.be");
            User ancien = personne("julie@solvantis.be", festival);

            assertThat(service.organisationParDefaut(ancien)).contains(festival);
        }

        @Test
        @DisplayName("une organisation desactivee ne compte pas")
        void ignoreLesOrganisationsDesactivees() {
            Organization resiliee = organisation(1L, "solvantis", "solvantis.be");
            resiliee.setActive(false);
            User adherent = personne("julie@solvantis.be", resiliee);

            assertThat(service.organisationParDefaut(adherent)).isEmpty();
            assertThat(service.identifiantsDesOrganisations(adherent)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Partage du cercle")
    class Cercle {

        @Test
        @DisplayName("un trajet sans organisation est visible de tous")
        void trajetSansOrganisation() {
            User etranger = personne("quelquun@gmail.com");
            assertThat(service.partageLeCercle(etranger, null)).isTrue();
        }

        @Test
        @DisplayName("un trajet d'une autre organisation ne l'est pas")
        void trajetDunAutreCercle() {
            Organization solvantis = organisation(1L, "solvantis", "solvantis.be");
            Organization novaris = organisation(2L, "novaris", "novaris.be");
            User chezNovaris = personne("marc@novaris.be", novaris);

            assertThat(service.partageLeCercle(chezNovaris, solvantis)).isFalse();
            assertThat(service.partageLeCercle(chezNovaris, novaris)).isTrue();
        }

        @Test
        @DisplayName("quelqu'un sans organisation ne partage aucun cercle")
        void sansOrganisation() {
            Organization solvantis = organisation(1L, "solvantis", "solvantis.be");
            User isole = personne("quelquun@gmail.com");

            assertThat(service.identifiantsDesOrganisations(isole)).isEmpty();
            assertThat(service.partageLeCercle(isole, solvantis)).isFalse();
        }
    }
    @Nested
    @DisplayName("Tableau de bord")
    class TableauDeBord {

        private Organization solvantis;

        @org.junit.jupiter.api.BeforeEach
        void chiffres() {
            solvantis = organisation(1L, "solvantis", "solvantis.be");
            when(messages.get(anyString())).thenReturn("motif");
            when(statsRepository.volumeParMois(1L)).thenReturn(List.of());
        }

        @Test
        @DisplayName("le taux de remplissage rapporte les places occupees aux places offertes")
        void tauxSurLesPlacesEtNonSurLesTrajets() {
            /* Le denominateur est le total des places offertes, occupees plus
               libres. Rapporter au nombre de trajets ferait passer une voiture
               de cinq places a moitie vide pour une de deux places pleine. */
            when(statsRepository.compterPlacesPartagees(1L)).thenReturn(7L);
            when(statsRepository.compterPlacesRestantes(1L)).thenReturn(14L);

            var tableau = service.tableauDeBord(personne("julie@solvantis.be", solvantis));

            assertThat(tableau).hasSize(1);
            assertThat(tableau.get(0).volumes().tauxRemplissage())
                    .isEqualByComparingTo(new java.math.BigDecimal("33.3"));
        }

        @Test
        @DisplayName("une organisation sans place offerte a un taux nul, pas une erreur")
        void aucunePlaceOfferte() {
            when(statsRepository.compterPlacesPartagees(1L)).thenReturn(0L);
            when(statsRepository.compterPlacesRestantes(1L)).thenReturn(0L);

            var tableau = service.tableauDeBord(personne("julie@solvantis.be", solvantis));

            assertThat(tableau.get(0).volumes().tauxRemplissage())
                    .isEqualByComparingTo(java.math.BigDecimal.ZERO);
        }

        @Test
        @DisplayName("annonce ce qui n'est pas mesure au lieu de l'estimer")
        void annonceLesAbsences() {
            /* Une distance approchee serait le chiffre le plus facile a produire
               de tout cet ecran, et le seul que personne ne songerait a
               verifier. L'absence est annoncee, pas comblee. */
            var tableau = service.tableauDeBord(personne("julie@solvantis.be", solvantis));

            assertThat(tableau.get(0).nonMesure().distanceParcourue()).isTrue();
            assertThat(tableau.get(0).nonMesure().emissionsEvitees()).isTrue();
            assertThat(tableau.get(0).nonMesure().motif()).isNotBlank();
        }

        @Test
        @DisplayName("l'organisation d'origine vient en tete, les autres par ordre alphabetique")
        void origineEnTete() {
            /* L'ordre n'est pas cosmetique : le formulaire de publication
               preselectionne la premiere entree. Un ordre purement alphabetique
               y mettait « Batiplus » alors que le serveur aurait retenu
               « Val Vert » — l'ecran annoncait autre chose que ce qu'il
               faisait. */
            Organization batiplus = organisation(2L, "batiplus", "batiplus.be");
            batiplus.setName("Batiplus Construct");
            Organization valVert = organisation(3L, "val-vert", "val-vert.be");
            valVert.setName("Clinique du Val Vert");
            when(statsRepository.volumeParMois(2L)).thenReturn(List.of());
            when(statsRepository.volumeParMois(3L)).thenReturn(List.of());

            var tableau = service.tableauDeBord(personne("sarah@val-vert.be", batiplus, valVert));

            assertThat(tableau).extracting(t -> t.slug()).containsExactly("val-vert", "batiplus");
        }

        @Test
        @DisplayName("une organisation desactivee ne figure pas au tableau")
        void ecarteLesOrganisationsDesactivees() {
            solvantis.setActive(false);

            assertThat(service.tableauDeBord(personne("julie@solvantis.be", solvantis))).isEmpty();
        }

        @Test
        @DisplayName("aucune organisation donne une liste vide, pas une erreur")
        void membreSansOrganisation() {
            /* Le cas d'une adresse dont le domaine n'est revendique par
               personne. Ce n'est pas une anomalie. */
            assertThat(service.tableauDeBord(personne("quelquun@gmail.com"))).isEmpty();
        }
    }
}
