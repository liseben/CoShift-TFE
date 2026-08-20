package com.coshift.api.controller.pwa;

import com.coshift.api.entity.Article;
import com.coshift.api.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Flux d'actualités sur la mobilité. Seule partie de l'API lisible sans jeton,
 * d'où le {@code @SecurityRequirements} vide qui lève l'exigence globale.
 */
@RestController
@RequestMapping("/api/pwa/articles")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "Actualités",
     description = "Articles sur la mobilité, agrégés depuis des sources externes. Lecture publique, sans jeton.")
public class ArticleController {

    // On passe par le Service, c'est lui le patron de la logique métier !
    private final ArticleService articleService;

    @Operation(
            summary = "Lister les articles",
            description = "Les articles disponibles, du plus récent au plus ancien.")
    @ApiResponse(responseCode = "200", description = "Liste des articles.")
    @GetMapping
    public ResponseEntity<List<Article>> getArticles() {
        return ResponseEntity.ok(articleService.getAllArticles());
    }

    /**
     * Un article isolé, pour la page de détail.
     *
     * Sans cette route, le front devait charger la liste entière puis y
     * chercher l'article — coûteux, et impossible à mettre en cache ou à
     * partager par URL de façon fiable.
     */
    @Operation(
            summary = "Consulter un article",
            description = """
                    Un article isolé, pour la page de détail.

                    Sans cette route, le client devait charger la liste entière puis y
                    chercher l'article : coûteux, et impossible à mettre en cache ou à
                    partager par URL de façon fiable.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Article demandé."),
            @ApiResponse(responseCode = "404", description = "Aucun article pour cet identifiant.", content = @Content())
    })
    @GetMapping("/{id}")
    public ResponseEntity<Article> getArticle(
            @Parameter(description = "Identifiant de l'article.")
            @PathVariable String id) {
        return ResponseEntity.ok(articleService.getArticleById(id));
    }
}
