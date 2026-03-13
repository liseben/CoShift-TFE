package com.coshift.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "articles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Article {

    @Id
    private String id; // On utilisera l'URL ou un hash comme ID unique

    private String category;
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String summary;
    
    private String source;
    
    @Column(name = "published_date")
    private String date; // Formatée en String pour React (ex: "12 mars 2026")
    
    @Column(name = "image_url")
    private String imageUrl;
    
    @Column(unique = true)
    private String url;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}