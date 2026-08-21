package com.coshift.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Confirmation de la suppression d'un compte.
 *
 * <p>L'adresse est retapée par la personne et comparée à celle du compte déduit
 * du jeton. L'opération étant irréversible, un appel qui n'exigerait rien de
 * plus que le jeton transformerait une requête malencontreuse — un clic de
 * trop, une page rechargée, une requête rejouée — en perte définitive.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeleteAccountRequest {

    @NotBlank(message = "{validation.confirmation.requise}")
    @Schema(description = "Adresse électronique du compte, retapée à l'identique.",
            example = "marc@coshift.be")
    private String confirmationEmail;
}
