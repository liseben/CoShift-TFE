package com.coshift.api.repository;

import com.coshift.api.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, String> {

    List<Article> findAllByOrderByDateDesc();

    boolean existsByUrl(String url);

    boolean existsByNormalizedTitle(String normalizedTitle);

    /**
     * Adresses et titres normalisés de tous les articles, sans les entités.
     *
     * <p>La détection de doublons appelait {@code findAll()} pour chaque article
     * candidat : chaque passage d'aspiration rechargeait donc la table entière
     * plus de cent fois, entités complètes comprises — résumé de mille
     * caractères inclus — pour ne comparer que des titres.</p>
     *
     * <p>Ces deux projections chargent le strict nécessaire, une fois par
     * passage. Le coût passe de cent lectures complètes à deux lectures de deux
     * colonnes.</p>
     */
    @Query("SELECT a.url FROM Article a")
    List<String> findAllUrls();

    @Query("SELECT a.normalizedTitle FROM Article a")
    List<String> findAllNormalizedTitles();
}