package com.coshift.api.controller.payment;

import com.coshift.api.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Notifications de Stripe.
 *
 * <h2>Pourquoi ce point d'entrée est l'autorité</h2>
 *
 * <p>La confirmation d'un paiement ne peut pas venir du navigateur : il est
 * entre les mains de la personne qui paie. Elle vient d'ici — un appel que
 * Stripe adresse au serveur, signé avec un secret partagé que le navigateur ne
 * possède pas.</p>
 *
 * <h2>La signature est vérifiée avant tout</h2>
 *
 * <p>Sans elle, ce point d'entrée serait une invitation ouverte : n'importe qui
 * connaissant son adresse déclarerait ses réservations réglées en envoyant un
 * corps de requête bien formé. C'est pourquoi le corps est lu <strong>brut</strong>
 * et non désérialisé par le cadre : la signature porte sur les octets exacts,
 * et une reconstruction JSON — ne serait-ce qu'un espace de différence —
 * invaliderait le calcul.</p>
 *
 * <h2>Pourquoi il répond 200 même sur un événement inconnu</h2>
 *
 * <p>Stripe réessaie ce qu'il n'a pas pu remettre. Répondre en erreur à un
 * événement qu'on ne traite pas — et il en émet des dizaines — le ferait
 * réessayer indéfiniment. On accuse réception ; ce qui n'est pas compris est
 * ignoré, sans bruit.</p>
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Paiements", description = "Notifications du prestataire de paiement.")
public class StripeWebhookController {

    private final PaymentService paymentService;

    /** Secret partagé avec Stripe. Vide quand aucun prestataire réel n'est branché. */
    @Value("${stripe.webhook-secret:}")
    private String secret;

    @Operation(
            summary = "Recevoir une notification de Stripe",
            description = """
                    Point d'entrée appelé par Stripe, jamais par le navigateur.

                    **C'est ici que se décide qu'un paiement est acquis.** La page qui suit le
                    règlement affiche « c'est payé », mais elle est entre les mains de la
                    personne qui paie : la croire reviendrait à laisser qui le souhaite
                    marquer sa réservation réglée depuis l'outil de développement de son
                    navigateur.

                    La signature `Stripe-Signature` est vérifiée avant toute lecture du
                    contenu. Sans elle, ce chemin serait une invitation ouverte. Le corps est
                    donc lu brut : la signature porte sur les octets exacts.

                    Répond **200 même sur un événement non traité** — Stripe réessaie ce qu'il
                    n'a pas pu remettre, et une erreur sur un événement dont on n'a que faire
                    le ferait réessayer sans fin.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification reçue."),
            @ApiResponse(responseCode = "400", description = "Signature absente ou invalide.")
    })
    @PostMapping("/webhook")
    public ResponseEntity<String> recevoir(@RequestBody String corps,
                                           @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        if (secret == null || secret.isBlank() || secret.contains("REMPLACE")) {
            /* Aucun secret configuré : on ne peut rien vérifier, donc on ne
               croit rien. Accepter les notifications sans signature reviendrait
               a laisser n'importe qui declarer des paiements. */
            log.warn("Notification de paiement reçue alors qu'aucun secret n'est configuré — ignorée.");
            return ResponseEntity.badRequest().body("webhook non configuré");
        }

        Event evenement;
        try {
            evenement = Webhook.constructEvent(corps, signature, secret);
        } catch (SignatureVerificationException e) {
            /* Consigné comme un fait de sécurité : une signature invalide n'est
               pas une maladresse, c'est une tentative. */
            log.warn("Notification de paiement à la signature invalide, rejetée : {}", e.getMessage());
            return ResponseEntity.badRequest().body("signature invalide");
        }

        if ("payment_intent.succeeded".equals(evenement.getType())) {
            evenement.getDataObjectDeserializer().getObject()
                    .filter(PaymentIntent.class::isInstance)
                    .map(PaymentIntent.class::cast)
                    .ifPresent(intention -> paymentService.confirmerDepuisPrestataire(intention.getId()));
        } else {
            log.debug("Événement Stripe non traité : {}", evenement.getType());
        }

        return ResponseEntity.ok("reçu");
    }
}
