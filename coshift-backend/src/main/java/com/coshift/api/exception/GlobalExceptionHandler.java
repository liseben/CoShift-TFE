package com.coshift.api.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Traduit les exceptions de l'application en réponses HTTP cohérentes.
 *
 * <p>Chaque exception métier possède son propre statut : le message rédigé dans
 * le service parvient donc intact au frontend, qui le lit dans
 * {@code error.response.data.message}. Le handler générique reste en dernier
 * recours : il journalise la trace complète côté serveur et ne renvoie qu'un
 * message neutre au client, pour ne pas exposer de détail d'implémentation.</p>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ─────────────────────────── 400 — Requête invalide ───────────────────────────

    /** Échec de la validation {@code @Valid} sur un DTO : on remonte les messages de champ. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining(" "));

        if (details.isBlank()) {
            details = "Certains champs du formulaire sont invalides.";
        }
        return build(HttpStatus.BAD_REQUEST, "Validation Error", details, request);
    }

    @ExceptionHandler({BadRequestException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    /** Plus aucune place disponible sur le trajet demandé. */
    @ExceptionHandler(NoSeatsAvailableException.class)
    public ResponseEntity<ErrorResponse> handleNoSeats(NoSeatsAvailableException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Full Trip", ex.getMessage(), request);
    }

    // ─────────────────────── 401 — Authentification refusée ───────────────────────

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleBadCredentials(RuntimeException ex, WebRequest request) {
        // Message volontairement identique dans les deux cas : ne pas révéler
        // si l'adresse existe en base (énumération de comptes).
        return build(HttpStatus.UNAUTHORIZED, "Invalid Credentials",
                "Email ou mot de passe incorrect.", request);
    }

    // ─────────────────────────── 403 — Accès interdit ─────────────────────────────

    /** Compte existant mais non activé (email non vérifié). */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException ex, WebRequest request) {
        return build(HttpStatus.FORBIDDEN, "Account Disabled", ex.getMessage(), request);
    }

    @ExceptionHandler({UnauthorizedException.class, AccessDeniedException.class, SecurityException.class})
    public ResponseEntity<ErrorResponse> handleForbidden(RuntimeException ex, WebRequest request) {
        return build(HttpStatus.FORBIDDEN, "Unauthorized Access", ex.getMessage(), request);
    }

    // ────────────────────────── 404 — Ressource absente ───────────────────────────

    @ExceptionHandler({ResourceNotFoundException.class, TripNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    // ──────────────────────────── 409 — Conflit d'état ────────────────────────────

    @ExceptionHandler({ConflictException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException ex, WebRequest request) {
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
    }

    // ─────────────────────────── 413 — Fichier trop lourd ─────────────────────────

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadTooLarge(MaxUploadSizeExceededException ex, WebRequest request) {
        return build(HttpStatus.CONTENT_TOO_LARGE, "File Too Large",
                "Le fichier envoyé dépasse la taille maximale autorisée.", request);
    }

    // ──────────────────────── 429 — Trop de tentatives ────────────────────────────

    /** Freinage des essais successifs sur la connexion et les codes à six chiffres. */
    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequests(TooManyRequestsException ex, WebRequest request) {
        return build(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", ex.getMessage(), request);
    }

    // ───────────────────────────── 500 — Dernier recours ──────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, WebRequest request) {
        // Sans ce log, une erreur inattendue disparaissait sans laisser de trace.
        log.error("Erreur non gérée sur {} : ", request.getDescription(false), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error",
                "Une erreur inattendue est survenue.", request);
    }

    // ────────────────────────────────── Fabrique ──────────────────────────────────

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error,
                                                String message, WebRequest request) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                error,
                (message == null || message.isBlank()) ? status.getReasonPhrase() : message,
                request.getDescription(false)
        );
        return new ResponseEntity<>(body, status);
    }
}
