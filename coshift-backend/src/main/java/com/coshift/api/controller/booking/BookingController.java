package com.coshift.api.controller.booking;

import com.coshift.api.dto.BookingDecisionRequest;
import com.coshift.api.dto.BookingRequest;
import com.coshift.api.dto.BookingResponse;
import com.coshift.api.dto.ReviewRequest;
import com.coshift.api.dto.ReviewResponse;
import com.coshift.api.service.BookingService;
import com.coshift.api.exception.ResourceNotFoundException;
import com.coshift.api.service.Messages;
import com.coshift.api.repository.UserRepository;
import com.coshift.api.service.PaymentService;
import com.coshift.api.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Cycle de vie d'une réservation, des deux côtés : le passager demande, le
 * conducteur décide.
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Réservations",
     description = "Demandes de place côté passager, décisions côté conducteur, et suivi des deux.")
public class BookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final UserRepository userRepository;
    private final Messages messages;
    private final ReviewService reviewService;

    // F27 — Réserver une ou plusieurs places
    @Operation(
            summary = "Demander une place",
            description = """
                    Crée une demande au statut `PENDING`. **Rien n'est encore réservé** :
                    la place n'est décomptée qu'au moment où le conducteur accepte.

                    Un conducteur ne peut pas réserver sur son propre trajet, et l'adresse
                    doit avoir été vérifiée.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Demande enregistrée, en attente de la réponse du conducteur."),
            @ApiResponse(responseCode = "400", description = "Trajet fermé, déjà passé, ou plus assez de places.", content = @Content()),
            @ApiResponse(responseCode = "403", description = "Compte non vérifié, ou trajet vous appartenant.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Trajet introuvable.", content = @Content()),
            @ApiResponse(responseCode = "409", description = "Vous avez déjà une demande en cours sur ce trajet.", content = @Content())
    })
    @PostMapping
    public ResponseEntity<BookingResponse> book(
            @Valid @RequestBody BookingRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.book(auth.getName(), request));
    }

    // F30 — Mes réservations en tant que passager
    @Operation(
            summary = "Lister mes réservations",
            description = """
                    Les demandes faites par le membre connecté, avec leur statut et, le cas
                    échéant, le motif d'un refus ou d'une annulation.

                    Le téléphone du conducteur n'est présent que sur les réservations
                    **confirmées** — c'est le moment où les deux personnes ont besoin de se
                    joindre, et pas avant.""")
    @ApiResponse(responseCode = "200", description = "Liste des réservations du passager.")
    @GetMapping("/mine")
    public ResponseEntity<List<BookingResponse>> getMyBookings(Authentication auth) {
        return ResponseEntity.ok(bookingService.getMyBookings(auth.getName()));
    }

    // F19 — Toutes les demandes reçues sur mes trajets
    @Operation(
            summary = "Lister les demandes reçues",
            description = "Toutes les demandes portant sur les trajets du membre connecté, tous statuts confondus.")
    @ApiResponse(responseCode = "200", description = "Liste des demandes reçues.")
    @GetMapping("/received")
    public ResponseEntity<List<BookingResponse>> getBookingsReceived(Authentication auth) {
        return ResponseEntity.ok(bookingService.getBookingsReceived(auth.getName()));
    }

    // F19 — Les demandes reçues sur un trajet précis
    @Operation(
            summary = "Lister les demandes sur un trajet",
            description = "Restreint la liste précédente à un trajet. Réservé au conducteur de ce trajet.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demandes portant sur ce trajet."),
            @ApiResponse(responseCode = "403", description = "Vous n'êtes pas le conducteur de ce trajet.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Trajet introuvable.", content = @Content())
    })
    @GetMapping("/trip/{tripUuid}")
    public ResponseEntity<List<BookingResponse>> getBookingsForTrip(
            @Parameter(description = "Identifiant public du trajet.")
            @PathVariable String tripUuid,
            Authentication auth) {
        return ResponseEntity.ok(bookingService.getBookingsForTrip(auth.getName(), tripUuid));
    }

    // F20 — Accepter une demande
    @Operation(
            summary = "Accepter une demande",
            description = """
                    Fait passer la demande en `CONFIRMED` et **décompte les places** du
                    trajet. Le trajet bascule en `FULL` s'il n'en reste plus.

                    C'est à partir de cet instant que le passager voit le numéro du
                    conducteur.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demande confirmée."),
            @ApiResponse(responseCode = "400", description = "Plus assez de places disponibles.", content = @Content()),
            @ApiResponse(responseCode = "403", description = "Vous n'êtes pas le conducteur de ce trajet.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable.", content = @Content()),
            @ApiResponse(responseCode = "409", description = "La demande n'est plus en attente.", content = @Content())
    })
    @PatchMapping("/{uuid}/accept")
    public ResponseEntity<BookingResponse> accept(
            @Parameter(description = "Identifiant public de la réservation.")
            @PathVariable String uuid,
            Authentication auth) {
        return ResponseEntity.ok(bookingService.accept(auth.getName(), uuid));
    }

    // F20 — Refuser une demande, avec motif
    @Operation(
            summary = "Refuser une demande",
            description = """
                    Fait passer la demande en `REJECTED`. Le corps de la requête est
                    facultatif ; s'il est fourni, le motif — 200 caractères au plus — est
                    **affiché au passager**. Refuser sans un mot le laisse sans
                    explication.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demande refusée."),
            @ApiResponse(responseCode = "403", description = "Vous n'êtes pas le conducteur de ce trajet.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable.", content = @Content()),
            @ApiResponse(responseCode = "409", description = "La demande n'est plus en attente.", content = @Content())
    })
    @PatchMapping("/{uuid}/reject")
    public ResponseEntity<BookingResponse> reject(
            @PathVariable String uuid,
            @Valid @RequestBody(required = false) BookingDecisionRequest request,
            Authentication auth) {
        String reason = (request != null) ? request.getReason() : null;
        return ResponseEntity.ok(bookingService.reject(auth.getName(), uuid, reason));
    }

    // F29 — Annuler sa réservation
    @Operation(
            summary = "Annuler ma réservation",
            description = """
                    Réservé au **passager** qui a fait la demande. Si elle était confirmée,
                    les places sont rendues au trajet, qui redevient réservable.

                    Le motif est facultatif et visible du conducteur.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réservation annulée."),
            @ApiResponse(responseCode = "403", description = "Cette réservation n'est pas la vôtre.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable.", content = @Content()),
            @ApiResponse(responseCode = "409", description = "Réservation déjà annulée, refusée ou terminée.", content = @Content())
    })
    @PatchMapping("/{uuid}/cancel")
    public ResponseEntity<BookingResponse> cancel(
            @PathVariable String uuid,
            @Valid @RequestBody(required = false) BookingDecisionRequest request,
            Authentication auth) {
        String reason = (request != null) ? request.getReason() : null;
        return ResponseEntity.ok(bookingService.cancel(auth.getName(), uuid, reason));
    }

    // F21 — Confirmer que le trajet a eu lieu
    @Operation(
            summary = "Confirmer que le trajet a eu lieu",
            description = """
                    Réservé au **passager**, et volontairement : le conducteur a un intérêt
                    à déclarer la course effectuée — elle alimente son compteur de trajets
                    et, demain, sa rémunération. Le passager n'en a pas ; il confirme ce
                    qu'il a constaté. C'est ce qui rend l'information fiable.

                    La réservation doit être **acceptée** et le départ doit être **passé** :
                    une demande restée en attente n'a transporté personne, et une course à
                    venir ne peut pas être confirmée d'avance.

                    **Effet :** la réservation passe en `COMPLETED`, la date de confirmation
                    est enregistrée, et le compteur de trajets des **deux** participants est
                    incrémenté — un covoiturage effectué compte pour celui qui conduit comme
                    pour celui qui monte.

                    L'opération n'est pas rejouable : une seconde confirmation renvoie 409.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prestation confirmée."),
            @ApiResponse(responseCode = "403", description = "Cette réservation n'est pas la vôtre.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable.", content = @Content()),
            @ApiResponse(responseCode = "409", description = "Réservation non acceptée, déjà confirmée, ou trajet pas encore parti.", content = @Content())
    })
    @PatchMapping("/{uuid}/complete")
    public ResponseEntity<BookingResponse> complete(
            @Parameter(description = "Identifiant public de la réservation.")
            @PathVariable String uuid,
            Authentication auth) {
        return ResponseEntity.ok(bookingService.complete(auth.getName(), uuid));
    }

    // F22 / F31 — Noter l'autre participant
    @Operation(
            summary = "Noter l'autre participant du trajet",
            description = """
                    La notation est **réciproque** : sur une même réservation, le passager
                    note le conducteur et le conducteur note le passager. La personne notée
                    se déduit de la place qu'occupe l'auteur ; il n'y a rien à préciser.

                    **Trois conditions**, dans cet ordre :

                    1. *Il faut avoir voyagé.* La réservation doit être `COMPLETED`,
                       c'est-à-dire confirmée par le passager. Sans cela, réserver puis
                       annuler donnerait le droit de noter.
                    2. *Il faut avoir voyagé avec la personne notée.* Un tiers reçoit 403.
                    3. *Une seule fois.* Un trajet, un avis par participant — sinon noter
                       en boucle suffirait à couler quelqu'un.

                    La note va de 1 à 5 ; le commentaire est facultatif et borné à 500
                    caractères. La moyenne du profil est **relue depuis la table** après
                    chaque dépôt, jamais mise à jour par pondération : un calcul incrémental
                    dérive sans que rien ne le signale.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avis enregistré ; la moyenne du profil noté est recalculée."),
            @ApiResponse(responseCode = "400", description = "Note hors barème ou commentaire trop long.", content = @Content()),
            @ApiResponse(responseCode = "403", description = "Vous n'avez pas partagé ce trajet.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable.", content = @Content()),
            @ApiResponse(responseCode = "409", description = "Trajet non confirmé, ou avis déjà déposé.", content = @Content())
    })
    @PostMapping("/{uuid}/review")
    public ResponseEntity<ReviewResponse> review(
            @Parameter(description = "Identifiant public de la réservation.")
            @PathVariable String uuid,
            @Valid @RequestBody ReviewRequest request,
            Authentication auth) {
        return ResponseEntity.ok(reviewService.deposer(auth.getName(), uuid, request));
    }

    // F22 / F31 — Les avis que j'ai reçus
    @Operation(
            summary = "Les avis que j'ai reçus",
            description = """
                    Les avis laissés sur le membre connecté, du plus récent au plus ancien.

                    Chaque avis porte le **prénom seul** de son auteur : un avis se lit
                    d'abord pour ce qu'il dit, et afficher un nom complet à côté d'un
                    jugement exposerait une personne au-delà de ce que la fonctionnalité
                    demande.""")
    @ApiResponse(responseCode = "200", description = "Liste des avis reçus, éventuellement vide.")
    @GetMapping("/reviews/received")
    public ResponseEntity<List<ReviewResponse>> reviewsRecus(Authentication auth) {
        return ResponseEntity.ok(reviewService.avisRecus(auth.getName()));
    }

    // ────────────────────────── F28 — Partage de frais ───────────────────────

    @Operation(
            summary = "Ouvrir le règlement de sa réservation",
            description = """
                    Annonce le paiement au prestataire et renvoie de quoi le confirmer.

                    **Deux issues selon le prestataire configuré.** Sans clé Stripe, la
                    simulation déclare le montant acquis sur-le-champ : `regleImmediatement`
                    vaut vrai, `secretClient` est nul, et il n'y a rien de plus à faire. Avec
                    Stripe, `secretClient` porte un jeton à usage unique que le navigateur
                    présente aux serveurs de Stripe **avec les coordonnées bancaires — qui ne
                    transitent jamais par CoShift**. Le paiement reste alors dû : une
                    intention créée n'est pas un paiement reçu.

                    Le champ `prestataire` dit lequel des deux répond, pour que l'écran
                    puisse annoncer qu'aucun euro ne circule quand c'est le cas.

                    Appelé deux fois, il rend une nouvelle intention : un secret client est à
                    usage unique, et quelqu'un dont la carte a été refusée doit pouvoir
                    réessayer.

                    Seul le passager de la réservation peut ouvrir son règlement. Sans ce
                    contrôle, n'importe quel compte pourrait solder celle d'un autre à partir
                    de son seul identifiant public — et apprendre au passage ce qu'elle
                    coûte.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Intention de paiement ouverte."),
            @ApiResponse(responseCode = "403", description = "Ce n'est pas votre réservation.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Aucun paiement pour cette réservation.", content = @Content()),
            @ApiResponse(responseCode = "409", description = "Ce montant n'est plus dû.", content = @Content()),
            @ApiResponse(responseCode = "502", description = "Le prestataire de paiement n'a pas répondu.", content = @Content())
    })
    @PostMapping("/{uuid}/payment")
    public ResponseEntity<java.util.Map<String, Object>> ouvrirReglement(
            @Parameter(description = "Identifiant public de la réservation.") @PathVariable String uuid,
            Authentication auth) {
        var passager = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("auth.utilisateurIntrouvable")));
        var intention = paymentService.preparerReglement(passager, uuid);

        var reponse = new java.util.HashMap<String, Object>();
        reponse.put("prestataire", intention.regleImmediatement() ? "SIMULATION" : "STRIPE");
        reponse.put("regleImmediatement", intention.regleImmediatement());
        /* Le secret n'est ni conserve ni journalise : c'est un laissez-passer a
           usage unique, transmis puis oublie. */
        reponse.put("secretClient", intention.secretClient());
        reponse.put("reservation", bookingService.parUuidPourPassager(auth.getName(), uuid));
        return ResponseEntity.ok(reponse);
    }

    @Operation(
            summary = "Vérifier où en est le règlement",
            description = """
                    Interroge le prestataire et met l'état à jour.

                    **Pourquoi le navigateur n'est pas cru sur parole.** Après avoir confirmé,
                    la page annonce « c'est payé ». Cette page est entre les mains de la
                    personne qui paie : la croire reviendrait à laisser qui le souhaite
                    marquer sa réservation réglée depuis l'outil de développement de son
                    navigateur. Le serveur interroge donc Stripe, seul à savoir.

                    Ce chemin double celui des notifications signées, qui reste l'autorité en
                    production. Il existe parce qu'un poste de développement n'a pas d'adresse
                    publique où les recevoir, et parce qu'une notification peut se perdre.""")
    @ApiResponse(responseCode = "200", description = "État à jour de la réservation.")
    @PostMapping("/{uuid}/payment/verification")
    public ResponseEntity<BookingResponse> verifierReglement(
            @Parameter(description = "Identifiant public de la réservation.") @PathVariable String uuid,
            Authentication auth) {
        var passager = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("auth.utilisateurIntrouvable")));
        paymentService.verifier(passager, uuid);
        return ResponseEntity.ok(bookingService.parUuidPourPassager(auth.getName(), uuid));
    }

    @Operation(
            summary = "Ce qui serait remboursé en cas d'annulation",
            description = """
                    Renvoie la part qui serait rendue, en pourcentage, si la réservation était
                    annulée maintenant par son passager.

                    **Pourquoi un point d'entrée pour cela.** L'interface l'appelle pour
                    annoncer le montant *avant* que la personne confirme. Découvrir après coup
                    qu'on ne récupère que la moitié est le genre de surprise qui vaut une
                    réclamation — et le barème n'a d'intérêt que s'il est connu au moment de
                    décider.

                    Le barème tient en une idée : on ne fait pas payer quelqu'un pour une
                    décision qui n'est pas la sienne. Plus de 24 h avant le départ, tout est
                    rendu. En deçà, la moitié : le siège ne se reloue plus, et rendre tout
                    ferait de l'annulation de dernière minute une option gratuite. Après le
                    départ, rien.""")
    @ApiResponse(responseCode = "200", description = "Part remboursable, en pourcentage.")
    @GetMapping("/{uuid}/remboursement")
    public ResponseEntity<java.util.Map<String, Object>> remboursement(
            @Parameter(description = "Identifiant public de la réservation.") @PathVariable String uuid,
            Authentication auth) {
        var paiement = paymentService.trouver(uuid);
        int part = paymentService.partRendue(paiement.getBooking().getTrip().getDepartureTime(), false);
        return ResponseEntity.ok(java.util.Map.of(
                "partRendue", part,
                "montantRendu", paiement.getAmount()
                        .multiply(java.math.BigDecimal.valueOf(part))
                        .divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP),
                "devise", paiement.getCurrency()));
    }

}
