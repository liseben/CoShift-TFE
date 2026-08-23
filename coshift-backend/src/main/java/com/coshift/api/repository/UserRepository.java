package com.coshift.api.repository;

import com.coshift.api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * Membres visibles par un administrateur, filtres par une recherche libre.
     *
     * <p>Les comptes anonymises au titre de l article 17 sont exclus : ils n ont
     * plus de nom ni d adresse, et les moderer n aurait ni objet ni fondement.</p>
     *
     * <p>{@code plateforme} leve la restriction pour un SUPER_ADMIN. Sinon seuls
     * les membres d une organisation passee remontent. La liste d organisations
     * n est jamais vide — l appelant y met une valeur impossible — sans quoi le
     * {@code IN} ne serait pas du SQL valide.</p>
     *
     * <p>La recherche porte sur le nom, le prenom et l adresse. {@code EXISTS}
     * plutot qu une jointure : une jointure sur une relation a plusieurs
     * multiplierait les lignes et fausserait le decompte de la pagination.</p>
     */
    @Query("""
            SELECT u FROM User u
            WHERE u.deletedAt IS NULL
              AND (:plateforme = TRUE
                   OR EXISTS (SELECT 1 FROM User m JOIN m.organizations o
                              WHERE m.id = u.id AND o.id IN :organisations))
              AND (:recherche IS NULL
                   OR LOWER(u.email)     LIKE :recherche
                   OR LOWER(u.lastname)  LIKE :recherche
                   OR LOWER(u.firstname) LIKE :recherche)
            """)
    Page<User> rechercherPourAdministration(@Param("plateforme") boolean plateforme,
                                            @Param("organisations") java.util.Collection<Long> organisations,
                                            @Param("recherche") String recherche,
                                            Pageable pageable);
}
