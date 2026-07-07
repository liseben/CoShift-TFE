package com.coshift.api.dto;

import com.coshift.api.entity.EnergyType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VehiculeRequest {

    @NotBlank(message = "La marque est obligatoire")
    private String brand;

    @NotBlank(message = "Le modèle est obligatoire")
    private String model;

    @NotBlank(message = "L'immatriculation est obligatoire")
    private String licensePlate;

    @Min(value = 2, message = "Il faut au moins 2 places (conducteur + 1 passager)")
    private int seats;

    @NotNull(message = "Le type de carburant est obligatoire")
    private EnergyType energy;

    private String photoUrl;
}
