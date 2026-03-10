package com.coshift.api.exception;

public class TripNotFoundException extends RuntimeException {
  public TripNotFoundException(Long id) {
        super("Le trajet avec l'ID " + id + " est introuvable.");
    }
}
