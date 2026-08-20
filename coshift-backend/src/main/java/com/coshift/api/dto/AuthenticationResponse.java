package com.coshift.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
// Les champs non renseignés sont omis plutôt que sérialisés à null : une
// connexion réussie n'a pas à porter un « emailVerified: null » qui n'informe
// de rien et que chaque client devrait apprendre à ignorer.
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Réponse commune aux opérations d'authentification et de profil.")
public class AuthenticationResponse {

    @Schema(description = "Jeton JWT, à placer dans l'en-tête Authorization. Absent tant que l'adresse n'est pas vérifiée.",
            example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJtYXJjQGNvc2hpZnQuYmUifQ...")
    private String token;

    @Schema(description = "Message destiné à être affiché tel quel à l'utilisateur.",
            example = "Connexion réussie")
    private String message;

    /**
     * Renseigné uniquement là où l'état de vérification vient de changer, donc
     * après une modification d'adresse. {@code null} partout ailleurs : le
     * champ signale un événement, il ne décrit pas en permanence le compte.
     */
    @Schema(description = """
            Faux lorsque l'opération vient de retirer la vérification du compte — cas
            d'un changement d'adresse. Le client doit alors conduire l'utilisateur vers
            la saisie du code. Absent lorsque la question ne se pose pas.""",
            example = "false")
    private Boolean emailVerified;
}
