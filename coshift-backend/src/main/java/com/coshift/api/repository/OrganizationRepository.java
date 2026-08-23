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

    /**
     * Organisation revendiquant ce domaine de courriel.
     *
     * <p>Insensible à la casse : les adresses arrivent telles que la personne
     * les a tapées, et {@code @Solvantis.be} désigne le même domaine que
     * {@code @solvantis.be}.</p>
     *
     * <p>Une organisation désactivée ne rattache plus personne. Sans cette
     * condition, résilier un contrat laisserait les inscriptions continuer à
     * verser des comptes dans un cercle qui n'a plus de client.</p>
     */
    Optional<Organization> findByEmailDomainIgnoreCaseAndActiveTrue(String emailDomain);
}