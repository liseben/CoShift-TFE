package com.coshift.api.controller.vehicule;

import com.coshift.api.dto.VehiculeRequest;
import com.coshift.api.dto.VehiculeResponse;
import com.coshift.api.service.VehiculeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicules")
@RequiredArgsConstructor
@Tag(name = "Véhicules",
     description = "Véhicules déclarés par un membre. Publier un trajet suppose d'en avoir enregistré un.")
public class VehiculeController {

    private final VehiculeService vehiculeService;

    // F14bis — Lister mes véhicules
    @Operation(
            summary = "Lister mes véhicules",
            description = "Les véhicules du membre connecté. Un membre ne voit jamais ceux d'un autre.")
    @ApiResponse(responseCode = "200", description = "Liste des véhicules.")
    @GetMapping("/mine")
    public ResponseEntity<List<VehiculeResponse>> getMyVehicules(Authentication auth) {
        return ResponseEntity.ok(vehiculeService.getMyVehicules(auth.getName()));
    }

    // F14bis — Enregistrer un nouveau véhicule
    @Operation(
            summary = "Enregistrer un véhicule",
            description = """
                    Marque, modèle, plaque, nombre de places et motorisation.

                    Le nombre de places déclaré ici plafonne ensuite le nombre de passagers
                    acceptés sur un trajet, **place du conducteur déduite**.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Véhicule enregistré."),
            @ApiResponse(responseCode = "400", description = "Champ manquant ou nombre de places hors bornes.", content = @Content()),
            @ApiResponse(responseCode = "409", description = "Cette plaque est déjà enregistrée.", content = @Content())
    })
    @PostMapping
    public ResponseEntity<VehiculeResponse> addVehicule(
            @Valid @RequestBody VehiculeRequest request,
            Authentication auth) {
        VehiculeResponse created = vehiculeService.addVehicule(auth.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // F17 — Modifier un véhicule
    @Operation(
            summary = "Modifier un véhicule",
            description = "Réservé au propriétaire du véhicule.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Véhicule modifié."),
            @ApiResponse(responseCode = "403", description = "Ce véhicule ne vous appartient pas.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Véhicule introuvable.", content = @Content())
    })
    @PutMapping("/{uuid}")
    public ResponseEntity<VehiculeResponse> updateVehicule(
            @Parameter(description = "Identifiant public du véhicule.")
            @PathVariable String uuid,
            @Valid @RequestBody VehiculeRequest request,
            Authentication auth) {
        return ResponseEntity.ok(vehiculeService.updateVehicule(auth.getName(), uuid, request));
    }

    // Supprimer un véhicule (si aucun trajet actif dessus)
    @Operation(
            summary = "Supprimer un véhicule",
            description = """
                    Refusé tant qu'un trajet actif s'appuie sur ce véhicule : le supprimer
                    laisserait des trajets publiés sans voiture, et des passagers avec une
                    réservation sur un trajet devenu incohérent.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Véhicule supprimé."),
            @ApiResponse(responseCode = "403", description = "Ce véhicule ne vous appartient pas.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Véhicule introuvable.", content = @Content()),
            @ApiResponse(responseCode = "409", description = "Un trajet actif utilise encore ce véhicule.", content = @Content())
    })
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Map<String, String>> deleteVehicule(
            @PathVariable String uuid,
            Authentication auth) {
        vehiculeService.deleteVehicule(auth.getName(), uuid);
        return ResponseEntity.ok(Map.of("message", "Véhicule supprimé avec succès."));
    }
}
