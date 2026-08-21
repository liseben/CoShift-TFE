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

    @NotBlank(message = "{validation.reservation.trajet}")
    private String tripUuid;

    @Min(value = 1, message = "{validation.reservation.placesMin}")
    @Max(value = 8, message = "{validation.reservation.placesMax}")
    private int seatsBooked = 1;
}
