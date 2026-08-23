package com.coshift.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Avis déposé à l'issue d'un trajet partagé (F22, F31). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {

    /**
     * Note de 1 à 5.
     *
     * <p>Le barème est également contraint en base. La validation protège la
     * personne d'une faute de saisie ; la contrainte protège la table d'un
     * défaut du code. Une note hors barème fausserait toutes les moyennes.</p>
     */
    @Min(value = 1, message = "{validation.note.bareme}")
    @Max(value = 5, message = "{validation.note.bareme}")
    private int rating;

    /**
     * Commentaire facultatif.
     *
     * <p>Volontairement borné : au-delà, ce n'est plus un avis sur un trajet.
     * La limite suit celle de la colonne, pour qu'un texte trop long soit
     * refusé avec un message clair plutôt que tronqué en silence ou rejeté par
     * la base.</p>
     */
    @Size(max = 500, message = "{validation.commentaire.longueur}")
    private String comment;
}
