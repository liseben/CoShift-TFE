package com.coshift.api.dto;

import com.coshift.api.entity.Review;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Avis tel qu'il est rendu au client.
 *
 * <p>L'auteur est identifié par son prénom seul. Un avis se lit d'abord pour
 * ce qu'il dit, et afficher le nom complet d'un membre à côté d'un jugement
 * exposerait une personne bien au-delà de ce que la fonctionnalité demande.
 * L'adresse et le téléphone n'y figurent évidemment pas.</p>
 */
@Data
@Builder
public class ReviewResponse {

    private String uuid;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;

    /** Prénom de l'auteur, ou {@code null} si son compte a été effacé. */
    private String authorFirstname;
    private String authorPictureUrl;

    /** Trajet concerné, pour situer l'avis. */
    private String departureCity;
    private String arrivalCity;
    private LocalDateTime departureTime;

    public static ReviewResponse from(Review review) {
        var auteur = review.getAuthor();
        var trajet = review.getBooking() != null ? review.getBooking().getTrip() : null;

        return ReviewResponse.builder()
                .uuid(review.getUuid())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .authorFirstname(auteur != null ? auteur.getFirstname() : null)
                .authorPictureUrl(auteur != null ? auteur.getPictureUrl() : null)
                .departureCity(trajet != null ? trajet.getDepartureCity() : null)
                .arrivalCity(trajet != null ? trajet.getArrivalCity() : null)
                .departureTime(trajet != null ? trajet.getDepartureTime() : null)
                .build();
    }
}
