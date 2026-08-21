package com.coshift.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- CORRECTION : Ajout de @Builder.Default ---
    @Builder.Default
    @Column(unique = true, nullable = false, updatable = false)
    private String uuid = UUID.randomUUID().toString();

    @NotBlank(message = "{validation.email.requis}")
    @Email(message = "{validation.email.format}")
    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String firstname;
    private String lastname;
    
    @Column(name = "picture_url")
    private String pictureUrl;

    @Column(name = "phone_number")
    private String phoneNumber;

    // F7 : Validation email
    @Builder.Default
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "verification_code", length = 6)
    private String verificationCode;

    @Column(name = "verification_code_expiry")
    private LocalDateTime verificationCodeExpiry;

    // F6 : réinitialisation du mot de passe. Codes séparés de ceux de la
    // vérification d'email : les deux demandes peuvent coexister sur un même
    // compte, et l'un ne doit jamais pouvoir servir à la place de l'autre.
    @Column(name = "password_reset_code", length = 6)
    private String passwordResetCode;

    @Column(name = "password_reset_expiry")
    private LocalDateTime passwordResetExpiry;

    // Statistiques profil (F8)
    @Builder.Default
    @Column(name = "average_rating", nullable = false)
    private double averageRating = 0.0;

    @Builder.Default
    @Column(name = "trips_count", nullable = false)
    private int tripsCount = 0;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "organization_members", // Table de liaison
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "organization_id")
    )
    @Builder.Default // Tu l'avais bien mis ici, c'est parfait !
    private Set<Organization> organizations = new HashSet<>();

    // --- RÔLES & SÉCURITÉ ---
    
    @Enumerated(EnumType.STRING)
    private Role role; 

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // --- DROITS DES PERSONNES (RGPD) ---

    /**
     * Date de l'effacement, ou {@code null} pour un compte vivant.
     *
     * <p>La ligne n'est pas supprimée : {@code trips.driver_id} et
     * {@code bookings.passenger_id} sont obligatoires, et un covoiturage passé
     * engage deux personnes. L'effacement écrase donc les champs identifiants
     * sur place, et cette date marque l'opération.</p>
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** Date de l'acceptation des conditions générales — preuve de l'accord. */
    @Column(name = "cgu_accepted_at")
    private LocalDateTime cguAcceptedAt;

    /** Version des conditions acceptées : sans elle, la date ne dit rien. */
    @Column(name = "cgu_version", length = 10)
    private String cguVersion;

    // --- MÉTHODES USERDETAILS (Spring Security) ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Si le rôle est null, on donne un rôle par défaut pour éviter le crash
        if (role == null) return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() { return deletedAt == null; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }

    /**
     * Un compte effacé ne se connecte plus.
     *
     * <p>L'anonymisation remplace déjà l'empreinte du mot de passe par une
     * valeur aléatoire, ce qui rend l'authentification impossible en pratique.
     * Le contrôle explicite ne s'y substitue pas : il garantit que le refus ne
     * dépend pas d'un effet de bord d'une autre opération.</p>
     */
    @Override
    public boolean isEnabled() { return emailVerified && deletedAt == null; }
}