package com.coshift.api.controller.trip;

import com.coshift.api.dto.TripRequest;
import com.coshift.api.dto.TripResponse;
import com.coshift.api.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    // F25 — Rechercher des trajets (accessible à tous les membres)
    @GetMapping("/search")
    public ResponseEntity<List<TripResponse>> searchTrips(
            @RequestParam(required = false) String departure,
            @RequestParam(required = false) String arrival,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer seats,
            Authentication auth) {
        return ResponseEntity.ok(
                tripService.searchTrips(auth.getName(), departure, arrival, date, seats));
    }

    // F26 — Détail d'un trajet
    @GetMapping("/{uuid}")
    public ResponseEntity<TripResponse> getTripDetail(@PathVariable String uuid) {
        return ResponseEntity.ok(tripService.getTripByUuid(uuid));
    }

    // F19 — Mes trajets (conducteur)
    @GetMapping("/mine")
    public ResponseEntity<List<TripResponse>> getMyTrips(Authentication auth) {
        return ResponseEntity.ok(tripService.getMyTrips(auth.getName()));
    }

    // F16 — Publier un trajet
    @PostMapping
    public ResponseEntity<TripResponse> publishTrip(
            @Valid @RequestBody TripRequest request,
            Authentication auth) {
        TripResponse created = tripService.publishTrip(auth.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // F18 — Annuler un trajet
    @PatchMapping("/{uuid}/cancel")
    public ResponseEntity<TripResponse> cancelTrip(
            @PathVariable String uuid,
            Authentication auth) {
        return ResponseEntity.ok(tripService.cancelTrip(auth.getName(), uuid));
    }

    // F21 — Marquer comme terminé
    @PatchMapping("/{uuid}/complete")
    public ResponseEntity<Map<String, String>> completeTrip(
            @PathVariable String uuid,
            Authentication auth) {
        // TODO : implémenter avec logique de paiement Stripe
        return ResponseEntity.ok(Map.of("message", "Fonctionnalité disponible avec Stripe (F28)."));
    }
}
