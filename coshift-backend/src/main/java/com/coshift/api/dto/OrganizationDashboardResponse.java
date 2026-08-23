package com.coshift.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tableau de bord de mobilité d'une organisation.
 *
 * <p>Destiné à ses membres, pas au public : c'est ce qui le distingue du jeu de
 * données ouvert, qui agrège toute la plateforme pour un lecteur anonyme et
 * applique pour cette raison un seuil de k-anonymat. Ici, les trajets comptés
 * sont ceux que le lecteur voit déjà un par un dans la recherche ; un seuil
 * masquerait des chiffres dont le détail est à portée de clic.</p>
 *
 * <p>Les définitions sont volontairement identiques à celles du jeu ouvert.
 * Deux chiffres portant le même nom dans deux écrans du produit doivent se
 * calculer pareil.</p>
 */
@Builder
@Schema(name = "TableauDeBordOrganisation",
        description = "Chiffres de covoiturage d'une organisation, réservés à ses membres.")
public record OrganizationDashboardResponse(

        @Schema(description = "Identifiant public de l'organisation.",
                example = "0a1-0001-0000-4000-8000-000000000001")
        String uuid,

        @Schema(example = "Solvantis Belgium") String name,
        @Schema(example = "solvantis") String slug,
        @Schema(description = "Logo de l'organisation, ou null.") String logoUrl,

        Volumes volumes,
        Participation participation,

        @Schema(description = "Volume mensuel, du mois le plus ancien au plus récent.")
        List<Mois> parMois,

        @Schema(description = "Ce que ces chiffres ne disent pas, et pourquoi.")
        NonMesure nonMesure
) {

    @Builder
    @Schema(name = "TableauDeBordOrganisation.Volumes")
    public record Volumes(
            @Schema(description = "Trajets ouverts à l'organisation, hors annulés.", example = "7")
            long trajetsPublies,

            @Schema(description = "Trajets annulés par leur conducteur. Comptés à part : les fondre dans le volume le gonflerait.",
                    example = "0")
            long trajetsAnnules,

            @Schema(description = "Trajets dont l'heure de départ est passée sans annulation.", example = "5")
            long trajetsRealises,

            @Schema(description = "Places effectivement occupées par un passager.", example = "7")
            long placesPartagees,

            @Schema(description = "Places encore libres sur les trajets non annulés.", example = "14")
            long placesRestantes,

            @Schema(description = "Part des places proposées qui ont trouvé preneur, en pourcentage.",
                    example = "33.3")
            BigDecimal tauxRemplissage
    ) {}

    @Builder
    @Schema(name = "TableauDeBordOrganisation.Participation")
    public record Participation(
            @Schema(description = "Personnes rattachées à l'organisation, qu'elles aient roulé ou non.", example = "11")
            long membres,

            @Schema(description = "Membres ayant publié au moins un trajet.", example = "4")
            long conducteurs,

            @Schema(description = "Membres ayant occupé au moins une place.", example = "5")
            long passagers
    ) {}

    @Builder
    @Schema(name = "TableauDeBordOrganisation.Mois")
    public record Mois(
            @Schema(description = "Mois de départ, au format AAAA-MM.", example = "2026-05") String mois,
            @Schema(example = "2") long trajets,
            @Schema(example = "4") long placesPartagees
    ) {}

    /**
     * Ce que le produit ne mesure pas.
     *
     * <p>Un tableau de bord d'employeur annonce d'ordinaire des kilomètres
     * partagés et des tonnes de CO₂ évitées. CoShift ne les publie pas, parce
     * qu'il ne les mesure pas : un trajet porte une ville de départ et une ville
     * d'arrivée, pas une distance. La produire supposerait un calcul
     * d'itinéraire à la publication, donc un service de cartographie appelé et
     * une valeur stockée.</p>
     *
     * <p>Rien n'empêcherait d'afficher un chiffre approché — c'est même le plus
     * facile à produire de tout cet écran, et le seul que personne ne songerait
     * à vérifier. Il serait faux. Annoncer l'absence est plus utile à qui doit
     * s'en servir qu'un ordre de grandeur présenté comme une mesure.</p>
     */
    @Builder
    @Schema(name = "TableauDeBordOrganisation.NonMesure")
    public record NonMesure(
            @Schema(description = "Vrai tant que la distance des trajets n'est pas calculée.", example = "true")
            boolean distanceParcourue,

            @Schema(description = "Vrai tant que la distance n'est pas connue : sans elle, aucune économie d'émissions n'est calculable.",
                    example = "true")
            boolean emissionsEvitees,

            @Schema(description = "Raison de ces absences, à afficher telle quelle.",
                    example = "Un trajet porte des villes, pas une distance.")
            String motif
    ) {}
}
