package com.coshift.api.repository;

import com.coshift.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Pour le Login (Sécurité)
    Optional<User> findByEmail(String email);

    // Pour l'API publique
    Optional<User> findByUuid(String uuid);

    // Pour éviter les doublons à l'inscription
    boolean existsByEmail(String email);

    /**
     * Inscriptions jamais confirmées et antérieures à une date.
     *
     * <p>La condition sur {@code deletedAt} écarte les comptes effacés à la
     * demande : l'anonymisation y remet {@code emailVerified} à faux, et sans
     * cette exclusion la purge les reprendrait à chaque passage.</p>
     */
    List<User> findByEmailVerifiedFalseAndDeletedAtIsNullAndCreatedAtBefore(LocalDateTime limite);

    /**
     * Comptes portant un code de vérification ou de réinitialisation périmé.
     *
     * <p>Une requête plutôt qu'un nom dérivé : la condition est une disjonction
     * sur deux couples de colonnes, et le nom de méthode correspondant serait
     * illisible.</p>
     */
    @Query("""
            SELECT u FROM User u
            WHERE (u.verificationCodeExpiry IS NOT NULL AND u.verificationCodeExpiry < :maintenant)
               OR (u.passwordResetExpiry   IS NOT NULL AND u.passwordResetExpiry   < :maintenant)
            """)
    List<User> findWithExpiredCodes(@Param("maintenant") LocalDateTime maintenant);
}
