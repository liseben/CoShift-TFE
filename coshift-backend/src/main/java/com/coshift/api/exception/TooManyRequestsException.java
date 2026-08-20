package com.coshift.api.exception;

/**
 * Trop de tentatives sur un point d'entrée sensible — traduite en HTTP 429.
 *
 * <p>Utilisée par {@code LoginAttemptService} pour la connexion, la vérification
 * d'adresse et la réinitialisation du mot de passe : trois opérations dont le
 * secret se devine par essais successifs si rien ne les freine.</p>
 */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
