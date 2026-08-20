package com.coshift.api.controller.booking;

import com.coshift.api.dto.BookingDecisionRequest;
import com.coshift.api.dto.BookingRequest;
import com.coshift.api.dto.BookingResponse;
import com.coshift.api.service.BookingService;
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

/**
 * Cycle de vie d'une réservation, des deux côtés : le passager demande, le
 * conducteur décide.
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Réservations",
     description = "Demandes de place côté passager, décisions côté conducteur, et suivi des deux.")
public class BookingController {

    private final BookingService bookingService;

    // F27 — Réserver une ou plusieurs places
    @Operation(
            summary = "Demander une place",
            description = """
                    Crée une demande au statut `PENDING`. **Rien n'est encore réservé** :
                    la place n'est décomptée qu'au moment où le conducteur accepte.

                    Un conducteur ne peut pas réserver sur son propre trajet, et l'adresse
                    doit avoir été vérifiée.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Demande enregistrée, en attente de la réponse du conducteur."),
            @ApiResponse(responseCode = "400", description = "Trajet fermé, déjà passé, ou plus assez de places.", content = @Content()),
            @ApiResponse(responseCode = "403", description = "Compte non vérifié, ou trajet vous appartenant.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Trajet introuvable.", content = @Content()),
            @ApiResponse(responseCode = "409", description = "Vous avez déjà une demande en cours sur ce trajet.", content = @Content())
    })
    @PostMapping
    public ResponseEntity<BookingResponse> book(
            @Valid @RequestBody BookingRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.book(auth.getName(), request));
    }

    // F30 — Mes réservations en tant que passager
    @Operation(
            summary = "Lister mes réservations",
            description = """
                    Les demandes faites par le membre connecté, avec leur statut et, le cas
                    échéant, le motif d'un refus ou d'une annulation.

                    Le téléphone du conducteur n'est présent que sur les réservations
                    **confirmées** — c'est le moment où les deux personnes ont besoin de se
                    joindre, et pas avant.""")
    @ApiResponse(responseCode = "200", description = "Liste des réservations du passager.")
    @GetMapping("/mine")
    public ResponseEntity<List<BookingResponse>> getMyBookings(Authentication auth) {
        return ResponseEntity.ok(bookingService.getMyBookings(auth.getName()));
    }

    // F19 — Toutes les demandes reçues sur mes trajets
    @Operation(
            summary = "Lister les demandes reçues",
            description = "Toutes les demandes portant sur les trajets du membre connecté, tous statuts confondus.")
    @ApiResponse(responseCode = "200", description = "Liste des demandes reçues.")
    @GetMapping("/received")
    public ResponseEntity<List<BookingResponse>> getBookingsReceived(Authentication auth) {
        return ResponseEntity.ok(bookingService.getBookingsReceived(auth.getName()));
    }

    // F19 — Les demandes reçues sur un trajet précis
    @Operation(
            summary = "Lister les demandes sur un trajet",
            description = "Restreint la liste précédente à un trajet. Réservé au conducteur de ce trajet.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demandes portant sur ce trajet."),
            @ApiResponse(responseCode = "403", description = "Vous n'êtes pas le conducteur de ce trajet.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Trajet introuvable.", content = @Content())
    })
    @GetMapping("/trip/{tripUuid}")
    public ResponseEntity<List<BookingResponse>> getBookingsForTrip(
            @Parameter(description = "Identifiant public du trajet.")
            @PathVariable String tripUuid,
            Authentication auth) {
        return ResponseEntity.ok(bookingService.getBookingsForTrip(auth.getName(), tripUuid));
    }

    // F20 — Accepter une demande
    @Operation(
            summary = "Accepter une demande",
            description = """
                    Fait passer la demande en `CONFIRMED` et **décompte les places** du
                    trajet. Le trajet bascule en `FULL` s'il n'en reste plus.

                    C'est à partir de cet instant que le passager voit le numéro du
                    conducteur.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demande confirmée."),
            @ApiResponse(responseCode = "400", description = "Plus assez de places disponibles.", content = @Content()),
            @ApiResponse(responseCode = "403", description = "Vous n'êtes pas le conducteur de ce trajet.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable.", content = @Content()),
            @ApiResponse(responseCode = "409", description = "La demande n'est plus en attente.", content = @Content())
    })
    @PatchMapping("/{uuid}/accept")
    public ResponseEntity<BookingResponse> accept(
            @Parameter(description = "Identifiant public de la réservation.")
            @PathVariable String uuid,
            Authentication auth) {
        return ResponseEntity.ok(bookingService.accept(auth.getName(), uuid));
    }

    // F20 — Refuser une demande, avec motif
    @Operation(
            summary = "Refuser une demande",
            description = """
                    Fait passer la demande en `REJECTED`. Le corps de la requête est
                    facultatif ; s'il est fourni, le motif — 200 caractères au plus — est
                    **affiché au passager**. Refuser sans un mot le laisse sans
                    explication.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demande refusée."),
            @ApiResponse(responseCode = "403", description = "Vous n'êtes pas le conducteur de ce trajet.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable.", content = @Content()),
            @ApiResponse(responseCode = "409", description = "La demande n'est plus en attente.", content = @Content())
    })
    @PatchMapping("/{uuid}/reject")
    public ResponseEntity<BookingResponse> reject(
            @PathVariable String uuid,
            @Valid @RequestBody(required = false) BookingDecisionRequest request,
            Authentication auth) {
        String reason = (request != null) ? request.getReason() : null;
        return ResponseEntity.ok(bookingService.reject(auth.getName(), uuid, reason));
    }

    // F29 — Annuler sa réservation
    @Operation(
            summary = "Annuler ma réservation",
            description = """
                    Réservé au **passager** qui a fait la demande. Si elle était confirmée,
                    les places sont rendues au trajet, qui redevient réservable.

                    Le motif est facultatif et visible du conducteur.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réservation annulée."),
            @ApiResponse(responseCode = "403", description = "Cette réservation n'est pas la vôtre.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable.", content = @Content()),
            @ApiResponse(responseCode = "409", description = "Réservation déjà annulée, refusée ou terminée.", content = @Content())
    })
    @PatchMapping("/{uuid}/cancel")
    public ResponseEntity<BookingResponse> cancel(
            @PathVariable String uuid,
            @Valid @RequestBody(required = false) BookingDecisionRequest request,
            Authentication auth) {
        String reason = (request != null) ? request.getReason() : null;
        return ResponseEntity.ok(bookingService.cancel(auth.getName(), uuid, reason));
    }
}
