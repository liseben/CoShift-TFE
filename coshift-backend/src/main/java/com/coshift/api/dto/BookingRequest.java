package com.coshift.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * F27 — Demande de réservation de places sur un trajet.
 */
@Data
public class BookingRequest {

    @NotBlank(message = "Le trajet à réserver est obligatoire")
    private String tripUuid;

    @Min(value = 1, message = "Il faut réserver au moins 1 place")
    @Max(value = 8, message = "Vous ne pouvez pas réserver plus de 8 places")
    private int seatsBooked = 1;
}
