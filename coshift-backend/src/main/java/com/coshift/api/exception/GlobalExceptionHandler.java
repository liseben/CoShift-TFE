package com.coshift.api.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import com.coshift.api.security.SecurityAuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
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
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final SecurityAuditService audit;
    /** Pour renvoyer un message dans la langue de l'appelant plutot que celui d'un tiers. */
    private final com.coshift.api.service.Messages messages;

    // ─────────────────────────── 400 — Requête invalide ───────────────────────────

    /** Échec de la validation {@code @Valid} sur un DTO : on remonte les messages de champ. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        /* Le filtre sur les messages absents n'est pas une précaution de style.
           Collectors.joining traduit un null en la chaîne « null », qui n'est
           pas vide : le repli ci-dessous ne s'appliquait donc jamais, et un
           champ sans message rendait littéralement « null » à l'utilisateur —
           ou pire, « Le mot de passe est trop court. null » dès qu'un seul
           champ sur plusieurs était concerné. */
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(message -> message != null && !message.isBlank())
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

    /**
     * Compte suspendu par la modération.
     *
     * <p>Sans ce gestionnaire, {@code LockedException} tombait dans le filet de
     * {@code Exception} et sortait en <strong>500</strong> : une décision de
     * modération parfaitement volontaire se présentait comme une panne du
     * serveur, et le message expliquant la suspension n'atteignait jamais la
     * personne.</p>
     *
     * <p>403 et non 401 : les identifiants étaient bons. Ce n'est pas
     * l'authentification qui a échoué, c'est l'accès qui est refusé.</p>
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLocked(LockedException ex, WebRequest request) {
        return build(HttpStatus.FORBIDDEN, "Account Suspended", ex.getMessage(), request);
    }

    /**
     * Accès refusé à une ressource appartenant à quelqu'un d'autre.
     *
     * <p>Consigné au journal de sécurité : neuf contrôles de propriété
     * refusaient correctement l'accès sans en garder trace, si bien qu'un compte
     * méthodiquement sondé ne se distinguait pas d'un utilisateur maladroit.</p>
     */
    @ExceptionHandler({UnauthorizedException.class, AccessDeniedException.class, SecurityException.class})
    public ResponseEntity<ErrorResponse> handleForbidden(RuntimeException ex, WebRequest request,
                                                         HttpServletRequest http) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        audit.consigner(SecurityAuditService.Evenement.ACCES_REFUSE,
                (auth != null) ? auth.getName() : null,
                http.getRemoteAddr(),
                http.getMethod() + " " + chemin(request));
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

    /**
     * Panne du prestataire de paiement.
     *
     * <p>502 et non 500 : la panne n'est pas dans CoShift mais chez un service
     * tiers, et la distinction compte pour qui lit les journaux. Le message de
     * Stripe reste au journal — il est écrit pour un développeur et peut
     * contenir des détails de configuration ; la personne qui paie reçoit une
     * phrase du catalogue, dans sa langue.</p>
     */
    @ExceptionHandler(com.coshift.api.service.StripeGateway.PaymentGatewayException.class)
    public ResponseEntity<ErrorResponse> handleGateway(RuntimeException ex, WebRequest request) {
        return build(HttpStatus.BAD_GATEWAY, "Payment Gateway Error",
                messages.get("paiement.prestataireIndisponible"), request);
    }

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
                chemin(request)
        );
        return new ResponseEntity<>(body, status);
    }

    /**
     * Chemin de la requête, débarrassé du préfixe technique.
     *
     * <p>{@code WebRequest.getDescription(false)} renvoie {@code uri=/api/...} :
     * le préfixe fuitait tel quel dans le champ {@code path} de chaque erreur,
     * obligeant les clients à le retirer eux-mêmes.</p>
     */
    private String chemin(WebRequest request) {
        String description = request.getDescription(false);
        return (description != null && description.startsWith("uri="))
                ? description.substring(4)
                : description;
    }
}
