package com.coshift.api.repository;

import com.coshift.api.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, String> {
    
    // On veut renvoyer les articles au Front-End triés par date de création dans la DB
    List<Article> findAllByOrderByCreatedAtDesc();
    
    // Pour vérifier rapidement si un article existe déjà avant de l'insérer
    boolean existsByUrl(String url);
}