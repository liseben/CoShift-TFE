package com.coshift.api.controller.seo;

import com.coshift.api.entity.Article;
import com.coshift.api.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Plan du site, engendré depuis la base.
 *
 * <h2>Pourquoi le serveur et non un fichier statique</h2>
 *
 * <p>Les pages de présentation tiennent sur les doigts d'une main et pourraient
 * figurer dans un fichier écrit à la main. Les <strong>articles</strong>, eux,
 * sont alimentés en continu par deux agrégateurs : un fichier statique serait
 * périmé le lendemain de sa rédaction, et personne ne penserait à le
 * régénérer.</p>
 *
 * <h2>Une précaution sur les adresses</h2>
 *
 * <p>Les adresses listées ici sont celles du <em>site</em>, pas celles de
 * l'API : un plan de site ne peut déclarer que des pages du domaine qui le
 * sert. En production, le serveur web fait donc suivre
 * {@code coshift.be/sitemap.xml} vers ce point d'entrée. C'est aussi pourquoi
 * le domaine se lit dans une propriété plutôt que d'être écrit en dur.</p>
 */
@RestController
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "Référencement", description = "Plan du site, destiné aux moteurs de recherche.")
public class SitemapController {

    private final ArticleService articleService;

    @Value("${app.public-base-url}")
    private String siteUrl;

    /** Pages fixes : chemin, priorité, fréquence de changement annoncée. */
    private static final List<String[]> PAGES = List.of(
            new String[] { "/",           "1.0", "daily" },
            new String[] { "/actus",      "0.9", "daily" },
            new String[] { "/a-propos",   "0.7", "monthly" },
            new String[] { "/entreprises","0.8", "monthly" },
            new String[] { "/blog",       "0.6", "weekly" },
            new String[] { "/register",   "0.5", "yearly" });

    @Operation(
            summary = "Plan du site",
            description = """
                    Renvoie le plan du site au format XML : les pages de présentation, puis
                    tous les articles de la rubrique Actus.

                    Les pages de l'espace membre en sont **volontairement absentes** :
                    interdites d'exploration par `robots.txt`, elles n'auraient rien à faire
                    ici. Déclarer une adresse qu'on interdit par ailleurs est le genre de
                    contradiction qu'un moteur signale.""")
    @ApiResponse(responseCode = "200", description = "Plan du site au format XML.")
    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        StringBuilder xml = new StringBuilder(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        String aujourdhui = LocalDate.now().toString();
        for (String[] page : PAGES) {
            xml.append(entree(siteUrl + page[0], aujourdhui, page[2], page[1]));
        }

        for (Article a : articleService.getAllArticles()) {
            xml.append(entree(
                    siteUrl + "/actus/" + fragmentLisible(a.getTitle()) + "--" + a.getId(),
                    (a.getDate() != null) ? a.getDate().toString() : aujourdhui,
                    "monthly",
                    "0.6"));
        }

        xml.append("</urlset>\n");

        return ResponseEntity.ok()
                // Un moteur ne relit pas ce fichier plusieurs fois par heure ; le
                // recalculer à chaque appel n'aurait pour effet que d'offrir un moyen
                // gratuit de faire travailler la base.
                .cacheControl(CacheControl.maxAge(Duration.ofHours(6)).cachePublic())
                .body(xml.toString());
    }

    private String entree(String url, String derniereModification, String frequence, String priorite) {
        return "  <url>\n"
                + "    <loc>" + echapper(url) + "</loc>\n"
                + "    <lastmod>" + derniereModification + "</lastmod>\n"
                + "    <changefreq>" + frequence + "</changefreq>\n"
                + "    <priority>" + priorite + "</priority>\n"
                + "  </url>\n";
    }

    /**
     * Reproduit le fragment lisible construit côté navigateur.
     *
     * <p>Les deux doivent donner le même résultat : une adresse annoncée au plan
     * du site mais différente de celle des liens produirait deux versions de la
     * même page, donc du contenu dupliqué.</p>
     */
    private String fragmentLisible(String titre) {
        if (titre == null || titre.isBlank()) return "article";
        String sansAccent = Normalizer.normalize(titre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String fragment = sansAccent.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (fragment.length() > 60) fragment = fragment.substring(0, 60).replaceAll("-+$", "");
        return fragment.isBlank() ? "article" : fragment;
    }

    /** Une esperluette non échappée suffit à rendre tout le plan illisible. */
    private String echapper(String v) {
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
