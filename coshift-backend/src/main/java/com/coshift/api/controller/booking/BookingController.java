package com.coshift.api.controller.booking;

import com.coshift.api.dto.BookingDecisionRequest;
import com.coshift.api.dto.BookingRequest;
import com.coshift.api.dto.BookingResponse;
import com.coshift.api.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // F27 — Réserver une ou plusieurs places
    @PostMapping
    public ResponseEntity<BookingResponse> book(
            @Valid @RequestBody BookingRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.book(auth.getName(), request));
    }

    // F30 — Mes réservations en tant que passager
    @GetMapping("/mine")
    public ResponseEntity<List<BookingResponse>> getMyBookings(Authentication auth) {
        return ResponseEntity.ok(bookingService.getMyBookings(auth.getName()));
    }

    // F19 — Toutes les demandes reçues sur mes trajets
    @GetMapping("/received")
    public ResponseEntity<List<BookingResponse>> getBookingsReceived(Authentication auth) {
        return ResponseEntity.ok(bookingService.getBookingsReceived(auth.getName()));
    }

    // F19 — Les demandes reçues sur un trajet précis
    @GetMapping("/trip/{tripUuid}")
    public ResponseEntity<List<BookingResponse>> getBookingsForTrip(
            @PathVariable String tripUuid,
            Authentication auth) {
        return ResponseEntity.ok(bookingService.getBookingsForTrip(auth.getName(), tripUuid));
    }

    // F20 — Accepter une demande
    @PatchMapping("/{uuid}/accept")
    public ResponseEntity<BookingResponse> accept(
            @PathVariable String uuid,
            Authentication auth) {
        return ResponseEntity.ok(bookingService.accept(auth.getName(), uuid));
    }

    // F20 — Refuser une demande, avec motif
    @PatchMapping("/{uuid}/reject")
    public ResponseEntity<BookingResponse> reject(
            @PathVariable String uuid,
            @Valid @RequestBody(required = false) BookingDecisionRequest request,
            Authentication auth) {
        String reason = (request != null) ? request.getReason() : null;
        return ResponseEntity.ok(bookingService.reject(auth.getName(), uuid, reason));
    }

    // F29 — Annuler sa réservation
    @PatchMapping("/{uuid}/cancel")
    public ResponseEntity<BookingResponse> cancel(
            @PathVariable String uuid,
            @Valid @RequestBody(required = false) BookingDecisionRequest request,
            Authentication auth) {
        String reason = (request != null) ? request.getReason() : null;
        return ResponseEntity.ok(bookingService.cancel(auth.getName(), uuid, reason));
    }
}
