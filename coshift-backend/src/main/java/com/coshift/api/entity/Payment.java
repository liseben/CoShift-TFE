package com.coshift.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ce qui est dû, réglé et rendu pour une réservation.
 *
 * <h2>Ce que cette entité n'est pas</h2>
 *
 * <p>Elle ne fait pas circuler d'argent. Elle tient la comptabilité d'un
 * siège : ce qui est dû, ce qui a été réglé, ce qui a été rendu et pourquoi.
 * Le mouvement de fonds appartient à un prestataire, et {@link #provider} dit
 * lequel a traité l'opération.</p>
 *
 * <h2>Pourquoi le montant est recopié</h2>
 *
 * <p>{@link #amount} duplique {@code booking.totalPrice} au moment où le
 * paiement naît. C'est délibéré : le prix d'un trajet peut changer, et un
 * paiement doit dire ce qui a été réglé ce jour-là, pas ce que coûterait la
 * même place aujourd'hui. Une facture ne se recalcule pas.</p>
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(unique = true, nullable = false, updatable = false)
    private String uuid = UUID.randomUUID().toString();

    /** Une réservation, un paiement : deux dus pour un même siège n'auraient pas de sens. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(nullable = false, length = 3)
    private String currency = "EUR";

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.DUE;

    /**
     * Prestataire ayant traité l'opération.
     *
     * <p>{@code SIMULATION} tant qu'aucun prestataire agréé n'est branché. Le
     * nommer vaut mieux que de laisser une colonne vide qui laisserait croire à
     * un encaissement réel.</p>
     */
    @Builder.Default
    @Column(nullable = false, length = 20)
    private String provider = "SIMULATION";

    /** Référence chez le prestataire : la seule façon de rapprocher cette ligne de son relevé. */
    @Column(name = "provider_reference")
    private String providerReference;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    /**
     * Montant réellement rendu.
     *
     * <p>Distinct du montant total : le barème d'annulation prévoit des
     * remboursements partiels, et « remboursé » sans montant ne dirait pas
     * combien.</p>
     */
    @Builder.Default
    @Column(name = "refunded_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    /** Pourquoi ce remboursement, et selon quelle règle du barème. */
    @Column(name = "refund_reason", length = 255)
    private String refundReason;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** Vrai si de l'argent a été prélevé et n'a pas été intégralement rendu. */
    public boolean estRegle() {
        return status == PaymentStatus.PAID || status == PaymentStatus.PARTIALLY_REFUNDED;
    }

    /** Ce qui reste acquis au conducteur, une fois le remboursement déduit. */
    public BigDecimal montantAcquis() {
        if (!estRegle()) return BigDecimal.ZERO;
        return amount.subtract(refundedAmount);
    }
}
