package com.coshift.api.controller.trip;

import com.coshift.api.dto.TripRequest;
import com.coshift.api.dto.TripResponse;
import com.coshift.api.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
@Tag(name = "Trajets",
     description = "Publication, recherche, consultation et annulation des trajets proposés par les conducteurs.")
public class TripController {

    private final TripService tripService;

    // F25 — Rechercher des trajets (accessible à tous les membres)
    @Operation(
            summary = "Rechercher des trajets",
            description = """
                    Renvoie les trajets encore ouverts correspondant aux critères. Tous les
                    paramètres sont facultatifs : sans aucun d'eux, la recherche retourne
                    l'ensemble des trajets disponibles.

                    Trois filtres sont appliqués d'office et ne se désactivent pas : seuls
                    les trajets au statut `PLANNED`, dont le départ est encore à venir, et
                    publiés par un membre d'une **organisation commune** à celle de
                    l'appelant. C'est la traduction technique du caractère B2B du produit :
                    on ne monte pas dans la voiture d'un inconnu.""")
    @ApiResponse(responseCode = "200", description = "Liste des trajets correspondants, éventuellement vide.")
    @GetMapping("/search")
    public ResponseEntity<List<TripResponse>> searchTrips(
            @Parameter(description = "Ville de départ, recherche partielle et insensible à la casse.", example = "Liège")
            @RequestParam(required = false) String departure,
            @Parameter(description = "Ville d'arrivée, recherche partielle et insensible à la casse.", example = "Bruxelles")
            @RequestParam(required = false) String arrival,
            @Parameter(description = "Jour du départ, au format ISO (AAAA-MM-JJ).", example = "2026-09-03")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Nombre de places nécessaires.", example = "2")
            @RequestParam(required = false) Integer seats,
            Authentication auth) {
        return ResponseEntity.ok(
                tripService.searchTrips(auth.getName(), departure, arrival, date, seats));
    }

    // F26 — Détail d'un trajet
    @Operation(
            summary = "Consulter le détail d'un trajet",
            description = """
                    Itinéraire, horaire, prix, préférences, conducteur et véhicule.

                    Le numéro de téléphone du conducteur **n'apparaît pas ici** : il n'est
                    communiqué qu'au passager dont la réservation a été confirmée.

                    Un trajet ouvert à une organisation n'est lisible que par ses membres,
                    et par son conducteur. Hors de ce cercle, la réponse est **404** et non
                    403 : distinguer les deux confirmerait l'existence du trajet et
                    révélerait l'organisation de son conducteur.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Détail du trajet."),
            @ApiResponse(responseCode = "404", description = "Aucun trajet pour cet identifiant, ou trajet hors de votre cercle.", content = @Content())
    })
    @GetMapping("/{uuid}")
    public ResponseEntity<TripResponse> getTripDetail(
            @Parameter(description = "Identifiant public du trajet.", example = "0d4-0107-0000-4000-8000-000000000107")
            @PathVariable String uuid,
            Authentication auth) {
        return ResponseEntity.ok(tripService.getTripByUuid(auth.getName(), uuid));
    }

    // F19 — Mes trajets (conducteur)
    @Operation(
            summary = "Lister mes trajets proposés",
            description = "Tous les trajets publiés par le membre connecté, quel que soit leur statut, du plus récent au plus ancien.")
    @ApiResponse(responseCode = "200", description = "Liste des trajets du conducteur.")
    @GetMapping("/mine")
    public ResponseEntity<List<TripResponse>> getMyTrips(Authentication auth) {
        return ResponseEntity.ok(tripService.getMyTrips(auth.getName()));
    }

    // F16 — Publier un trajet
    @Operation(
            summary = "Publier un trajet",
            description = """
                    Met un trajet à disposition des membres de l'organisation.

                    Trois règles bloquantes, toutes vérifiées côté serveur — les contrôles
                    du formulaire ne sont qu'un confort, un appel direct à l'API les
                    contournerait :

                    1. le départ doit être **au moins deux heures** après la publication ;
                    2. un conducteur ne peut avoir plus de **cinq trajets actifs** ;
                    3. le nombre de places proposées ne peut dépasser la capacité du
                       véhicule **une fois celle du conducteur déduite**.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trajet publié."),
            @ApiResponse(responseCode = "400", description = "Départ trop proche, ou plus de places que le véhicule n'en compte.", content = @Content()),
            @ApiResponse(responseCode = "403", description = "Le véhicule désigné ne vous appartient pas.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Véhicule introuvable.", content = @Content()),
            @ApiResponse(responseCode = "409", description = "Quota de cinq trajets actifs atteint.", content = @Content())
    })
    @PostMapping
    public ResponseEntity<TripResponse> publishTrip(
            @Valid @RequestBody TripRequest request,
            Authentication auth) {
        TripResponse created = tripService.publishTrip(auth.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // F18 — Annuler un trajet
    @Operation(
            summary = "Annuler un trajet",
            description = """
                    Retire le trajet des recherches et **annule en cascade** toutes les
                    demandes en attente et les réservations déjà confirmées, avec le motif
                    « Trajet annulé par le conducteur ». Sans cette cascade, un passager
                    conserverait une réservation confirmée sur un trajet qui n'existe plus.

                    Réservé au conducteur du trajet, et impossible sur un trajet déjà
                    parti. L'action est définitive : il n'existe pas de « désannulation ».""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trajet annulé ; le statut renvoyé est CANCELLED."),
            @ApiResponse(responseCode = "403", description = "Vous n'êtes pas le conducteur de ce trajet.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Trajet introuvable.", content = @Content()),
            @ApiResponse(responseCode = "409", description = "Le trajet est déjà passé.", content = @Content())
    })
    @PatchMapping("/{uuid}/cancel")
    public ResponseEntity<TripResponse> cancelTrip(
            @Parameter(description = "Identifiant public du trajet.")
            @PathVariable String uuid,
            Authentication auth) {
        return ResponseEntity.ok(tripService.cancelTrip(auth.getName(), uuid));
    }

}
