package com.coshift.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Jeu de données ouvert de CoShift : statistiques agrégées de covoiturage.
 *
 * <p>Chaque bloc porte sa propre définition dans la documentation. Un chiffre
 * dont on ignore la règle de calcul n'est pas réutilisable, et une statistique
 * non réutilisable n'est pas une donnée ouverte.</p>
 */
@Builder
@Schema(name = "DonneesOuvertes",
        description = "Statistiques agrégées de mobilité, publiques et librement réutilisables.")
public record OpenDataResponse(

        @Schema(description = "Instant de calcul du jeu de données.", example = "2026-08-20T07:15:00")
        LocalDateTime genereLe,

        @Schema(description = "Instant avant lequel une nouvelle requête renverra ces mêmes valeurs.",
                example = "2026-08-20T07:30:00")
        LocalDateTime valableJusqua,

        @Schema(description = "Version du format de ce jeu de données, indépendante de celle de l'API.",
                example = "1.0")
        String versionJeuDeDonnees,

        Licence licence,
        Perimetre perimetre,
        Volumes volumes,
        Anonymisation anonymisation,

        @Schema(description = "Volume mensuel, ordonné du mois le plus ancien au plus récent.")
        List<Mois> parMois,

        @Schema(description = "Villes desservies, de la plus fréquentée à la moins fréquentée.")
        List<Ville> villes
) {

    @Builder
    @Schema(name = "DonneesOuvertes.Licence")
    public record Licence(
            @Schema(example = "Licence Ouverte / Open Licence 2.0 (Etalab)") String nom,
            @Schema(example = "https://www.etalab.gouv.fr/licence-ouverte-open-licence/") String url,
            @Schema(description = "Mention à faire figurer par le réutilisateur.",
                    example = "Source : CoShift, données ouvertes.") String attributionDemandee
    ) {}

    @Builder
    @Schema(name = "DonneesOuvertes.Perimetre")
    public record Perimetre(
            @Schema(description = "Date du premier trajet couvert.", example = "2026-06-19") LocalDate premierTrajet,
            @Schema(description = "Date du dernier trajet couvert.", example = "2026-09-03") LocalDate dernierTrajet,
            @Schema(description = "Organisations ayant publié au moins un trajet.", example = "12") long organisations
    ) {}

    @Builder
    @Schema(name = "DonneesOuvertes.Volumes")
    public record Volumes(
            @Schema(description = "Trajets publiés, hors annulés.", example = "150") long trajetsPublies,
            @Schema(description = "Trajets annulés par leur conducteur.", example = "14") long trajetsAnnules,
            @Schema(description = "Réservations confirmées ou honorées.", example = "258") long reservationsAbouties,
            @Schema(description = "Places effectivement occupées par un passager.", example = "312") long placesPartagees,
            @Schema(description = "Places encore libres sur les trajets non annulés.", example = "188") long placesRestantes,
            @Schema(description = "placesPartagees / (placesPartagees + placesRestantes), en pourcentage.",
                    example = "62.4") BigDecimal tauxDeRemplissage
    ) {}

    @Builder
    @Schema(name = "DonneesOuvertes.Anonymisation")
    public record Anonymisation(
            @Schema(description = "Nombre minimal de trajets pour qu'un groupe soit publié.", example = "5")
            int seuil,
            @Schema(description = "Villes écartées faute d'atteindre ce seuil.", example = "0")
            long villesEcartees,
            @Schema(description = "Couples départ-arrivée distincts recensés.", example = "127")
            long liaisonsRecensees,
            @Schema(description = "Couples atteignant le seuil, et donc publiables.", example = "0")
            long liaisonsPubliables,
            @Schema(description = "Pourquoi les liaisons ne figurent pas dans ce jeu.")
            String noteSurLesLiaisons,
            @Schema(description = "Ce que le jeu de données ne contient jamais.")
            List<String> donneesExclues
    ) {}

    @Builder
    @Schema(name = "DonneesOuvertes.Mois")
    public record Mois(
            @Schema(description = "Mois de départ, au format AAAA-MM.", example = "2026-08") String mois,
            @Schema(example = "38") long trajets,
            @Schema(example = "71") long placesPartagees
    ) {}

    @Builder
    @Schema(name = "DonneesOuvertes.Ville")
    public record Ville(
            @Schema(example = "Liège") String ville,
            @Schema(description = "Trajets partant de cette ville.", example = "9") long trajetsAuDepart,
            @Schema(description = "Trajets y arrivant.", example = "6") long trajetsALArrivee,
            @Schema(description = "Places occupées sur les trajets partant de cette ville.", example = "11")
            long placesPartagees,
            @Schema(description = "Prix moyen d'une place au départ de cette ville, en euros.", example = "5.75")
            BigDecimal prixMoyenAuDepart
    ) {}
}
