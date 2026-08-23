package com.coshift.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Avis laissé par un participant sur l'autre, à l'issue d'un trajet partagé
 * (F22, F31).
 *
 * <p>Le covoiturage repose sur le fait de monter en voiture avec un inconnu.
 * Le seul substitut au lien social est la réputation accumulée : cette entité
 * en est le support, et non l'ornement d'un profil.</p>
 *
 * <h2>Rattaché à une réservation, pas à une paire de personnes</h2>
 *
 * <p>Une réservation est la preuve qu'un trajet a été partagé. S'y adosser
 * garantit qu'on ne note que ce qu'on a vécu, et rend naturelle la règle
 * « un trajet, un avis par participant » — portée par une contrainte d'unicité
 * en base.</p>
 */
@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(unique = true, nullable = false, updatable = false)
    private String uuid = UUID.randomUUID().toString();

    /** Le trajet partagé qui fonde l'avis. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /** Qui écrit. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /**
     * Sur qui.
     *
     * <p>Déductible de la réservation, mais explicite ici : sans cette colonne,
     * calculer la moyenne d'une personne imposerait de déterminer à chaque
     * ligne de quel côté du trajet se plaçait l'auteur.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id", nullable = false)
    private User target;

    /**
     * De 1 à 5.
     *
     * <p>La colonne est un {@code TINYINT} : une note tient sur un octet, et
     * V8 l'a écrite ainsi à dessein. Le champ Java reste un {@code int}, qui
     * est le type naturel d'un barème en Java et celui qu'attendent les
     * annotations {@code @Min}/{@code @Max} du DTO.</p>
     *
     * <p>Sans cette annotation, Hibernate déduit {@code INTEGER} du type Java
     * et {@code ddl-auto=validate} refuse de démarrer : « wrong column type
     * encountered in column [rating] ». Le défaut ne se voyait ni à la
     * relecture ni aux tests unitaires — il n'apparaissait qu'au démarrage
     * d'un contexte Spring complet contre la vraie base.</p>
     *
     * <p>La contrainte {@code CHECK} posée par V8 sur le barème est, elle,
     * analysée puis ignorée par MySQL 5.7 : ce sont les annotations du DTO qui
     * protègent réellement.</p>
     */
    @Column(nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.TINYINT)
    private int rating;

    /**
     * Commentaire libre, facultatif.
     *
     * <p>C'est la donnée la plus sensible de la table : un texte rédigé par une
     * personne au sujet d'une autre. Il est effacé lorsque son auteur exerce
     * son droit à l'effacement ; la note chiffrée subsiste, détachée de tout
     * nom, et ne se rapporte alors plus à personne.</p>
     */
    @Column(length = 500)
    private String comment;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
