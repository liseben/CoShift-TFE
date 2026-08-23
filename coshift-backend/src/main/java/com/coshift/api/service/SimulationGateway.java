package com.coshift.api.service;

import com.coshift.api.entity.Payment;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Prestataire de démonstration.
 *
 * <h2>Ce qu'il fait, et pourquoi il porte ce nom</h2>
 *
 * <p>Il ne déplace rien. Il attribue une référence et déclare le paiement
 * acquis, ce qui permet d'éprouver la totalité des règles — barème, états,
 * remboursements partiels — sans compte chez un prestataire, et de faire
 * tourner l'application à qui clone le dépôt sans clé.</p>
 *
 * <p>Il s'appelle {@code SIMULATION} et non {@code DEFAULT} : le nom est
 * enregistré avec chaque opération et affiché à l'écran. Un nom neutre
 * laisserait croire, en relisant la base dans six mois, que de l'argent a
 * circulé.</p>
 */
@Slf4j
public class SimulationGateway implements PaymentGateway {

    @Override
    public String nom() {
        return "SIMULATION";
    }

    @Override
    public Intention preparer(Payment paiement) {
        String reference = "sim_" + UUID.randomUUID().toString().substring(0, 12);
        log.info("Règlement simulé de {} {} pour la réservation {} — référence {}",
                paiement.getAmount(), paiement.getCurrency(),
                paiement.getBooking().getUuid(), reference);
        /* Rien à confirmer : pas de secret client, et le paiement est acquis
           dès maintenant. C'est la seule différence de forme avec un vrai
           prestataire, et le service la traite sans savoir lequel répond. */
        return new Intention(reference, null, true);
    }

    @Override
    public EtatDistant etat(String reference) {
        return EtatDistant.REGLE;
    }

    @Override
    public String rembourser(Payment paiement, BigDecimal montant) {
        String reference = "simr_" + UUID.randomUUID().toString().substring(0, 12);
        log.info("Remboursement simulé de {} {} sur la réservation {} — référence {}",
                montant, paiement.getCurrency(), paiement.getBooking().getUuid(), reference);
        return reference;
    }
}
