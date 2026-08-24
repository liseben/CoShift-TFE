package com.coshift.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Arrays;
import java.util.List;

/**
 * Le texte d'un billet, dans une langue.
 *
 * <h2>Le corps est du texte, jamais du HTML</h2>
 *
 * <p>Les paragraphes sont séparés par une ligne vide et rendus comme du texte.
 * Accepter du HTML dans un éditeur d'administration ouvrirait une injection de
 * script sur une page publique — et la première personne à en profiter serait
 * celle qui compromettrait un compte d'administration.</p>
 */
@Entity
@Table(name = "blog_post_translations",
       uniqueConstraints = @UniqueConstraint(name = "uk_blog_translation",
                                             columnNames = {"post_id", "locale"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogPostTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private BlogPost post;

    /** Code de langue, tel que le catalogue de l'interface les nomme : `fr`, `en`. */
    @Column(nullable = false, length = 5)
    private String locale;

    @Column(nullable = false, length = 200)
    private String title;

    /**
     * Chapeau : la phrase qui donne envie de lire, affichée dans la liste.
     *
     * <p>Le nom de colonne est cité entre accents graves parce que LEAD est un
     * mot réservé depuis MySQL 8 — c'est la fonction de fenêtrage. Sans la
     * citation, Hibernate écrirait {@code insert into … (…, lead, …)} et
     * l'éditeur de blog échouerait en erreur de syntaxe sur tout serveur
     * récent, alors que tout fonctionne sur le 5.7 de développement.</p>
     */
    @Column(name = "`lead`", nullable = false, length = 500)
    private String lead;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String body;

    /**
     * Le corps découpé en paragraphes.
     *
     * <p>Le découpage a lieu ici plutôt que dans l'interface : c'est une
     * propriété du format de stockage, pas une décision d'affichage. Les lignes
     * vides multiples et les espaces de fin sont absorbés, sans quoi un
     * paragraphe vide apparaîtrait à chaque frappe de trop.</p>
     */
    public List<String> paragraphes() {
        if (body == null || body.isBlank()) return List.of();
        return Arrays.stream(body.split("\\R\\s*\\R"))
                .map(String::strip)
                .filter(p -> !p.isEmpty())
                .toList();
    }
}
