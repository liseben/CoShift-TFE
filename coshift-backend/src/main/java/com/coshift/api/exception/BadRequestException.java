package com.coshift.api.exception;

/**
 * Requête invalide sur le plan métier : code de vérification erroné ou expiré,
 * fichier au mauvais format...
 * Traduite en HTTP 400 par le {@link GlobalExceptionHandler}.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
