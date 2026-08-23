package com.coshift.api.security;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Journal des événements de sécurité.
 *
 * <h2>Pourquoi un journal séparé</h2>
 *
 * <p>Une détection d'intrusion ne commence pas par un outil, elle commence par
 * une trace. Avant ce service, CoShift refusait correctement les accès
 * illégitimes mais n'en gardait presque rien : neuf contrôles de propriété
 * levaient une exception, deux seulement laissaient une ligne. Un compte
 * méthodiquement sondé ne se distinguait donc pas d'un utilisateur maladroit.</p>
 *
 * <p>Les événements partent sur un enregistreur dédié, {@code SECURITE}, que la
 * configuration route vers son propre fichier. Les mêlant au journal applicatif,
 * ils se noieraient sous les lignes d'aspiration d'articles et de démarrage.</p>
 *
 * <h2>Format</h2>
 *
 * <p>Une ligne par événement, en couples {@code cle=valeur} : lisible par un
 * humain, analysable par un {@code grep} ou par un collecteur de journaux, sans
 * analyseur dédié.</p>
 *
 * <h2>Données personnelles</h2>
 *
 * <p>Le journal contient une adresse électronique et une adresse IP, deux
 * données personnelles. Leur consignation relève de l'intérêt légitime à
 * sécuriser le service (RGPD, article 6.1.f) : sans identifiant, une trace ne
 * permet ni de constater une attaque ciblée ni d'avertir la personne visée. En
 * contrepartie, la conservation est limitée à douze mois et <strong>aucun mot de
 * passe, jeton ou code n'est jamais écrit</strong>, même tronqué.</p>
 */
@Service
@Slf4j
public class SecurityAuditService {

    /** Enregistreur dédié, routé vers son propre fichier par la configuration. */
    private static final Logger SECURITE = LoggerFactory.getLogger("SECURITE");

    /** Nature des événements consignés. */
    public enum Evenement {
        /** Identifiants refusés. */
        CONNEXION_ECHOUEE,
        /** Connexion aboutie. */
        CONNEXION_REUSSIE,
        /** Seuil de tentatives atteint : la clé est bloquée. */
        BLOCAGE_TENTATIVES,
        /** Appel sur une clé déjà bloquée — signe d'un acharnement. */
        TENTATIVE_PENDANT_BLOCAGE,
        /** Code de vérification ou de réinitialisation erroné. */
        CODE_INVALIDE,
        /** Accès refusé à une ressource appartenant à autrui. */
        ACCES_REFUSE,
        /** Mot de passe modifié par le parcours de réinitialisation. */
        MOT_DE_PASSE_REINITIALISE,
        /** Adresse électronique modifiée : le compte repasse en attente. */
        ADRESSE_MODIFIEE,
        /** Connexion refusée faute d'adresse vérifiée. */
        COMPTE_NON_ACTIVE,
        /**
         * Compte effacé à la demande de son titulaire — RGPD, article 17.
         *
         * <p>L'acteur consigné est l'identifiant technique du compte, jamais son
         * adresse : conserver l'adresse d'un compte effacé viderait l'effacement
         * de son objet.</p>
         */
        COMPTE_EFFACE,
        /** Compte purgé par la tâche de rétention, sans demande — RGPD, article 5.1.e. */
        COMPTE_PURGE,
        /**
         * Compte suspendu par la modération.
         *
         * <p>Le détail porte qui a pris la décision. Une mesure de modération se
         * conteste : savoir qui l'a prise, et quand, fait partie de ce qu'on doit
         * pouvoir produire.</p>
         */
        COMPTE_SUSPENDU,
        /** Suspension levée. Consignée aussi : une décision annulée reste une décision. */
        COMPTE_REACTIVE,
        /** Connexion refusée sur un compte suspendu, mot de passe pourtant correct. */
        CONNEXION_COMPTE_SUSPENDU
    }

    /**
     * Consigne un événement.
     *
     * @param evenement nature de l'événement
     * @param acteur    compte concerné, ou {@code null} s'il est inconnu
     * @param ip        adresse de l'appelant
     * @param detail    précision libre, sans donnée secrète
     */
    public void consigner(Evenement evenement, String acteur, String ip, String detail) {
        SECURITE.warn("evenement={} compte={} ip={} detail={}",
                evenement, valeur(acteur), valeur(ip), valeur(detail));
    }

    public void consigner(Evenement evenement, String acteur, String ip) {
        consigner(evenement, acteur, ip, "-");
    }

    /**
     * Neutralise ce qui casserait le format {@code cle=valeur} : un saut de
     * ligne dans une valeur permettrait d'injecter une fausse ligne dans le
     * journal, et donc de masquer un événement réel derrière un événement
     * inventé.
     */
    private String valeur(String v) {
        if (v == null || v.isBlank()) return "-";
        return v.replaceAll("[\\r\\n\\t]", " ").trim();
    }
}
