package com.coshift.api.repository;

import com.coshift.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Pour le Login (Sécurité)
    Optional<User> findByEmail(String email);

    // Pour l'API publique
    Optional<User> findByUuid(String uuid);

    // Pour éviter les doublons à l'inscription
    boolean existsByEmail(String email);
}