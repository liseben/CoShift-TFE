package com.coshift.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** F6 — Choix du nouveau mot de passe, muni du code reçu par courriel. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordRequest {

    @NotBlank(message = "{validation.email.requis}")
    @Email(message = "{validation.email.format}")
    private String email;

    @NotBlank(message = "{validation.code.reset}")
    private String code;

    // Même règle qu'à l'inscription : une exigence plus faible ici ouvrirait un
    // contournement, la réinitialisation devenant le chemin le moins protégé.
    @NotBlank(message = "{validation.motdepasse.requis}")
    @Size(min = 6, message = "{validation.motdepasse.longueur}")
    private String newPassword;
}
