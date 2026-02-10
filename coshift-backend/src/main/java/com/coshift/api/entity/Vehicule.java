package com.coshift.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "La marque est obligatoire")
    private String brand; // ex: Tesla, Renault

    @NotBlank(message = "Le modèle est obligatoire")
    private String model; // ex: Model 3, Clio

    @NotBlank(message = "L'immatriculation est obligatoire")
    @Column(unique = true) // Une plaque est unique !
    private String licensePlate;

    @Min(value = 1, message = "Il faut au moins 1 place")
    private int seats; // Nombre de places TOTALES (conducteur compris)

    @Enumerated(EnumType.STRING)
    private EnergyType energy; // Électrique, Diesel, etc. (Pour le calcul CO2 !)

    // Relation : Un véhicule appartient à un User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}