package com.coshift.api.dto;

import com.coshift.api.entity.BlogCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

/**
 * Un billet tel qu'un administrateur le soumet.
 *
 * <p>Le slug n'est pas déduit du titre : il est saisi. Une déduction
 * automatique le ferait changer à la moindre correction de titre, et chaque
 * lien déjà partagé tomberait. Ici il est écrit une fois, puis figé par le
 * service dès la publication.</p>
 */
@Data
@Schema(name = "DemandeBillet")
public class BlogPostRequest {

    @NotBlank(message = "{validation.blog.slug}")
    @Size(max = 120, message = "{validation.blog.slugLong}")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "{validation.blog.slugFormat}")
    @Schema(description = "Minuscules, chiffres et tirets. Apparaît dans l'adresse du billet.",
            example = "confirmer-un-trajet")
    private String slug;

    @NotNull(message = "{validation.blog.rubrique}")
    @Schema(example = "CONCEPTION")
    private BlogCategory category;

    @Min(value = 1, message = "{validation.blog.lecture}")
    @Max(value = 60, message = "{validation.blog.lecture}")
    @Schema(description = "Durée de lecture annoncée, en minutes.", example = "3")
    private int readingMinutes = 3;

    /**
     * Publier tout de suite, ou garder en brouillon.
     *
     * <p>Repasser un billet publié en brouillon est possible : c'est le seul
     * moyen de retirer un texte sans le supprimer, et donc sans perdre son
     * adresse ni son historique.</p>
     */
    @Schema(description = "Vrai pour rendre le billet visible du public.", example = "true")
    private boolean publie;

    /**
     * Le texte, dans une ou plusieurs langues.
     *
     * <p>Au moins une est exigée. Aucune ne l'est en particulier : un billet
     * rédigé d'abord en anglais est un billet valable, et attendre sa traduction
     * pour l'accepter reviendrait à faire du français une condition technique
     * plutôt qu'un choix éditorial.</p>
     */
    @NotEmpty(message = "{validation.blog.traduction}")
    @Valid
    private List<Traduction> traductions;

    @Data
    @Schema(name = "DemandeBillet.Traduction")
    public static class Traduction {

        @NotBlank(message = "{validation.blog.langue}")
        @Pattern(regexp = "^(fr|en|nl)$", message = "{validation.blog.langue}")
        @Schema(example = "fr")
        private String locale;

        @NotBlank(message = "{validation.blog.titre}")
        @Size(max = 200, message = "{validation.blog.titreLong}")
        private String title;

        @NotBlank(message = "{validation.blog.chapeau}")
        @Size(max = 500, message = "{validation.blog.chapeauLong}")
        private String lead;

        /**
         * Corps du billet, paragraphes séparés par une ligne vide.
         *
         * <p>Du texte, jamais du HTML : ce qui est saisi est rendu tel quel.
         * Accepter des balises ouvrirait une injection de script sur une page
         * publique, et la première personne à en profiter serait celle qui
         * compromettrait un compte d'administration.</p>
         */
        @NotBlank(message = "{validation.blog.corps}")
        @Schema(description = "Paragraphes séparés par une ligne vide. Texte brut, le HTML n'est pas interprété.")
        private String body;
    }
}
