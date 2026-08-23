package com.coshift.api.entity;

/**
 * Rubrique d'un billet.
 *
 * <p>Une énumération et non une chaîne libre : la rubrique est affichée avec
 * une couleur et un libellé traduit, et les deux vivent dans l'interface. Une
 * valeur inventée à la saisie y produirait une pastille sans nom ni teinte.</p>
 */
public enum BlogCategory {
    /** Ce que le produit fait et pourquoi. */
    PRODUIT,
    /** Ce qui est fait des données personnelles. */
    CONFIDENTIALITE,
    /** Données ouvertes, code, transparence. */
    OUVERTURE,
    /** Décisions de conception et leurs raisons. */
    CONCEPTION
}
