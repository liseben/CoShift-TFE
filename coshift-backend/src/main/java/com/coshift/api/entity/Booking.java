package com.coshift.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    
    @Builder.Default
    @Column(unique = true, nullable = false, updatable = false)
    private String uuid = UUID.randomUUID().toString();

    // --- RELATIONS ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private User passenger;

    // --- DÉTAILS ---

    @Min(value = 1, message = "{validation.reservation.placesMin}")
    @Column(nullable = false)
    private int seatsBooked;

    @NotNull(message = "{validation.reservation.prixTotal}")
    @Column(nullable = false)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    /**
     * Motif du dernier changement de statut : raison du refus saisie par le
     * conducteur (F20) ou de l'annulation par le passager (F29).
     */
    @Column(name = "status_reason", length = 500)
    private String statusReason;

    /**
     * Date à laquelle le passager a confirmé que le trajet avait eu lieu (F21).
     *
     * <p>{@code null} tant que la prestation n'est pas confirmée. Le statut
     * {@link BookingStatus#COMPLETED} ne suffirait pas à porter cette
     * information : il dit que la course a eu lieu, pas quand elle a été
     * reconnue. C'est cette date qui ouvre la fenêtre de notation — on ne note
     * qu'après coup — et qui distingue une confirmation immédiate d'une
     * confirmation tardive.</p>
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // --- AUDIT ---

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}