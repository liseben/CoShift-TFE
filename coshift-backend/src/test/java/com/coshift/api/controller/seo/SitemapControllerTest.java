package com.coshift.api.controller.seo;

import com.coshift.api.entity.Article;
import com.coshift.api.service.ArticleService;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Plan du site.
 *
 * <p>Deux propriétés se testent bien ici, et elles se cassent toutes deux en
 * silence. Un plan mal formé n'est pas rejeté par l'application : il est
 * simplement ignoré par les moteurs, et personne ne s'en aperçoit. Et une
 * adresse annoncée ici qui diffère de celle des liens du site produit deux
 * versions de la même page — du contenu dupliqué, qui se paie au
 * référencement.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SitemapController — plan du site")
class SitemapControllerTest {

    @Mock private ArticleService articleService;

    @InjectMocks private SitemapController controller;

    private static final String SITE = "https://coshift.be";

    @BeforeEach
    void preparer() {
        // Normalement injecté par @Value depuis app.public-base-url.
        ReflectionTestUtils.setField(controller, "siteUrl", SITE);
        when(articleService.getAllArticles()).thenReturn(List.of());
    }

    private String plan() {
        return controller.sitemap().getBody();
    }

    @Nested
    @DisplayName("Forme du document")
    class Forme {

        @Test
        @DisplayName("déclare l'espace de noms attendu par les moteurs")
        void declareLespaceDeNoms() {
            // Sans lui, le document est du XML valide que personne n'interprète.
            assertThat(plan())
                    .startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                    .contains("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">")
                    .endsWith("</urlset>\n");
        }

        @Test
        @DisplayName("ouvre et ferme autant de balises url")
        void balisesEquilibrees() {
            String xml = plan();

            assertThat(compter(xml, "<url>")).isEqualTo(compter(xml, "</url>"));
        }

        @Test
        @DisplayName("porte les quatre champs attendus pour chaque adresse")
        void quatreChampsParAdresse() {
            String xml = plan();
            int adresses = compter(xml, "<url>");

            assertThat(compter(xml, "<loc>")).isEqualTo(adresses);
            assertThat(compter(xml, "<lastmod>")).isEqualTo(adresses);
            assertThat(compter(xml, "<changefreq>")).isEqualTo(adresses);
            assertThat(compter(xml, "<priority>")).isEqualTo(adresses);
        }

        @Test
        @DisplayName("demande une mise en cache de six heures")
        void demandeUneMiseEnCache() {
            // Un moteur ne relit pas ce fichier plusieurs fois par heure. Le
            // recalculer à chaque appel n'offrirait qu'un moyen gratuit de faire
            // travailler la base.
            assertThat(controller.sitemap().getHeaders().getCacheControl())
                    .contains("max-age=21600");
        }
    }

    @Nested
    @DisplayName("Pages déclarées")
    class Pages {

        @Test
        @DisplayName("préfixe chaque adresse par le domaine public")
        void prefixeParLeDomaine() {
            assertThat(plan()).contains("<loc>" + SITE + "/</loc>");
            assertThat(plan()).contains("<loc>" + SITE + "/actus</loc>");
        }

        @Test
        @DisplayName("déclare les documents légaux")
        void declareLesDocumentsLegaux() {
            // Leur présence dans les résultats est un signal de confiance, et
            // l'obligation d'accessibilité « facile, directe et permanente » de
            // l'article XII.6 du Code de droit économique s'accommode mal d'une
            // page que seul un lien de pied de page permettrait d'atteindre.
            assertThat(plan())
                    .contains("/mentions-legales")
                    .contains("/confidentialite")
                    .contains("/cgu")
                    .contains("/cookies");
        }

        @Test
        @DisplayName("n'annonce aucune page de l'espace membre")
        void nAnnoncePasLespaceMembre() {
            // Ces adresses sont interdites d'exploration par robots.txt.
            // Déclarer ici une adresse qu'on interdit par ailleurs est le genre
            // de contradiction qu'un moteur signale.
            assertThat(plan())
                    .doesNotContain("/dashboard")
                    .doesNotContain("/bookings")
                    .doesNotContain("/trips")
                    .doesNotContain("/verify-email")
                    .doesNotContain("/styleguide");
        }
    }

