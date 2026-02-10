package com.coshift.api.repository;

import com.coshift.api.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    
    // Pour trouver une org via l'URL (ex: /app/google)
    Optional<Organization> findBySlug(String slug);
    
    // Pour l'API publique
    Optional<Organization> findByUuid(String uuid);
    
    // Vérifier si un slug existe déjà (pour l'inscription)
    boolean existsBySlug(String slug);
}