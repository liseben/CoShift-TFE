package com.coshift.api.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Motif accompagnant un changement de statut : refus par le conducteur (F20)
 * ou annulation par le passager (F29).
 */
@Data
public class BookingDecisionRequest {

    @Size(max = 500, message = "Le motif ne peut pas dépasser 500 caractères")
    private String reason;
}
