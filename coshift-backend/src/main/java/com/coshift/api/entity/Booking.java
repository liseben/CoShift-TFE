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
    /**
     * Ce qui est dû, réglé et rendu pour cette réservation.
     *
     * <p>Côté inverse de la relation : c'est {@link Payment} qui porte la clé
     * étrangère, parce que c'est le paiement qui n'existe pas sans réservation
     * et non l'inverse.</p>
     *
     * <h2>Pourquoi EAGER, contre l'habitude</h2>
     *
     * <p>Un {@code @OneToOne} inverse ne se charge pas paresseusement sans
     * instrumentation du bytecode : Hibernate doit interroger la base ne
     * serait-ce que pour savoir s'il existe une ligne, et ne peut donc pas
     * fabriquer de mandataire. Déclarée LAZY, cette relation lançait cette
     * requête au milieu du traitement d'une autre et faisait échouer
     * l'annulation d'une réservation avec un « Illegal pop() with non-matching
     * JdbcValuesSourceProcessingState » — un message qui ne dit rien de la
     * cause, et qui n'apparaissait qu'à l'exécution contre une vraie base.</p>
     *
     * <p>Le coût est une requête supplémentaire par réservation chargée. Il est
     * assumé à cette échelle — une liste de réservations tient en quelques
     * lignes — et se lèverait par une jointure explicite dans les requêtes de
     * liste le jour où le volume le demanderait.</p>
     */
    @OneToOne(mappedBy = "booking", fetch = FetchType.EAGER)
    private Payment payment;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // --- AUDIT ---

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}