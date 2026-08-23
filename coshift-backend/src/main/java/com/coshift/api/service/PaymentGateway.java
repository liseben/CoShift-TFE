package com.coshift.api.service;

import com.coshift.api.entity.Payment;

import java.math.BigDecimal;

/**
 * Le prestataire qui fait circuler l'argent.
 *
 * <h2>Pourquoi une interface</h2>
 *
 * <p>Les règles du partage de frais — ce qui est dû, ce qui est rendu, selon
 * quel barème — sont un choix de produit et se testent. Le mouvement de fonds
 * est une affaire de prestataire, avec ses clés, son réseau et ses pannes. Les
 * mêler aurait rendu le barème impossible à éprouver sans compte chez un tiers.</p>
 *
 * <h2>Deux temps, et non un</h2>
 *
 * <p>La première version de ce port avait une seule méthode, {@code encaisser},
 * qui rendait une référence : un règlement en un appel. C'était la forme de la
 * simulation, pas celle d'un vrai prestataire. Chez Stripe, le serveur
 * <em>prépare</em> une intention de paiement, le navigateur la confirme avec
 * les coordonnées bancaires — qui ne transitent jamais par CoShift — et le
 * résultat revient par une notification signée. Le port a donc été refait pour
 * décrire ce cycle plutôt que celui de son bouchon.</p>
 *
 * <p>{@link Intention#regleImmediatement()} distingue les deux mondes : la
 * simulation dit « c'est fait », Stripe dit « à confirmer ». Le service n'a pas
 * à savoir lequel des deux répond.</p>
 */
public interface PaymentGateway {

    /** Nom du prestataire, consigné avec chaque opération. */
    String nom();

    /**
     * Ce que le prestataire répond quand on lui annonce un paiement.
     *
     * @param reference          identifiant de l'opération chez lui
     * @param secretClient       jeton que le navigateur présente pour confirmer,
     *                           ou {@code null} si rien n'est à confirmer.
     *                           Il n'est jamais conservé en base : c'est un
     *                           laissez-passer à usage unique, pas une donnée.
     * @param regleImmediatement vrai si le paiement est acquis dès maintenant
     */
    record Intention(String reference, String secretClient, boolean regleImmediatement) {}

    /** État d'une opération, tel que le prestataire le voit. */
    enum EtatDistant { REGLE, EN_ATTENTE, ECHOUE }

    /**
     * Annonce le paiement au prestataire.
     *
     * <p>Ne prélève rien par elle-même chez un vrai prestataire : elle réserve
     * l'opération et rend de quoi la confirmer.</p>
     */
    Intention preparer(Payment paiement);

    /**
     * Demande au prestataire où en est une opération.
     *
     * <p>C'est le recours quand la notification n'arrive pas — un réseau local
     * sans adresse publique, par exemple. Interroger vaut mieux que croire le
     * navigateur sur parole : lui seul est entre les mains de la personne qui
     * paie.</p>
     */
    EtatDistant etat(String reference);

    /**
     * Rend tout ou partie du montant.
     *
     * @return la référence de l'opération de remboursement
     */
    String rembourser(Payment paiement, BigDecimal montant);
}
