package com.coshift.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Motif d'une suspension de compte.
 *
 * <p>Un corps de requête pour un seul champ, plutôt qu'un paramètre d'URL : un
 * motif de modération n'a rien à faire dans une adresse, où il finirait dans
 * les journaux d'accès du serveur et dans l'historique du navigateur.</p>
 */
@Data
@Schema(name = "DemandeSuspension")
public class SuspensionRequest {

    /**
     * Pourquoi le compte est suspendu.
     *
     * <p>Obligatoire. Une décision sans raison écrite ne peut plus être
     * expliquée trois mois plus tard, ni à la personne concernée, ni à un
     * juge.</p>
     */
    @NotBlank(message = "{validation.admin.motif}")
    @Size(max = 255, message = "{validation.admin.motifLong}")
    @Schema(description = "Motif de la suspension, conservé et affiché à l'administrateur.",
            example = "Annonces répétées hors du cadre professionnel, après avertissement.")
    private String motif;
}
