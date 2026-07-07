package com.coshift.api.controller.vehicule;

import com.coshift.api.dto.VehiculeRequest;
import com.coshift.api.dto.VehiculeResponse;
import com.coshift.api.service.VehiculeService;
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
@CrossOrigin(origins = "${app.cors.allowed-origins:*}")
public class VehiculeController {

    private final VehiculeService vehiculeService;

    // F14bis — Lister mes véhicules
    @GetMapping("/mine")
    public ResponseEntity<List<VehiculeResponse>> getMyVehicules(Authentication auth) {
        return ResponseEntity.ok(vehiculeService.getMyVehicules(auth.getName()));
    }

    // F14bis — Enregistrer un nouveau véhicule
    @PostMapping
    public ResponseEntity<VehiculeResponse> addVehicule(
            @Valid @RequestBody VehiculeRequest request,
            Authentication auth) {
        VehiculeResponse created = vehiculeService.addVehicule(auth.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // F17 — Modifier un véhicule
    @PutMapping("/{uuid}")
    public ResponseEntity<VehiculeResponse> updateVehicule(
            @PathVariable String uuid,
            @Valid @RequestBody VehiculeRequest request,
            Authentication auth) {
        return ResponseEntity.ok(vehiculeService.updateVehicule(auth.getName(), uuid, request));
    }

    // Supprimer un véhicule (si aucun trajet actif dessus)
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Map<String, String>> deleteVehicule(
            @PathVariable String uuid,
            Authentication auth) {
        vehiculeService.deleteVehicule(auth.getName(), uuid);
        return ResponseEntity.ok(Map.of("message", "Véhicule supprimé avec succès."));
    }
}
