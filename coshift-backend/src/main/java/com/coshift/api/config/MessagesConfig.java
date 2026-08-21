package com.coshift.api.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Langue des réponses du serveur.
 *
 * <h2>Le problème</h2>
 *
 * <p>Les messages du serveur étaient écrits en français dans le code —
 * quatre-vingt-neuf chaînes réparties entre les services, les contrôleurs et
 * les annotations de validation. Une interface traduite en anglais affichait
 * donc « Un compte existe déjà avec cet email » à la première erreur, ce qui
 * annule le bénéfice de la traduction là où elle compte le plus : au moment où
 * quelque chose ne marche pas.</p>
 *
 * <h2>Comment la langue est déterminée</h2>
 *
 * <p>Par l'en-tête {@code Accept-Language} de la requête. L'interface le pose
 * explicitement à partir du choix de la personne, et non de la configuration
 * de son navigateur : quelqu'un dont le système est en néerlandais mais qui a
 * demandé l'anglais doit recevoir l'anglais.</p>
 *
 * <p>Le français reste la langue par défaut, y compris pour une requête sans
 * en-tête — appel direct à l'API, script, outil de test.</p>
 */
@Configuration
public class MessagesConfig {

    /**
     * Catalogue des messages.
     *
     * <p>{@code ReloadableResourceBundleMessageSource} plutôt que la variante
     * non rechargeable : en développement, corriger une formulation ne demande
     * pas de redémarrer.</p>
     *
     * <p>L'encodage est déclaré explicitement. Sans lui, Spring lit les
     * fichiers de propriétés en ISO-8859-1 et chaque accent devient un
     * caractère de remplacement — le défaut ne se voit qu'à l'exécution, dans
     * la réponse envoyée au client.</p>
     */
    @Bean
    public MessageSource messageSource() {
        var source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:messages");
        source.setDefaultEncoding(StandardCharsets.UTF_8.name());
        /* Une clé absente du catalogue lève une exception plutôt que de
           renvoyer la clé elle-même : un message manquant doit se voir en
           test, pas s'afficher tel quel à un utilisateur. */
        source.setUseCodeAsDefaultMessage(false);
        source.setFallbackToSystemLocale(false);
        return source;
    }

    /**
     * Résolution de la langue à partir de l'en-tête de la requête.
     *
     * <p>La liste des langues acceptées est restreinte à celles que CoShift
     * sert réellement. Sans cette restriction, un navigateur annonçant
     * {@code de-DE} obtiendrait la locale allemande, pour laquelle aucun
     * catalogue n'existe.</p>
     */
    @Bean
    public LocaleResolver localeResolver() {
        var resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.FRENCH);
        resolver.setSupportedLocales(List.of(Locale.FRENCH, Locale.ENGLISH));
        return resolver;
    }

    /**
     * Branche la validation sur le même catalogue.
     *
     * <p>Sans cela, les messages des annotations {@code @NotBlank},
     * {@code @Email} et consorts resteraient les chaînes littérales écrites
     * dans les DTO. Ils s'expriment désormais en clés — {@code {validation.email.requis}} —
     * qui sont résolues ici, dans la langue de la requête.</p>
     */
    @Bean
    public LocalValidatorFactoryBean getValidator(MessageSource messageSource) {
        var validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messageSource);
        return validator;
    }
}
