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