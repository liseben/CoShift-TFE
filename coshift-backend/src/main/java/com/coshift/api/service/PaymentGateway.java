package com.coshift.api.service;

import com.coshift.api.entity.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Le prestataire qui fait circuler l'argent.
 *
 * <h2>Pourquoi une interface, et non un appel direct à un prestataire</h2>
 *
 * <p>Encaisser pour le compte d'un tiers relève de la directive européenne sur
 * les services de paiement et du statut d'agent de paiement. CoShift ne l'a
 * pas, et ses conditions générales le disent : « aucun paiement n'est perçu par
 * CoShift à ce jour ». Écrire les appels d'un prestataire au milieu de la
 * logique métier aurait mêlé deux choses de nature différente — les règles du
 * partage de frais, qui sont un choix de produit, et le mouvement de fonds, qui
 * est une affaire réglementée.</p>
 *
 * <p>Tout ce qui est vérifiable ici — ce qui est dû, ce qui est rendu, selon
 * quel barème — vit donc dans {@link PaymentService} et se teste. Ce qui
 * demande un agrément vit derrière cette interface, et se remplace le jour où
 * l'agrément existe, sans toucher aux règles.</p>
 *
 * <h2>Ce qu'un branchement réel demanderait</h2>
 *
 * <p>Une seconde implémentation appelant le prestataire choisi, un point
 * d'entrée pour ses notifications — l'encaissement n'est confirmé que par
 * elles, jamais par la réponse immédiate —, et une réconciliation entre les
 * références conservées ici et les lignes de son relevé. La couture est prête ;
 * l'agrément ne l'est pas.</p>
 */
public interface PaymentGateway {

    /** Nom du prestataire, consigné avec chaque opération. */
    String nom();

    /**
     * Prélève le montant.
     *
     * @return la référence de l'opération chez le prestataire
     */
    String encaisser(Payment paiement);

    /**
     * Rend tout ou partie du montant.
     *
     * @return la référence de l'opération de remboursement
     */
    String rembourser(Payment paiement, BigDecimal montant);

    /**
     * Prestataire de démonstration.
     *
     * <h2>Ce qu'il fait, et pourquoi il porte ce nom</h2>
     *
     * <p>Il ne déplace rien. Il attribue une référence et rend la main, ce qui
     * permet d'éprouver la totalité des règles — barème, états, remboursements
     * partiels — sans compte chez un prestataire.</p>
     *
     * <p>Il s'appelle {@code SIMULATION} et non {@code DEFAULT} : le nom est
     * enregistré avec chaque opération et lu par la console d'administration.
     * Un nom neutre laisserait croire, en relisant la base dans six mois, que
     * de l'argent a circulé.</p>
     */
    @Component
    @Slf4j
    class Simulation implements PaymentGateway {

        @Override
        public String nom() {
            return "SIMULATION";
        }

        @Override
        public String encaisser(Payment paiement) {
            String reference = "sim_" + UUID.randomUUID().toString().substring(0, 12);
            log.info("Encaissement simulé de {} {} pour la réservation {} — référence {}",
                    paiement.getAmount(), paiement.getCurrency(),
                    paiement.getBooking().getUuid(), reference);
            return reference;
        }

        @Override
        public String rembourser(Payment paiement, BigDecimal montant) {
            String reference = "simr_" + UUID.randomUUID().toString().substring(0, 12);
            log.info("Remboursement simulé de {} {} sur la réservation {} — référence {}",
                    montant, paiement.getCurrency(),
                    paiement.getBooking().getUuid(), reference);
            return reference;
        }
    }
}
