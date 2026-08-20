package com.coshift.api.service;

import com.coshift.api.dto.OpenDataResponse;
import com.coshift.api.repository.OpenDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Produit le jeu de données ouvert.
 *
 * <h2>Deux garde-fous</h2>
 *
 * <p><strong>Anonymat.</strong> Rien de nominatif ne sort d'ici : ni conducteur,
 * ni passager, ni adresse précise, ni horaire. Seules des villes et des
 * comptages. Un groupe réunissant moins de {@value #SEUIL} trajets est écarté —
 * « un trajet, une place » décrit un déplacement individuel, qu'un annuaire
 * d'entreprise suffirait à rattacher à quelqu'un. Le nombre de groupes ainsi
 * retirés est publié, faute de quoi le lecteur croirait le jeu exhaustif.</p>
 *
 * <p>C'est pour cette raison que l'agrégation porte sur la <em>ville</em> et non
 * sur le couple départ-arrivée : au volume actuel, la liaison la plus empruntée
 * ne compte que deux trajets. Le seuil les écarterait toutes, et le jeu de
 * données serait vide.</p>
 *
 * <p><strong>Coût.</strong> Le point d'entrée est public et sans jeton : rien
 * n'empêche de l'appeler en boucle. Le résultat est donc calculé au plus une
 * fois toutes les {@value #CACHE_MINUTES} minutes et servi depuis la mémoire
 * entre-temps. Sans cela, une poignée de requêtes par seconde suffirait à
 * occuper la base avec des agrégations complètes.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpenDataService {

    /** Nombre minimal de trajets pour qu'un groupe soit publié (k-anonymat). */
    public static final int SEUIL = 5;

    /** Durée de validité du jeu calculé. */
    public static final int CACHE_MINUTES = 15;

    /** Version du format publié, indépendante de celle de l'API. */
    private static final String VERSION_JEU = "1.0";

    private final OpenDataRepository repository;

    private volatile OpenDataResponse cache;

    /**
     * Renvoie le jeu de données, recalculé seulement si le précédent a expiré.
     *
     * <p>Deux appels simultanés peuvent calculer en parallèle et l'un écraser
     * l'autre : sans conséquence, les deux produisent la même chose. Un verrou
     * coûterait plus cher que le calcul qu'il éviterait.</p>
     */
    public OpenDataResponse jeuDeDonnees() {
        OpenDataResponse actuel = cache;
        if (actuel != null && actuel.valableJusqua().isAfter(LocalDateTime.now())) {
            return actuel;
        }
        OpenDataResponse frais = calculer();
        cache = frais;
        log.debug("Jeu de données ouvert recalculé : {} villes publiées.", frais.villes().size());
        return frais;
    }

    private OpenDataResponse calculer() {
        LocalDateTime maintenant = LocalDateTime.now();

        long placesPartagees = repository.compterPlacesPartagees();
        long placesRestantes = repository.compterPlacesRestantes();
        long total = placesPartagees + placesRestantes;

        // Un taux calculé sur zéro place n'a pas de sens : on publie 0 plutôt
        // qu'une division impossible ou un champ absent.
        BigDecimal taux = (total == 0)
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(placesPartagees * 100.0 / total).setScale(1, RoundingMode.HALF_UP);

        Object[] bornes = premiereLigne(repository.bornesTemporelles());

        return OpenDataResponse.builder()
                .genereLe(maintenant)
                .valableJusqua(maintenant.plus(Duration.ofMinutes(CACHE_MINUTES)))
                .versionJeuDeDonnees(VERSION_JEU)
                .licence(OpenDataResponse.Licence.builder()
                        .nom("Licence Ouverte / Open Licence 2.0 (Etalab)")
                        .url("https://www.etalab.gouv.fr/licence-ouverte-open-licence/")
                        .attributionDemandee("Source : CoShift, données ouvertes.")
                        .build())
                .perimetre(OpenDataResponse.Perimetre.builder()
                        .premierTrajet(enDate(bornes, 0))
                        .dernierTrajet(enDate(bornes, 1))
                        .organisations(repository.compterOrganisations())
                        .build())
                .volumes(OpenDataResponse.Volumes.builder()
                        .trajetsPublies(repository.compterTrajets())
                        .trajetsAnnules(repository.compterTrajetsAnnules())
                        .reservationsAbouties(repository.compterReservationsAbouties())
                        .placesPartagees(placesPartagees)
                        .placesRestantes(placesRestantes)
                        .tauxDeRemplissage(taux)
                        .build())
                .anonymisation(OpenDataResponse.Anonymisation.builder()
                        .seuil(SEUIL)
                        .villesEcartees(repository.compterVillesEcartees(SEUIL))
                        .liaisonsRecensees(repository.compterLiaisonsRecensees())
                        .liaisonsPubliables(repository.compterLiaisonsPubliables(SEUIL))
                        .noteSurLesLiaisons("""
                                Les couples départ-arrivée ne sont pas publiés : au volume actuel, \
                                aucun n'atteint le seuil de """ + SEUIL + """
                                 trajets. Publier une liaison empruntée une ou deux fois reviendrait \
                                à décrire un déplacement individuel. L'agrégation se fait donc par \
                                ville desservie, qui regroupe assez de trajets pour que personne n'y \
                                soit reconnaissable.""")
                        .donneesExclues(List.of(
                                "identité des conducteurs et des passagers",
                                "adresses de départ et d'arrivée précises",
                                "horaires de départ",
                                "nom des organisations",
                                "montants payés individuellement"))
                        .build())
                .parMois(repository.volumeParMois().stream()
                        .map(l -> OpenDataResponse.Mois.builder()
                                .mois((String) l[0])
                                .trajets(enLong(l[1]))
                                .placesPartagees(enLong(l[2]))
                                .build())
                        .toList())
                .villes(repository.villesDesservies(SEUIL).stream()
                        .map(l -> OpenDataResponse.Ville.builder()
                                .ville((String) l[0])
                                .trajetsAuDepart(enLong(l[1]))
                                .trajetsALArrivee(enLong(l[2]))
                                .placesPartagees(enLong(l[3]))
                                .prixMoyenAuDepart(enDecimal(l[4]))
                                .build())
                        .toList())
                .build();
    }

    /**
     * Export CSV des villes desservies, au format RFC 4180.
     *
     * <p>Le CSV n'est pas un luxe : c'est le format qu'ouvre un tableur, donc
     * celui par lequel une donnée ouverte est réellement réutilisée par
     * quelqu'un qui n'écrit pas de code.</p>
     */
    public String villesEnCsv() {
        StringBuilder csv = new StringBuilder(
                "ville,trajets_au_depart,trajets_a_l_arrivee,places_partagees,prix_moyen_au_depart_eur\n");
        for (OpenDataResponse.Ville v : jeuDeDonnees().villes()) {
            csv.append(echapper(v.ville())).append(',')
               .append(v.trajetsAuDepart()).append(',')
               .append(v.trajetsALArrivee()).append(',')
               .append(v.placesPartagees()).append(',')
               .append(v.prixMoyenAuDepart() == null ? "" : v.prixMoyenAuDepart().toPlainString())
               .append('\n');
        }
        return csv.toString();
    }

    /** Une valeur contenant une virgule, un guillemet ou un saut de ligne doit être encadrée. */
    private String echapper(String valeur) {
        if (valeur == null) return "";
        if (valeur.contains(",") || valeur.contains("\"") || valeur.contains("\n")) {
            return '"' + valeur.replace("\"", "\"\"") + '"';
        }
        return valeur;
    }

    private Object[] premiereLigne(List<Object[]> lignes) {
        return lignes.isEmpty() ? new Object[] { null, null } : lignes.get(0);
    }

    private LocalDate enDate(Object[] ligne, int index) {
        Object v = (ligne.length > index) ? ligne[index] : null;
        if (v == null) return null;
        if (v instanceof Timestamp t) return t.toLocalDateTime().toLocalDate();
        if (v instanceof LocalDateTime d) return d.toLocalDate();
        return null;
    }

    private long enLong(Object v) {
        return (v instanceof Number n) ? n.longValue() : 0L;
    }

    private BigDecimal enDecimal(Object v) {
        if (v instanceof BigDecimal b) return b;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        return null;
    }
}
