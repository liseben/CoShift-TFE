package com.coshift.api.controller.pwa;

import com.coshift.api.entity.Article;
import com.coshift.api.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pwa/articles")
@RequiredArgsConstructor
public class ArticleController {

    // On passe par le Service, c'est lui le patron de la logique métier !
    private final ArticleService articleService;

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
    @GetMapping("/{id}")
    public ResponseEntity<Article> getArticle(@PathVariable String id) {
        return ResponseEntity.ok(articleService.getArticleById(id));
    }
}
