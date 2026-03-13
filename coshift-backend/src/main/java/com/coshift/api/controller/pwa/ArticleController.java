package com.coshift.api.controller.pwa;

import com.coshift.api.entity.Article;
import com.coshift.api.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pwa/articles") // J'ai adapté la route pour refléter l'architecture
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Permet à ton React (localhost:5173) de faire ses requêtes
public class ArticleController {

    private final ArticleRepository articleRepository;

    @GetMapping
    public ResponseEntity<List<Article>> getArticles() {
        // Renvoie tous les articles triés du plus récent ajouté au plus ancien
        List<Article> articles = articleRepository.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(articles);
    }
}