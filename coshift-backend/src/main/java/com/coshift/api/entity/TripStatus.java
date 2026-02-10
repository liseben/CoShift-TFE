package com.coshift.api.entity;

public enum TripStatus {
    PLANNED,    // Trajet publié et ouvert
    FULL,       // Plus de places disponibles
    COMPLETED,  // Trajet terminé (les passagers sont arrivés)
    CANCELLED   // Annulé par le conducteur
}