package com.coshift.api.exception;

/**
 * Ressource demandée inexistante (utilisateur, trajet, véhicule...).
 * Traduite en HTTP 404 par le {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
