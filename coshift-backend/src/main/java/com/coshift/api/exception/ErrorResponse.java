package com.coshift.api.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Forme unique de toutes les réponses d'erreur de l'API.
 *
 * <p>Un client n'a qu'un seul format à savoir lire, quel que soit le code
 * renvoyé : le champ {@code message} est rédigé pour être affiché tel quel à
 * l'utilisateur, en français.</p>
 */
@Schema(name = "ErrorResponse", description = "Forme commune à toutes les réponses d'erreur.")
public record ErrorResponse(

        @Schema(description = "Instant de l'erreur, côté serveur.", example = "2026-08-20T06:50:53.411")
        LocalDateTime timestamp,

        @Schema(description = "Code HTTP, repris ici pour les clients qui ne lisent que le corps.", example = "404")
        int status,

        @Schema(description = "Libellé technique de la famille d'erreur.", example = "Not Found")
        String error,

        @Schema(description = "Message destiné à être affiché tel quel à l'utilisateur.",
                example = "Trajet introuvable.")
        String message,

        @Schema(description = "Chemin de la requête fautive.", example = "/api/trips/inexistant")
        String path
) {}
