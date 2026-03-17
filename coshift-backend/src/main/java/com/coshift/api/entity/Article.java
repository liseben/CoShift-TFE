package com.coshift.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "articles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Article {

    @Id
    private String id;

    private String category;

    @Column(length = 500)
    private String title;

    // Titre normalisé pour la déduplication
    @Column(name = "normalized_title", length = 500)
    private String normalizedTitle;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private String source;

    // Nouvelle gestion propre de la date
    private LocalDate date;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(length = 1000, unique = true)
    private String url;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}