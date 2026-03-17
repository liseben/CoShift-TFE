package com.coshift.api.controller.pwa;

import com.coshift.api.entity.Article;
import com.coshift.api.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pwa/articles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Permet à React de lire les données
public class ArticleController {

    // On passe par le Service, c'est lui le patron de la logique métier !
    private final ArticleService articleService;

    @GetMapping
    public ResponseEntity<List<Article>> getArticles() {
        return ResponseEntity.ok(articleService.getAllArticles());
    }
}