    @Nested
    @DisplayName("Articles")
    class Articles {

        @Test
        @DisplayName("ajoute une entrée par article")
        void uneEntreeParArticle() {
            when(articleService.getAllArticles()).thenReturn(List.of(
                    article("1", "Le télétravail réduit les embouteillages", LocalDate.of(2026, 3, 4)),
                    article("2", "Mobilité douce à Namur", LocalDate.of(2026, 4, 1))));

            String xml = plan();

            assertThat(xml).contains("--1</loc>");
            assertThat(xml).contains("--2</loc>");
        }

        @Test
        @DisplayName("construit un fragment lisible sans accent ni ponctuation")
        void fragmentLisible() {
            // Ce fragment doit reproduire exactement celui que construit le
            // navigateur : une adresse différente de celle des liens produirait
            // deux versions de la même page.
            when(articleService.getAllArticles()).thenReturn(List.of(
                    article("7", "Mobilité douce : à Liège !", LocalDate.of(2026, 3, 4))));

            assertThat(plan()).contains("/actus/mobilite-douce-a-liege--7</loc>");
        }

        @Test
        @DisplayName("reprend la date de l'article")
        void reprendLaDate() {
            when(articleService.getAllArticles()).thenReturn(List.of(
                    article("1", "Un titre", LocalDate.of(2026, 3, 4))));

            assertThat(plan()).contains("<lastmod>2026-03-04</lastmod>");
        }

        @Test
        @DisplayName("retombe sur la date du jour si l'article n'en porte pas")
        void datePardefautSiAbsente() {
            when(articleService.getAllArticles()).thenReturn(List.of(
                    article("1", "Un titre", null)));

            assertThat(plan()).contains("<lastmod>" + LocalDate.now() + "</lastmod>");
        }

        @Test
        @DisplayName("retombe sur « article » quand le titre ne laisse rien d'exploitable")
        void fragmentDeSecours() {
            when(articleService.getAllArticles()).thenReturn(List.of(
                    article("3", "!!! ???", LocalDate.of(2026, 3, 4)),
                    article("4", "   ", LocalDate.of(2026, 3, 4)),
                    article("5", null, LocalDate.of(2026, 3, 4))));

            String xml = plan();

            assertThat(xml).contains("/actus/article--3</loc>");
            assertThat(xml).contains("/actus/article--4</loc>");
            assertThat(xml).contains("/actus/article--5</loc>");
        }

        @Test
        @DisplayName("borne la longueur du fragment sans le laisser finir par un tiret")
        void borneLaLongueur() {
            when(articleService.getAllArticles()).thenReturn(List.of(
                    article("9", "a".repeat(40) + " " + "b".repeat(40), LocalDate.of(2026, 3, 4))));

            String fragment = extraireFragment(plan());

            assertThat(fragment).hasSizeLessThanOrEqualTo(60);
            assertThat(fragment).doesNotEndWith("-");
        }

        @Test
        @DisplayName("échappe les caractères qui casseraient le XML")
        void echappeLeXml() {
            // Une esperluette non échappée suffit à rendre tout le plan
            // illisible — et l'échec est silencieux : le moteur abandonne le
            // fichier sans que l'application ne voie rien.
            when(articleService.getAllArticles()).thenReturn(List.of(
                    article("1", "Titre normal", LocalDate.of(2026, 3, 4))));
            ReflectionTestUtils.setField(controller, "siteUrl", "https://coshift.be?a=1&b=2");

            String xml = plan();

            assertThat(xml).contains("&amp;");
            assertThat(xml).doesNotContain("=1&b=");
        }
    }

    // ────────────────────────────────── Fabriques ───────────────────────────────

    private Article article(String id, String titre, LocalDate date) {
        Article a = new Article();
        a.setId(id);
        a.setTitle(titre);
        a.setDate(date);
        return a;
    }

    private int compter(String texte, String motif) {
        int n = 0, i = 0;
        while ((i = texte.indexOf(motif, i)) >= 0) { n++; i += motif.length(); }
        return n;
    }

    private String extraireFragment(String xml) {
        int debut = xml.indexOf("/actus/") + "/actus/".length();
        int fin = xml.indexOf("--", debut);
        return xml.substring(debut, fin);
    }
}
