package com.coshift.api.service;

import com.coshift.api.entity.Payment;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Paiement par Stripe, en mode test.
 *
 * <h2>Les coordonnées bancaires ne passent jamais par CoShift</h2>
 *
 * <p>C'est la propriété qui justifie tout le reste. Le serveur crée une
 * <em>intention</em> de paiement et rend un secret à usage unique ; le
 * navigateur présente ce secret aux serveurs de Stripe avec le numéro de carte.
 * Ni le numéro, ni la date, ni le cryptogramme n'atteignent l'application, ne
 * sont journalisés, ni ne pourraient fuiter d'une base compromise. C'est aussi
 * ce qui met le service hors du champ le plus lourd de la norme PCI DSS.</p>
 *
 * <h2>Les montants sont en centimes</h2>
 *
 * <p>Stripe compte en plus petite unité monétaire. Neuf euros valent
 * {@code 900}. Envoyer {@code 9} facturerait neuf centimes, et la conversion
 * est le genre d'erreur qui ne se voit qu'au relevé — d'où la conversion en un
 * seul endroit, avec un arrondi explicite.</p>
 *
 * <h2>Pourquoi le résultat n'est pas cru sur parole</h2>
 *
 * <p>La confirmation vient du navigateur, c'est-à-dire de la personne qui paie.
 * Un paiement n'est donc jamais marqué acquis parce que le client l'affirme :
 * il l'est parce que la notification signée de Stripe le dit
 * ({@code payment_intent.succeeded}), ou parce que le serveur a lui-même
 * interrogé Stripe. {@link #etat(String)} est ce second chemin, et il existe
 * parce qu'un poste de développement n'a pas d'adresse publique où recevoir
 * les notifications.</p>
 */
@Slf4j
public class StripeGateway implements PaymentGateway {

    private final StripeClient stripe;

    public StripeGateway(String cleSecrete) {
        this.stripe = StripeClient.builder().setApiKey(cleSecrete).build();
    }

    @Override
    public String nom() {
        return "STRIPE";
    }

    /** Montant en plus petite unité monétaire, telle que Stripe la compte. */
    private static long centimes(BigDecimal montant) {
        return montant.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    @Override
    public Intention preparer(Payment paiement) {
        try {
            PaymentIntent intention = stripe.paymentIntents().create(
                    PaymentIntentCreateParams.builder()
                            .setAmount(centimes(paiement.getAmount()))
                            .setCurrency(paiement.getCurrency().toLowerCase())
                            /* Stripe choisit les moyens de paiement disponibles
                               selon le pays et la devise, plutôt que de figer
                               « carte » ici : Bancontact est le moyen le plus
                               utilisé en Belgique, et le coder en dur
                               l'exclurait. */
                            .setAutomaticPaymentMethods(
                                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                            .setEnabled(true)
                                            .build())
                            /* Ce qui apparaîtra sur le relevé du passager. Sans
                               cela, il y lirait un identifiant technique et ne
                               reconnaîtrait pas la ligne. */
                            .setDescription("CoShift — participation aux frais, réservation "
                                    + paiement.getBooking().getUuid())
                            /* Rattache l'opération à la réservation des deux
                               côtés : c'est ce qui permet de rapprocher une
                               ligne du relevé Stripe d'une ligne d'ici, et ce
                               que la notification renverra. */
                            .putMetadata("bookingUuid", paiement.getBooking().getUuid())
                            .putMetadata("paymentUuid", paiement.getUuid())
                            .build());

            log.info("Intention de paiement Stripe {} créée pour la réservation {} ({} {})",
                    intention.getId(), paiement.getBooking().getUuid(),
                    paiement.getAmount(), paiement.getCurrency());

            /* `false` : rien n'est acquis. Le navigateur doit encore confirmer,
               et c'est Stripe qui dira si cela a abouti. */
            return new Intention(intention.getId(), intention.getClientSecret(), false);

        } catch (StripeException e) {
            /* Le message de Stripe est destiné au développeur et peut contenir
               des détails de configuration. Il va au journal, pas à la personne
               qui paie, qui reçoit un message du catalogue. */
            log.error("Stripe a refusé la création de l'intention pour la réservation {} : {}",
                    paiement.getBooking().getUuid(), e.getMessage());
            throw new PaymentGatewayException(e);
        }
    }

    @Override
    public EtatDistant etat(String reference) {
        try {
            PaymentIntent intention = stripe.paymentIntents().retrieve(reference);
            return switch (intention.getStatus()) {
                case "succeeded" -> EtatDistant.REGLE;
                case "canceled" -> EtatDistant.ECHOUE;
                /* `requires_payment_method` après un échec de carte, mais aussi
                   avant toute tentative : les deux se ressemblent vus d'ici, et
                   « en attente » est la lecture prudente — un paiement n'est
                   jamais déclaré échoué alors qu'il n'a pas été tenté. */
                default -> EtatDistant.EN_ATTENTE;
            };
        } catch (StripeException e) {
            log.error("Stripe n'a pas pu être interrogé sur l'intention {} : {}", reference, e.getMessage());
            throw new PaymentGatewayException(e);
        }
    }

    @Override
    public String rembourser(Payment paiement, BigDecimal montant) {
        try {
            Refund remboursement = stripe.refunds().create(
                    RefundCreateParams.builder()
                            .setPaymentIntent(paiement.getProviderReference())
                            /* Le montant, et non le remboursement total par
                               défaut : le barème d'annulation rend parfois la
                               moitié, et Stripe rembourserait tout si on se
                               taisait. */
                            .setAmount(centimes(montant))
                            .build());

            log.info("Remboursement Stripe {} de {} {} sur la réservation {}",
                    remboursement.getId(), montant, paiement.getCurrency(),
                    paiement.getBooking().getUuid());
            return remboursement.getId();

        } catch (StripeException e) {
            log.error("Stripe a refusé le remboursement de la réservation {} : {}",
                    paiement.getBooking().getUuid(), e.getMessage());
            throw new PaymentGatewayException(e);
        }
    }

    /**
     * Panne du prestataire.
     *
     * <p>Une exception distincte plutôt que la remontée telle quelle de celle de
     * Stripe : elle marque la frontière entre ce dont l'application répond et ce
     * dont elle ne répond pas, et permet au gestionnaire d'erreurs de renvoyer
     * un message compréhensible plutôt qu'un vocabulaire de prestataire.</p>
     */
    public static class PaymentGatewayException extends RuntimeException {
        public PaymentGatewayException(Throwable cause) {
            super(cause);
        }
    }
}
