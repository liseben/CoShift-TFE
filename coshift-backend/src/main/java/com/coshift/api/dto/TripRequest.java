package com.coshift.api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TripRequest {

    @NotBlank(message = "La ville de départ est obligatoire")
    private String departureCity;

    private String departureAddress;

    @NotBlank(message = "La ville d'arrivée est obligatoire")
    private String arrivalCity;

    private String arrivalAddress;

    @NotNull(message = "La date et l'heure de départ sont obligatoires")
    @Future(message = "Le trajet doit être dans le futur")
    private LocalDateTime departureTime;

    @Min(value = 1, message = "Il faut proposer au moins 1 place")
    private int availableSeats;

    @NotNull(message = "Le prix par place est obligatoire")
    @DecimalMin(value = "0.0", message = "Le prix ne peut pas être négatif")
    private BigDecimal pricePerSeat;

    private String description;

    // UUID du véhicule à utiliser (doit appartenir au conducteur)
    @NotBlank(message = "Vous devez sélectionner un véhicule")
    private String vehiculeUuid;

    // Préférences
    private boolean acceptsLuggage = true;
    private boolean acceptsPets    = false;
    private boolean musicAllowed   = true;
    private boolean talkingAllowed = true;
}
