package com.coshift.api.repository;

import com.coshift.api.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    // MULTI-TENANT : Trouver les trajets d'une organisation
    List<Trip> findByOrganizationId(Long organizationId);

    // Trouver les trajets proposés par un conducteur spécifique
    List<Trip> findByDriverId(Long driverId);

    // Pour l'affichage public d'un trajet unique
    Optional<Trip> findByUuid(String uuid);
}