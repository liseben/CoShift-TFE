package com.coshift.api.entity;

public enum BookingStatus {
    PENDING,    // En attente (ex: paiement non validé ou approbation conducteur)
    CONFIRMED,  // Validé et payé
    CANCELLED,  // Annulé par le passager
    REJECTED,   // Refusé par le conducteur
    COMPLETED   // Trajet effectué
}