package com.coshift.api.repository;

import com.coshift.api.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingId(Long bookingId);

    Optional<Payment> findByBookingUuid(String bookingUuid);

    /** Rapproche une notification du prestataire de l'operation enregistree ici. */
    Optional<Payment> findByProviderReference(String providerReference);
}
