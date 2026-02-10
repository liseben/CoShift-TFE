package com.coshift.api.repository;

import com.coshift.api.entity.Vehicule; // Import de ta classe Vehicule
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VehiculeRepository extends JpaRepository<Vehicule, Long> {

    // Trouver tous les véhicules d'un utilisateur
    List<Vehicule> findByOwnerId(Long ownerId);

    // Sécurité : Trouver un véhicule par UUID (pour l'API)
    Optional<Vehicule> findByUuid(String uuid);
}