package com.coshift.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * Description de l'API servie à Swagger UI et à la spécification OpenAPI.
 *
 * <p>La spécification est <em>dérivée du code</em> : springdoc lit les
 * contrôleurs, les DTO et leurs contraintes de validation. Une documentation
 * rédigée à part finit toujours par mentir sur au moins un endpoint ; celle-ci
 * ne le peut pas, puisqu'elle est régénérée à chaque démarrage.</p>
 *
 * <p>La classe est désactivée sous le profil {@code prod} : publier la carte
 * complète de l'API à un attaquant lui épargne la phase de reconnaissance.
 * Les propriétés {@code springdoc.*.enabled} d'{@code application-prod.properties}
 * coupent en plus le service des routes elles-mêmes — la ceinture et les
 * bretelles, l'une ne remplaçant pas l'autre.</p>
 */
@Configuration
@Profile("!prod")
public class OpenApiConfig {

    /** Schéma de sécurité déclaré une fois et appliqué à toute l'API. */
    private static final String JWT = "jetonJWT";

    @Value("${app.api.version}")
    private String versionApi;

    @Value("${app.base-url}")
    private String urlLocale;

    @Bean
    public OpenAPI coshiftOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API CoShift")
                        .version(versionApi)
                        .description(description())
                        .contact(new Contact()
                                .name("Élisabeth Benga — travail de fin d'études 2025-2026")
                                .email("lisait417@gmail.com"))
                        .license(new License()
                                .name("Données ouvertes : Licence Ouverte / Open Licence 2.0 (Etalab)")
                                .url("https://www.etalab.gouv.fr/licence-ouverte-open-licence/")))
                .servers(List.of(
                        new Server().url(urlLocale).description("Poste de développement"),
                        new Server().url("https://api.coshift.be").description("Production (non déployée à ce jour)")))
                .tags(List.of(
                        new Tag().name("Authentification")
                                .description("Inscription, connexion, vérification de l'adresse et réinitialisation du mot de passe."),
                        new Tag().name("Trajets")
                                .description("Publication, recherche, consultation et annulation des trajets."),
                        new Tag().name("Réservations")
                                .description("Demandes de place, décisions du conducteur, suivi côté passager."),
                        new Tag().name("Véhicules")
                                .description("Véhicules déclarés par un conducteur."),
                        new Tag().name("Utilisateurs")
                                .description("Profil du membre connecté et photo de profil."),
                        new Tag().name("Actualités")
                                .description("Flux d'articles sur la mobilité. Accessible sans authentification."),
                        new Tag().name("Données ouvertes")
                                .description("Statistiques agrégées de mobilité, publiques et réutilisables.")))
                .components(new Components().addSecuritySchemes(JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("""
                                Jeton obtenu par POST /api/auth/login.
                                Coller la valeur brute : Swagger UI ajoute lui-même le préfixe « Bearer ».""")))
                // Exigence appliquée globalement ; les routes publiques la lèvent
                // par @SecurityRequirements sur leur contrôleur.
                .addSecurityItem(new SecurityRequirement().addList(JWT));
    }

    private String description() {
        return """
                API REST de **CoShift**, plateforme de covoiturage à destination des
                entreprises, hautes écoles et organisateurs d'événements.

                ### Ce qu'il faut savoir avant d'appeler

                - Toutes les réponses sont en `application/json`, hormis les images servies sous `/uploads`.
                - Les ressources sont désignées par un **UUID** et jamais par leur identifiant
                  de base de données : un identifiant séquentiel se devine, et permettrait
                  d'énumérer les trajets ou les membres.
                - Sauf mention contraire, un endpoint exige un jeton JWT valide dans
                  l'en-tête `Authorization`.
                - Les erreurs suivent toutes la même forme, décrite par le schéma `ErrorResponse`.

                ### Pour essayer depuis cette page

                1. Appeler `POST /api/auth/login` avec un compte de démonstration.
                2. Copier la valeur du champ `token` de la réponse.
                3. Cliquer **Authorize** en haut à droite et la coller.
                4. Les appels suivants portent le jeton automatiquement.

                ### Données ouvertes

                Les endpoints `/api/open-data/**` sont **publics et sans jeton**. Ils ne
                servent que des agrégats : aucune donnée personnelle, et tout regroupement
                comptant moins de cinq trajets est écarté avant publication.
                """;
    }
}
