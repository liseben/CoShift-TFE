package com.coshift.api.repository;

import com.coshift.api.entity.BlogPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    /** Billets visibles du public, du plus récent au plus ancien. */
    List<BlogPost> findByPublishedAtIsNotNullOrderByPublishedAtDesc();

    /**
     * Tous les billets, brouillons compris, pour l'administration.
     *
     * <p>Le tri porte sur la date de création et non sur celle de publication :
     * un brouillon n'en a pas, et il finirait en queue de liste alors que c'est
     * précisément lui qui attend une action.</p>
     */
    List<BlogPost> findAllByOrderByCreatedAtDesc();

    Optional<BlogPost> findBySlug(String slug);

    Optional<BlogPost> findByUuid(String uuid);

    boolean existsBySlug(String slug);
}
