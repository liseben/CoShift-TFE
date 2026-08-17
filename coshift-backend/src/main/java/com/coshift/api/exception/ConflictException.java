package com.coshift.api.exception;

/**
 * Conflit avec l'état actuel de la ressource : email déjà utilisé,
 * quota de trajets actifs atteint, compte déjà vérifié...
 * Traduite en HTTP 409 par le {@link GlobalExceptionHandler}.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
