package com.coshift.api.entity;

/**
 * État comptable d'une réservation.
 *
 * <p>Les états ne se recouvrent pas et ne reviennent pas en arrière, à une
 * exception près : un paiement réglé peut être remboursé, en tout ou en
 * partie. C'est le seul chemin de retour, et il laisse toujours une trace —
 * un montant rendu et un motif.</p>
 */
public enum PaymentStatus {
    /** Montant dû, rien n'a été prélevé. */
    DUE,
    /** Réglé. */
    PAID,
    /** Intégralement rendu. */
    REFUNDED,
    /** Partiellement rendu, selon le barème d'annulation. */
    PARTIALLY_REFUNDED,
    /** La réservation n'a pas abouti : rien n'était dû, rien n'a été prélevé. */
    CANCELLED,
    /** Le prestataire a refusé l'opération. */
    FAILED
}
