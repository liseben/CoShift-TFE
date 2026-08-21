package com.coshift.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** F6 — Demande d'un code de réinitialisation du mot de passe. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ForgotPasswordRequest {

    @NotBlank(message = "{validation.email.requis}")
    @Email(message = "{validation.email.format}")
    private String email;
}
