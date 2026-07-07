package com.coshift.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
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

    @NotBlank(message = "Ville de départ obligatoire")
    private String departureCity;

    @Column(name = "departure_address")
    private String departureAddress;

    @NotBlank(message = "Ville d'arrivée obligatoire")
    private String arrivalCity;

    @Column(name = "arrival_address")
    private String arrivalAddress;

    @NotNull(message = "Date de départ obligatoire")
    @Future(message = "Le trajet doit être dans le futur")
    private LocalDateTime departureTime;

    @Min(value = 1, message = "Il faut au moins 1 place")
    private int availableSeats;

    @NotNull(message = "Le prix est obligatoire")
    @Min(value = 0, message = "Le prix ne peut pas être négatif")
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