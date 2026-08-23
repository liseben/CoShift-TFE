package com.coshift.api.service;

import com.coshift.api.repository.OpenDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Jeu de données ouvert.
 *
 * <p>Ce point d'entrée est public et sans jeton : ce qu'il publie est publié
 * pour de bon. Deux propriétés comptent donc plus que les autres — qu'aucune
 * donnée identifiante n'en sorte, et que les agrégats soient justes, puisque
 * personne ne pourra les corriger une fois repris ailleurs.</p>
 *
 * <p>Le seuil de {@value OpenDataService#SEUIL} trajets est ce qui sépare une
 * statistique d'un déplacement individuel. Les tests le verrouillent.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OpenDataService — données ouvertes")
class OpenDataServiceTest {

    @Mock private OpenDataRepository repository;

    @InjectMocks private OpenDataService service;

    @BeforeEach
    void preparer() {
        when(repository.compterTrajets()).thenReturn(150L);
        when(repository.compterTrajetsAnnules()).thenReturn(12L);
        when(repository.compterReservationsAbouties()).thenReturn(258L);
        when(repository.compterPlacesPartagees()).thenReturn(300L);
        when(repository.compterPlacesRestantes()).thenReturn(100L);
        when(repository.compterOrganisations()).thenReturn(12L);
        when(repository.compterVillesEcartees(anyInt())).thenReturn(4L);
        when(repository.compterLiaisonsRecensees()).thenReturn(87L);
        when(repository.compterLiaisonsPubliables(anyInt())).thenReturn(0L);
        when(repository.bornesTemporelles()).thenReturn(List.<Object[]>of(new Object[] {
                Timestamp.valueOf(LocalDateTime.of(2026, 2, 1, 8, 0)),
                Timestamp.valueOf(LocalDateTime.of(2026, 8, 20, 18, 0)) }));
        when(repository.volumeParMois()).thenReturn(List.<Object[]>of(
                new Object[] { "2026-02", 20L, 40L },
                new Object[] { "2026-03", 44L, 90L }));
        when(repository.villesDesservies(anyInt())).thenReturn(List.<Object[]>of(
                new Object[] { "Namur", 30L, 12L, 55L, new BigDecimal("5.20") },
                new Object[] { "Bruxelles", 12L, 40L, 80L, new BigDecimal("6.00") }));
    }

    @Nested
    @DisplayName("Contenu publié")
    class Contenu {

        @Test
        @DisplayName("annonce sa licence et l'attribution demandée")
        void annonceSaLicence() {
            // Publier une donnée sans licence, c'est la publier sous droit
            // d'auteur intégral : personne ne peut légalement la réutiliser.
            var licence = service.jeuDeDonnees().licence();

            assertThat(licence.nom()).contains("Licence Ouverte");
            assertThat(licence.url()).startsWith("https://");
            assertThat(licence.attributionDemandee()).contains("CoShift");
        }

        @Test
        @DisplayName("énumère ce qui est volontairement exclu")
        void enumereLesExclusions() {
            var exclues = service.jeuDeDonnees().anonymisation().donneesExclues();

            assertThat(exclues).isNotEmpty();
            assertThat(String.join(" ", exclues))
                    .contains("identité")
                    .contains("adresses");
        }

        @Test
        @DisplayName("publie les volumes tels que la base les rend")
        void publieLesVolumes() {
            var volumes = service.jeuDeDonnees().volumes();

            assertThat(volumes.trajetsPublies()).isEqualTo(150L);
            assertThat(volumes.trajetsAnnules()).isEqualTo(12L);
            assertThat(volumes.reservationsAbouties()).isEqualTo(258L);
        }

        @Test
        @DisplayName("distingue les trajets annulés des trajets publiés")
        void distingueLesAnnules() {
            // Les publier ensemble fausserait toute lecture des volumes ; les
            // taire relèverait de la malhonnêteté statistique.
            var volumes = service.jeuDeDonnees().volumes();

            assertThat(volumes.trajetsAnnules()).isNotEqualTo(volumes.trajetsPublies());
        }

        @Test
        @DisplayName("convertit les bornes temporelles en dates")
        void convertitLesBornes() {
            var perimetre = service.jeuDeDonnees().perimetre();

            assertThat(perimetre.premierTrajet()).isEqualTo(LocalDate.of(2026, 2, 1));
            assertThat(perimetre.dernierTrajet()).isEqualTo(LocalDate.of(2026, 8, 20));
        }

        @Test
        @DisplayName("tolère une base sans le moindre trajet")
        void tolereUneBaseVide() {
            // Au premier jour de mise en ligne, aucune borne n'existe. Le jeu
            // doit rester servable plutôt que de tomber.
            when(repository.bornesTemporelles()).thenReturn(List.of());

            var perimetre = service.jeuDeDonnees().perimetre();

            assertThat(perimetre.premierTrajet()).isNull();
            assertThat(perimetre.dernierTrajet()).isNull();
        }
    }

    @Nested
    @DisplayName("Taux de remplissage")
    class TauxDeRemplissage {

        @Test
        @DisplayName("calcule le rapport entre places partagées et places offertes")
        void calculeLeTaux() {
            // 300 partagées sur 400 offertes.
            assertThat(service.jeuDeDonnees().volumes().tauxDeRemplissage())
                    .isEqualByComparingTo(new BigDecimal("75.0"));
        }

        @Test
        @DisplayName("arrondit à une décimale")
        void arronditAUneDecimale() {
            when(repository.compterPlacesPartagees()).thenReturn(1L);
            when(repository.compterPlacesRestantes()).thenReturn(2L);

            assertThat(service.jeuDeDonnees().volumes().tauxDeRemplissage())
                    .isEqualByComparingTo(new BigDecimal("33.3"));
        }

        @Test
        @DisplayName("publie zéro plutôt qu'une division impossible")
        void neDivisePasParZero() {
            // Un taux calculé sur zéro place n'a pas de sens ; laisser passer
            // une division par zéro en aurait encore moins.
            when(repository.compterPlacesPartagees()).thenReturn(0L);
            when(repository.compterPlacesRestantes()).thenReturn(0L);

            assertThat(service.jeuDeDonnees().volumes().tauxDeRemplissage())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Seuil d'anonymisation")
    class Seuil {

        @Test
        @DisplayName("transmet le seuil aux requêtes d'agrégation")
        void transmetLeSeuil() {
            service.jeuDeDonnees();

            verify(repository).villesDesservies(OpenDataService.SEUIL);
            verify(repository).compterVillesEcartees(OpenDataService.SEUIL);
            verify(repository).compterLiaisonsPubliables(OpenDataService.SEUIL);
        }

        @Test
        @DisplayName("publie le seuil et le nombre de villes écartées")
        void publieCeQuIlEcarte() {
            // Annoncer combien de villes ont été retirées, c'est ce qui permet à
            // un réutilisateur de savoir que le jeu n'est pas exhaustif.
            var anonymisation = service.jeuDeDonnees().anonymisation();

            assertThat(anonymisation.seuil()).isEqualTo(OpenDataService.SEUIL);
            assertThat(anonymisation.villesEcartees()).isEqualTo(4L);
            assertThat(anonymisation.liaisonsRecensees()).isEqualTo(87L);
        }

        @Test
        @DisplayName("explique pourquoi les liaisons ne sont pas publiées")
        void expliqueLesLiaisons() {
            // Publier un couple départ-arrivée emprunté une ou deux fois
            // reviendrait à décrire un déplacement individuel.
            assertThat(service.jeuDeDonnees().anonymisation().noteSurLesLiaisons())
                    .contains("déplacement individuel");
        }
    }

    @Nested
    @DisplayName("Mise en cache")
    class Cache {

        @Test
        @DisplayName("ne recalcule pas dans la fenêtre de validité")
        void neRecalculePas() {
            // Le point d'entrée est public et sans jeton : sans cache, chaque
            // appel déclencherait une dizaine d'agrégations sur toute la base.
            service.jeuDeDonnees();
            service.jeuDeDonnees();
            service.jeuDeDonnees();

            verify(repository, times(1)).compterTrajets();
        }

        @Test
        @DisplayName("annonce sa date de péremption")
        void annonceSaPeremption() {
            // Un réutilisateur doit savoir à quel moment il tient une donnée
            // périmée.
            var jeu = service.jeuDeDonnees();

            assertThat(jeu.valableJusqua()).isAfter(jeu.genereLe());
            assertThat(jeu.valableJusqua())
                    .isEqualTo(jeu.genereLe().plusMinutes(OpenDataService.CACHE_MINUTES));
        }
    }

    @Nested
    @DisplayName("Export CSV")
    class Csv {

        @Test
        @DisplayName("commence par une ligne d'en-tête")
        void commenceParUnEnTete() {
            // C'est le format qu'ouvre un tableur, donc celui par lequel une
            // donnée ouverte est réellement réutilisée par qui n'écrit pas de
            // code. Sans en-tête, les colonnes sont indevinables.
            assertThat(service.villesEnCsv().lines().findFirst())
                    .hasValue("ville,trajets_au_depart,trajets_a_l_arrivee,"
                            + "places_partagees,prix_moyen_au_depart_eur");
        }

        @Test
        @DisplayName("écrit une ligne par ville")
        void uneLigneParVille() {
            assertThat(service.villesEnCsv().lines()).hasSize(3); // en-tête + 2
        }

        @Test
        @DisplayName("rend les valeurs sans notation scientifique")
        void sansNotationScientifique() {
            assertThat(service.villesEnCsv()).contains("Namur,30,12,55,5.20");
        }

        @Test
        @DisplayName("encadre une valeur contenant une virgule")
        void encadreUneVirgule() {
            // RFC 4180 : sans guillemets, « Braine-l'Alleud, Wallonie » ferait
            // glisser toutes les colonnes suivantes d'un cran.
            when(repository.villesDesservies(anyInt())).thenReturn(List.<Object[]>of(
                    new Object[] { "Braine, Wallonie", 5L, 5L, 10L, new BigDecimal("4.00") }));

            assertThat(service.villesEnCsv()).contains("\"Braine, Wallonie\"");
        }

        @Test
        @DisplayName("double les guillemets internes")
        void doubleLesGuillemets() {
            when(repository.villesDesservies(anyInt())).thenReturn(List.<Object[]>of(
                    new Object[] { "Ville \"test\"", 5L, 5L, 10L, new BigDecimal("4.00") }));

            assertThat(service.villesEnCsv()).contains("\"Ville \"\"test\"\"\"");
        }

        @Test
        @DisplayName("laisse la colonne vide plutôt que d'écrire « null »")
        void colonneVideSiPrixAbsent() {
            when(repository.villesDesservies(anyInt())).thenReturn(List.<Object[]>of(
                    new Object[] { "Namur", 5L, 5L, 10L, null }));

            assertThat(service.villesEnCsv()).contains("Namur,5,5,10,\n");
            assertThat(service.villesEnCsv()).doesNotContain("null");
        }

        @Test
        @DisplayName("rend un CSV réduit à son en-tête si aucune ville n'atteint le seuil")
        void enTeteSeulSiAucuneVille() {
            when(repository.villesDesservies(anyInt())).thenReturn(List.of());

            assertThat(service.villesEnCsv().lines()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Conversions de types")
    class Conversions {

        @Test
        @DisplayName("convertit un entier de la base en décimal à deux chiffres")
        void convertitUnEntierEnDecimal() {
            // Les moteurs renvoient parfois un Double, parfois un BigDecimal,
            // selon la fonction d'agrégation employée.
            when(repository.villesDesservies(anyInt())).thenReturn(List.<Object[]>of(
                    new Object[] { "Namur", 5L, 5L, 10L, 4.5d }));

            assertThat(service.jeuDeDonnees().villes().get(0).prixMoyenAuDepart())
                    .isEqualByComparingTo(new BigDecimal("4.50"));
        }

        @Test
        @DisplayName("tolère un comptage absent")
        void tolereUnComptageAbsent() {
            when(repository.volumeParMois()).thenReturn(List.<Object[]>of(
                    new Object[] { "2026-02", null, null }));

            var mois = service.jeuDeDonnees().parMois().get(0);

            assertThat(mois.trajets()).isZero();
            assertThat(mois.placesPartagees()).isZero();
        }
    }
}
