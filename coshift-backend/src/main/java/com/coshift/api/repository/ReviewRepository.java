package com.coshift.api.repository;

import com.coshift.api.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    /** Un trajet, un avis par participant : sert à refuser le second. */
    boolean existsByBookingIdAndAuthorId(Long bookingId, Long authorId);

    /** Avis reçus, du plus récent au plus ancien. */
    List<Review> findByTargetIdOrderByCreatedAtDesc(Long targetId);

    /** Avis écrits, pour l'export au titre de l'article 20 du RGPD. */
    List<Review> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    /**
     * Moyenne des notes reçues par une personne.
     *
     * <p>Recalculée en base plutôt qu'en mémoire : charger tous les avis d'un
     * membre pour en faire la moyenne serait un parcours inutile, et la valeur
     * n'a de sens qu'à l'instant du calcul.</p>
     *
     * <p>Renvoie {@code null} lorsque la personne n'a encore reçu aucun avis —
     * {@code AVG} sur un ensemble vide n'est pas zéro, c'est l'absence de
     * valeur. L'appelant traduit ce cas en « pas encore noté », qui ne se
     * confond pas avec « noté zéro ».</p>
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.target.id = :targetId")
    Double moyenneDesNotes(@Param("targetId") Long targetId);

    /** Nombre d'avis reçus, affiché à côté de la moyenne pour la pondérer. */
    long countByTargetId(Long targetId);

    /**
     * Réservations que cette personne a déjà notées.
     *
     * <p>Une seule requête pour toute une liste, plutôt qu'un
     * {@code existsBy...} par ligne affichée. Le client s'en sert pour ne
     * proposer « Noter » que là où c'est encore possible : sans cette
     * information, le bouton resterait visible après coup et le second clic
     * partirait chercher un 409 que l'on savait d'avance.</p>
     */
    @Query("SELECT r.booking.id FROM Review r WHERE r.author.id = :authorId")
    List<Long> reservationsDejaNoteesPar(@Param("authorId") Long authorId);
}
