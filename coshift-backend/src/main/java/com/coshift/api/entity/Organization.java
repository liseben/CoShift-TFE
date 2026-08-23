package com.coshift.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Builder.Default
    @Column(unique = true, nullable = false, updatable = false)
    private String uuid = UUID.randomUUID().toString();

    @NotBlank(message = "{validation.organisation.nom}")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "{validation.organisation.slug}")
    private String slug; 

    /**
     * Domaine de l'adresse professionnelle des membres, par exemple
     * {@code solvantis.be}.
     *
     * <p>C'est la clé du rattachement : quelqu'un qui s'inscrit depuis
     * {@code prenom.nom@solvantis.be} rejoint Solvantis sans que personne
     * n'ait à l'inviter. C'est aussi ce que promet la page d'accueil.</p>
     *
     * <p>Distinct du slug, qui est un fragment d'URL. Les faire coïncider
     * marcherait sur le jeu de démonstration, écrit ainsi, et cesserait de
     * marcher au premier client dont le domaine ne ressemble pas à son nom.</p>
     *
     * <p>Unique, parce que deux organisations qui revendiqueraient la même
     * adresse rendraient le rattachement ambigu — et le cercle de visibilité
     * fuirait de l'une vers l'autre. Nullable : une organisation peut exister
     * avant que son domaine soit connu.</p>
     */
    @Column(name = "email_domain", unique = true)
    private String emailDomain;

    private String logoUrl;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;


    @Builder.Default 
    @ManyToMany(mappedBy = "organizations")
    private Set<User> users = new HashSet<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}