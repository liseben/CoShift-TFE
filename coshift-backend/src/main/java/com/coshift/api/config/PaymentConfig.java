package com.coshift.api.config;

import com.coshift.api.service.PaymentGateway;
import com.coshift.api.service.SimulationGateway;
import com.coshift.api.service.StripeGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Choix du prestataire de paiement.
 *
 * <h2>Une seule décision, prise au démarrage</h2>
 *
 * <p>Une clé secrète utilisable est configurée : Stripe. Sinon : la simulation.
 * Le choix est fait ici et nulle part ailleurs, si bien qu'aucun service n'a à
 * demander « suis-je en mode réel ? » — question qui, posée en dix endroits,
 * finit par recevoir dix réponses différentes.</p>
 *
 * <h2>Pourquoi la simulation reste</h2>
 *
 * <p>Sans elle, l'application refuserait de démarrer chez quiconque clone le
 * dépôt sans compte Stripe — un membre du jury, par exemple. Elle refuserait
 * aussi de démarrer en intégration continue. Une dépendance à un service
 * extérieur pour <em>lancer</em> le programme est une dépendance de trop.</p>
 *
 * <h2>Le marque-place compte comme une absence</h2>
 *
 * <p>{@code .env.example} livre {@code sk_test_REMPLACE_PAR_...}. Cette valeur
 * est présente mais inutilisable : la traiter comme une clé ferait échouer le
 * premier paiement avec une erreur d'authentification Stripe, message
 * incompréhensible pour qui n'a jamais ouvert de compte. Elle est donc
 * reconnue et écartée, et le journal dit lequel des deux prestataires a été
 * retenu — c'est la première chose à vérifier quand un paiement se comporte
 * autrement qu'attendu.</p>
 */
@Configuration
@Slf4j
public class PaymentConfig {

    /** Vide par défaut : l'absence de clé est un cas normal, pas une erreur de configuration. */
    @Value("${stripe.secret-key:}")
    private String cleSecrete;

    @Bean
    public PaymentGateway paymentGateway() {
        if (!cleUtilisable()) {
            log.warn("Aucune clé Stripe utilisable : les paiements passent par la SIMULATION. "
                    + "Aucun euro ne circulera. Renseignez STRIPE_SECRET_KEY dans .env pour "
                    + "activer Stripe en mode test.");
            return new SimulationGateway();
        }

        /* Le préfixe est le seul indice fiable, et il vaut d'être vérifié : une
           clé `sk_live_` collée par mégarde ferait circuler de l'argent réel
           depuis un poste de développement. On ne l'interdit pas — c'est une
           décision d'exploitation — mais on la signale fort. */
        if (cleSecrete.startsWith("sk_live_")) {
            log.warn("Clé Stripe de PRODUCTION détectée : les paiements seront réels.");
        } else {
            log.info("Stripe activé en mode test. Carte d'essai : 4242 4242 4242 4242, "
                    + "date future, cryptogramme quelconque.");
        }
        return new StripeGateway(cleSecrete);
    }

    private boolean cleUtilisable() {
        return cleSecrete != null
                && !cleSecrete.isBlank()
                && !cleSecrete.contains("REMPLACE")
                && cleSecrete.startsWith("sk_");
    }
}
