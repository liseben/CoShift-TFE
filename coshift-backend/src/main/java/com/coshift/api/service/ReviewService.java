package com.coshift.api.service;

import com.coshift.api.dto.ReviewRequest;
import com.coshift.api.dto.ReviewResponse;
import com.coshift.api.entity.Booking;
import com.coshift.api.entity.BookingStatus;
import com.coshift.api.entity.Review;
import com.coshift.api.entity.Trip;
import com.coshift.api.entity.User;
import com.coshift.api.exception.ConflictException;
import com.coshift.api.exception.ResourceNotFoundException;
import com.coshift.api.exception.UnauthorizedException;
import com.coshift.api.repository.BookingRepository;
import com.coshift.api.repository.ReviewRepository;
import com.coshift.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Notation réciproque à l'issue d'un trajet partagé (F22, F31).
 *
 * <h2>Ce que la note remplace</h2>
 *
 * <p>Le covoiturage consiste à monter en voiture avec quelqu'un qu'on ne
 * connaît pas. Aucune fonctionnalité ne remplace le lien social ; la
 * réputation accumulée en est le seul substitut praticable. C'est pourquoi
 * {@code users.average_rating} figurait dans le schéma dès le premier jour —
 * et pourquoi le laisser à zéro pour tout le monde revenait à afficher un
 * indicateur faux plutôt qu'à ne rien afficher.</p>
 *
 * <h2>Trois barrières</h2>
 *
 * <ol>
 *   <li><strong>Il faut avoir voyagé.</strong> L'avis s'adosse à une
 *       réservation confirmée <em>puis</em> reconnue par le passager (F21).
 *       Sans cette condition, il suffirait de réserver et d'annuler pour
 *       obtenir le droit de noter.</li>
 *   <li><strong>Il faut avoir voyagé avec la personne notée.</strong> Seuls le
 *       passager et le conducteur de cette réservation peuvent s'exprimer, et
 *       chacun ne peut noter que l'autre.</li>
 *   <li><strong>Une seule fois.</strong> Un trajet, un avis par participant.
 *       Sans cette règle, noter en boucle suffirait à couler quelqu'un.</li>
 * </ol>
 *
 * <h2>La moyenne est recalculée, jamais incrémentée</h2>
 *
 * <p>Il serait tentant de mettre la moyenne à jour par pondération à chaque
 * nouvel avis. Ce calcul dérive : une erreur d'arrondi, un avis effacé, une
 * transaction interrompue, et la valeur affichée ne correspond plus à aucun
 * ensemble d'avis réel — sans que rien ne le signale. La moyenne est donc
 * relue depuis la table à chaque dépôt. La colonne reste une copie de travail,
 * jamais une source de vérité.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final Messages messages;

    /**
     * Dépose un avis sur l'autre participant d'un trajet effectué.
     *
     * @param authorEmail  auteur, déduit du jeton
     * @param bookingUuid  réservation qui fonde l'avis
     */
    @Transactional
    public ReviewResponse deposer(String authorEmail, String bookingUuid, ReviewRequest request) {
        User auteur = trouver(authorEmail);
        Booking booking = bookingRepository.findByUuid(bookingUuid)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("reservation.introuvable")));

        // 1. Il faut avoir voyagé. La confirmation de prestation par le passager
        //    (F21) est la seule preuve que la course a réellement eu lieu.
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new ConflictException(messages.get("avis.trajetNonTermine"));
        }

        // 2. Il faut avoir voyagé avec la personne notée.
        User cible = designerLautreParticipant(booking, auteur);

        // 3. Une seule fois. La contrainte d'unicité en base tient le même
        //    raisonnement ; ce contrôle existe pour rendre un message lisible
        //    plutôt qu'une violation de contrainte.
        if (reviewRepository.existsByBookingIdAndAuthorId(booking.getId(), auteur.getId())) {
            throw new ConflictException(messages.get("avis.dejaDepose"));
        }

        Review avis = Review.builder()
                .booking(booking)
                .author(auteur)
                .target(cible)
                .rating(request.getRating())
                .comment(videEnNull(request.getComment()))
                .build();

        reviewRepository.save(avis);
        recalculerLaMoyenne(cible);

        log.info("Avis déposé sur la réservation {} : {} étoile(s)", bookingUuid, request.getRating());
        return ReviewResponse.from(avis);
    }

    /** Avis reçus par un membre, les plus récents d'abord. */
    @Transactional(readOnly = true)
    public List<ReviewResponse> avisRecus(String email) {
        User membre = trouver(email);
        return reviewRepository.findByTargetIdOrderByCreatedAtDesc(membre.getId())
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    /**
     * Détermine qui est noté, et refuse l'accès à quiconque n'a pas partagé ce
     * trajet.
     *
     * <p>La notation est réciproque : sur une même réservation, le passager
     * note le conducteur et le conducteur note le passager. La cible se déduit
     * donc de la place qu'occupe l'auteur — et si l'auteur n'occupe aucune des
     * deux, il n'a rien à dire sur ce trajet.</p>
     */
    private User designerLautreParticipant(Booking booking, User auteur) {
        Trip trajet = booking.getTrip();
        if (trajet == null || trajet.getDriver() == null || booking.getPassenger() == null) {
            throw new ConflictException(messages.get("avis.participantsIntrouvables"));
        }

        Long auteurId = auteur.getId();
        Long passagerId = booking.getPassenger().getId();
        Long conducteurId = trajet.getDriver().getId();

        if (auteurId.equals(passagerId)) return trajet.getDriver();
        if (auteurId.equals(conducteurId)) return booking.getPassenger();

        throw new UnauthorizedException(messages.get("avis.pasParticipant"));
    }

    /**
     * Relit la moyenne depuis la table et la recopie sur le profil.
     *
     * <p>Aucun avis reçu ne donne pas une moyenne de zéro : {@code AVG} sur un
     * ensemble vide n'a pas de valeur. On retombe alors sur 0, qui est la
     * convention retenue par la colonne — et que l'interface traduit en
     * « pas encore noté », jamais en « noté zéro ».</p>
     */
    private void recalculerLaMoyenne(User cible) {
        Double moyenne = reviewRepository.moyenneDesNotes(cible.getId());
        cible.setAverageRating(moyenne == null ? 0.0 : arrondirAuDixieme(moyenne));
        userRepository.save(cible);
    }

    /** Une moyenne affichée avec quinze décimales n'apprend rien de plus. */
    private double arrondirAuDixieme(double valeur) {
        return Math.round(valeur * 10.0) / 10.0;
    }

    private User trouver(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("auth.utilisateurIntrouvable")));
    }

    private String videEnNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
