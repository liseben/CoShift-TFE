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
@RequestMapping("/api/pwa/articles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ArticleController {

    private final ArticleRepository articleRepository;

    @GetMapping
    public ResponseEntity<List<Article>> getArticles() {
        // CORRECTION : Appel de la nouvelle méthode de tri
        List<Article> articles = articleRepository.findAllByOrderByDateDesc();
        return ResponseEntity.ok(articles);
    }
}