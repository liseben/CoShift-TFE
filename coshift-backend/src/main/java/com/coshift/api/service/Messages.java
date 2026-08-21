package com.coshift.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Traduit une clé dans la langue de la requête en cours.
 *
 * <h2>Pourquoi passer par un service plutôt qu'injecter MessageSource partout</h2>
 *
 * <p>{@code MessageSource.getMessage} demande une {@link Locale} à chaque
 * appel. La faire circuler dans toutes les signatures — des contrôleurs
 * jusqu'aux services et à leurs méthodes privées — aurait pollué une trentaine
 * de méthodes pour transporter une information que Spring tient déjà.</p>
 *
 * <p>{@link LocaleContextHolder} la porte dans une variable liée au fil
 * d'exécution, renseignée par le résolveur de langue avant que le contrôleur
 * soit appelé. Ce service en fait la seule dépendance visible.</p>
 *
 * <h2>La limite de cette approche</h2>
 *
 * <p>Une tâche planifiée ne s'exécute pas dans le fil d'une requête : le
 * contexte y est vide et la langue retombe sur le défaut. C'est le comportement
 * voulu — un message écrit au journal n'a pas de destinataire humain
 * identifié — mais cela signifie qu'un courriel envoyé depuis une tâche
 * planifiée partirait en français. Les envois actuels partent tous du fil
 * d'une requête, {@link EmailService} recevant explicitement la langue.</p>
 */
@Service
@RequiredArgsConstructor
public class Messages {

    private final MessageSource messageSource;

    /** Traduit une clé, avec ses éventuels paramètres, dans la langue courante. */
    public String get(String cle, Object... parametres) {
        return messageSource.getMessage(cle, parametres, LocaleContextHolder.getLocale());
    }

    /** Traduit une clé dans une langue imposée — utile pour un courriel. */
    public String get(Locale langue, String cle, Object... parametres) {
        return messageSource.getMessage(cle, parametres, langue);
    }

    /** Langue de la requête en cours, pour la transmettre à un envoi différé. */
    public Locale langueCourante() {
        return LocaleContextHolder.getLocale();
    }
}
