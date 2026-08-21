package com.coshift.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "trips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

   
    @Builder.Default
    @Column(unique = true, nullable = false, updatable = false)
    private String uuid = UUID.randomUUID().toString();

    // --- INFOS TRAJET ---

    @NotBlank(message = "{validation.trajet.villeDepart}")
    private String departureCity;

    @Column(name = "departure_address")
    private String departureAddress;

    @NotBlank(message = "{validation.trajet.villeArrivee}")
    private String arrivalCity;

    @Column(name = "arrival_address")
    private String arrivalAddress;

    /**
     * Heure de départ.
     *
     * <h2>Pourquoi il n'y a pas de {@code @Future} ici</h2>
     *
     * <p>Il y en avait un, et il rendait <strong>tout trajet passé
     * immodifiable</strong>. Hibernate rejoue les contraintes de validation à
     * chaque écriture, y compris sur une mise à jour : un trajet parfaitement
     * valide à sa création devenait invalide au fil du temps, et la première
     * tentative de le modifier échouait au moment du commit.</p>
     *
     * <p>Conséquence observée : la tâche de clôture chargeait les trajets dont
     * l'heure était passée, les basculait en {@code COMPLETED}, écrivait
     * « 2 trajets clôturés » au journal — puis la transaction était annulée.
     * Rien n'était clôturé, l'opération recommençait tous les quarts d'heure,
     * et chaque passage déversait une trace d'exception complète. La même
     * cause aurait bloqué l'anonymisation des trajets anciens, qui écrit elle
     * aussi sur des trajets passés par construction.</p>
     *
     * <p>La contrainte reste où elle a un sens : sur
     * {@link com.coshift.api.dto.TripRequest}, qui porte une demande de
     * création. Une entité décrit un fait, une requête exprime une intention —
     * et « le départ doit être à venir » qualifie l'intention, pas le fait.</p>
     */
    @NotNull(message = "{validation.trajet.date}")
    private LocalDateTime departureTime;

    @Min(value = 1, message = "{validation.trajet.places}")
    private int availableSeats;

    @NotNull(message = "{validation.trajet.prixRequis}")
    @Min(value = 0, message = "{validation.trajet.prixNegatif}")
    private BigDecimal pricePerSeat;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Préférences conducteur
    @Builder.Default @Column(name = "accepts_luggage") private boolean acceptsLuggage = true;
    @Builder.Default @Column(name = "accepts_pets")    private boolean acceptsPets    = false;
    @Builder.Default @Column(name = "music_allowed")   private boolean musicAllowed   = true;
    @Builder.Default @Column(name = "talking_allowed") private boolean talkingAllowed = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TripStatus status = TripStatus.PLANNED;

    // --- RELATIONS CLÉS ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicule_id", nullable = false)
    private Vehicule vehicule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    // --- AUDIT ---

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}