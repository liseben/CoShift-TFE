package com.coshift.api.controller.opendata;

import com.coshift.api.dto.OpenDataResponse;
import com.coshift.api.service.OpenDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Données ouvertes de CoShift.
 *
 * <p>Publier ces chiffres n'est pas un ornement : une plateforme de covoiturage
 * produit une mesure de report modal que personne d'autre ne détient, et qui
 * intéresse les collectivités comme les employeurs. Les garder pour soi
 * reviendrait à demander aux organisations de croire l'argument écologique sur
 * parole.</p>
 *
 * <p>Aucune authentification : c'est la condition d'une donnée réellement
 * ouverte. L'exigence de jeton posée globalement est donc levée ici par
 * {@code @SecurityRequirements}.</p>
 */
@RestController
@RequestMapping("/api/open-data")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "Données ouvertes",
     description = """
             Statistiques agrégées de mobilité, publiques et réutilisables sous Licence
             Ouverte 2.0. Aucune donnée personnelle : ni identité, ni adresse, ni horaire.""")
public class OpenDataController {

    private final OpenDataService openDataService;

    @Operation(
            summary = "Consulter le jeu de données complet",
            description = """
                    Volumes, taux de remplissage, répartition mensuelle et liaisons entre
                    villes, accompagnés de leur licence et des règles d'anonymisation
                    appliquées.

                    **Fraîcheur.** Le jeu est recalculé au plus une fois toutes les quinze
                    minutes ; les champs `genereLe` et `valableJusqua` donnent la fenêtre
                    exacte. Un en-tête `Cache-Control` de même durée accompagne la réponse,
                    pour qu'un client bien élevé n'ait pas à redemander.

                    **Ce que le jeu ne contient pas.** Aucune identité, aucune adresse
                    précise, aucun horaire. Une liaison empruntée moins de cinq fois est
                    écartée, et le nombre de liaisons ainsi retirées est publié : sans cette
                    mention, le lecteur croirait le jeu exhaustif.""")
    @ApiResponse(responseCode = "200", description = "Jeu de données complet.")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OpenDataResponse> jeuDeDonnees() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(OpenDataService.CACHE_MINUTES)).cachePublic())
                .body(openDataService.jeuDeDonnees());
    }

    @Operation(
            summary = "Télécharger les villes desservies au format CSV",
            description = """
                    Les mêmes villes que dans le jeu complet, au format CSV (RFC 4180,
                    encodage UTF-8, séparateur virgule).

                    Colonnes : `ville`, `trajets_au_depart`, `trajets_a_l_arrivee`,
                    `places_partagees`, `prix_moyen_au_depart_eur`.

                    Le CSV n'est pas un doublon du JSON : c'est le format qu'ouvre un
                    tableur, donc celui par lequel la donnée est réutilisée par quelqu'un
                    qui n'écrit pas de code.""")
    @ApiResponse(responseCode = "200", description = "Fichier CSV des villes desservies.")
    @GetMapping(value = "/villes.csv", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<String> villesCsv() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(OpenDataService.CACHE_MINUTES)).cachePublic())
                .header("Content-Disposition", "attachment; filename=\"coshift-villes.csv\"")
                .body(openDataService.villesEnCsv());
    }
}
