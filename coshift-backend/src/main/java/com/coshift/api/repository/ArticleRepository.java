package com.coshift.api.repository;

import com.coshift.api.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, String> {
    
    // CORRECTION : On trie par date de publication (le champ 'date' YYYY-MM-DD), exactement comme React
    List<Article> findAllByOrderByDateDesc();
    
    boolean existsByUrl(String url);

    boolean existsByTitleIgnoreCase(String title);
}