package com.coshift.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

/**
 * Vue de supervision : ce qui se passe sur le périmètre de l'administrateur.
 *
 * <h2>Deux portées, un seul écran</h2>
 *
 * <p>Un {@code SUPER_ADMIN} répond de la plateforme entière et voit tout. Un
 * {@code ADMIN} répond de ses organisations et ne voit qu'elles. Ce n'est pas
 * une commodité d'affichage : c'est le cercle fermé appliqué à la supervision.
 * Un administrateur d'entreprise n'a pas à connaître les trajets d'une autre
 * société parce qu'on lui a donné un rôle.</p>
 *
 * <p>Le champ {@link #portee} dit laquelle des deux s'applique, pour que
 * l'écran l'annonce au lieu de laisser croire à un décompte global.</p>
 */
@Builder
@Schema(name = "SupervisionAdmin",
        description = "Chiffres de supervision, bornés au périmètre de l'administrateur connecté.")
public record AdminOverviewResponse(

        @Schema(description = "PLATEFORME pour un SUPER_ADMIN, ORGANISATIONS pour un ADMIN.",
                example = "PLATEFORME")
        String portee,

        @Schema(description = "Organisations couvertes. Vide pour la portée plateforme, qui les couvre toutes.")
        List<String> organisations,

        Membres membres,
        Trajets trajets,
        Reservations reservations
) {

    @Builder
    @Schema(name = "SupervisionAdmin.Membres")
    public record Membres(
            @Schema(description = "Comptes du périmètre, effacés exclus.", example = "122") long total,
            @Schema(description = "Comptes dont l'adresse a été confirmée.", example = "98") long verifies,
            @Schema(description = "Comptes suspendus par la modération.", example = "0") long suspendus,
            @Schema(description = "Comptes anonymisés au titre de l'article 17. Comptés, jamais listés.",
                    example = "0") long effaces
    ) {}

    @Builder
    @Schema(name = "SupervisionAdmin.Trajets")
    public record Trajets(
            @Schema(example = "81") long aVenir,
            @Schema(example = "119") long realises,
            @Schema(example = "8") long annules,
            @Schema(description = "Trajets rattachés à aucune organisation.", example = "2") long sansOrganisation
    ) {}

    @Builder
    @Schema(name = "SupervisionAdmin.Reservations")
    public record Reservations(
            @Schema(example = "12") long enAttente,
            @Schema(example = "180") long confirmees,
            @Schema(example = "60") long honorees,
            @Schema(example = "10") long annulees
    ) {}
}
