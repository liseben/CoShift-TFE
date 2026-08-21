package com.coshift.api.dto;

import com.coshift.api.entity.EnergyType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VehiculeRequest {

    @NotBlank(message = "{validation.vehicule.marque}")
    private String brand;

    @NotBlank(message = "{validation.vehicule.modele}")
    private String model;

    @NotBlank(message = "{validation.vehicule.plaque}")
    private String licensePlate;

    @Min(value = 2, message = "{validation.vehicule.places}")
    private int seats;

    @NotNull(message = "{validation.vehicule.energie}")
    private EnergyType energy;

    private String photoUrl;
}
