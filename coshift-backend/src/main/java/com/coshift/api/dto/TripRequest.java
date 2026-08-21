package com.coshift.api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TripRequest {

    @NotBlank(message = "{validation.trajet.villeDepart}")
    private String departureCity;

    private String departureAddress;

    @NotBlank(message = "{validation.trajet.villeArrivee}")
    private String arrivalCity;

    private String arrivalAddress;

    @NotNull(message = "{validation.trajet.date}")
    @Future(message = "{validation.trajet.futur}")
    private LocalDateTime departureTime;

    @Min(value = 1, message = "{validation.trajet.places}")
    private int availableSeats;

    @NotNull(message = "{validation.trajet.prixRequis}")
    @DecimalMin(value = "0.0", message = "{validation.trajet.prixNegatif}")
    private BigDecimal pricePerSeat;

    private String description;

    // UUID du véhicule à utiliser (doit appartenir au conducteur)
    @NotBlank(message = "{validation.trajet.vehicule}")
    private String vehiculeUuid;

    // Préférences
    private boolean acceptsLuggage = true;
    private boolean acceptsPets    = false;
    private boolean musicAllowed   = true;
    private boolean talkingAllowed = true;
}
