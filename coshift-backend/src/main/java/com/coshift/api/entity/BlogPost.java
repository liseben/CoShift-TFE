package com.coshift.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Un billet du blog.
 *
 * <h2>Ce que porte le billet, et ce que portent ses traductions</h2>
 *
 * <p>Le billet existe indépendamment de la langue : il a une date, une
 * rubrique, un auteur, un fragment d'URL. Ce sont ses {@link BlogPostTranslation}
 * qui portent le texte. Écrire {@code titreFr}, {@code titreEn}, {@code chapeauFr}
 * dans une seule entité obligerait à une migration de schéma le jour où le
 * néerlandais arrive, et laisserait des colonnes vides pour chaque langue
 * absente.</p>
 *
 * <h2>Un billet peut n'exister que dans une langue</h2>
 *
 * <p>C'est assumé : mieux vaut un billet lisible en français qu'un billet
 * retenu jusqu'à sa traduction. La lecture se rabat alors sur la langue
 * disponible, et le fait qu'il n'y en ait qu'une se voit à l'écran plutôt que
 * de produire une page vide.</p>
 */
@Entity
@Table(name = "blog_posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(unique = true, nullable = false, updatable = false)
    private String uuid = UUID.randomUUID().toString();

    /**
     * Fragment d'URL.
     *
     * <p>Stable et unique : il est indexé par les moteurs et partagé par les
     * lecteurs. Le modifier après publication casse chaque lien déjà en
     * circulation — le service le refuse pour cette raison.</p>
     */
    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BlogCategory category;

    /**
     * Instant de publication, ou {@code null} tant que le billet est un
     * brouillon.
     *
     * <p>Une date plutôt qu'un booléen {@code publie} : elle répond à
     * « depuis quand », qui est ce que le lecteur voit et ce sur quoi la liste
     * est triée. Un drapeau aurait demandé une seconde colonne pour la même
     * information.</p>
     */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /** Durée de lecture annoncée, en minutes. */
    @Builder.Default
    @Column(name = "reading_minutes", nullable = false)
    private int readingMinutes = 3;

    /**
     * Auteur, ou {@code null}.
     *
     * <p>Nul pour les quatre billets antérieurs à l'éditeur, et nul aussi le
     * jour où un auteur fait valoir son droit à l'effacement : la relation ne
     * peut donc pas être {@code NOT NULL}. Un billet sans auteur reste un
     * billet ; c'est la signature qui manque, pas le texte.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<BlogPostTranslation> translations = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** Vrai si le billet est visible du public. */
    public boolean estPublie() {
        return publishedAt != null;
    }

    /**
     * Traduction dans la langue demandée, à défaut n'importe laquelle.
     *
     * <p>Le repli n'est pas « le français » mais « la première disponible » :
     * le jour où un billet est rédigé d'abord en néerlandais, imposer le
     * français produirait une page vide alors qu'un texte existe.</p>
     */
    public Optional<BlogPostTranslation> traduction(String locale) {
        if (translations == null || translations.isEmpty()) return Optional.empty();
        return translations.stream()
                .filter(t -> t.getLocale().equalsIgnoreCase(locale))
                .findFirst()
                .or(() -> translations.stream().findFirst());
    }
}
