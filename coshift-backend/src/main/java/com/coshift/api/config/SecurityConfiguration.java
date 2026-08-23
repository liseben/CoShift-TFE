package com.coshift.api.config;

import com.coshift.api.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
/* Active @PreAuthorize. Sans elle, l'annotation est simplement ignoree :
   les methodes protegees s'executeraient pour tout le monde, en silence. */
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Activer CORS dans Spring Security (va utiliser le Bean corsConfigurationSource ci-dessous)
            .cors(Customizer.withDefaults())
            
            // 2. Désactiver CSRF (inutile pour les API REST stateless)
            .csrf(csrf -> csrf.disable())
            
            // 3. Configuration des accès URL
            .authorizeHttpRequests(auth -> auth
                // LA SOLUTION EST ICI : Autoriser toutes les requêtes OPTIONS (Preflight)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // Routes publiques (auth + vérification + articles publics)
                .requestMatchers("/api/auth/**").permitAll()
                // Le flux d'actualités est public, liste ET détail d'un article.
                // Sans le second motif, la page /actus/{id} renvoyait 401 à un
                // visiteur non connecté alors que la liste s'affichait.
                .requestMatchers(HttpMethod.GET, "/api/pwa/articles").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/pwa/articles/**").permitAll()
                // Photo de profil accessible sans auth (images publiques)
                .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()

                // Données ouvertes : agrégats sans donnée personnelle, dont la
                // réutilisation libre est justement l'objet.
                .requestMatchers(HttpMethod.GET, "/api/open-data/**").permitAll()

                /* Le blog se lit sans compte : un blog qui exige une inscription
                   pour etre lu n'est pas un blog. Seul le GET est ouvert — ecrire
                   passe par @PreAuthorize sur le controleur. La regle est bornee
                   aux deux chemins de lecture, et non a /api/blog/** en GET, pour
                   que la liste d'administration reste fermee. */
                /* Stripe appelle ce chemin sans pouvoir s'authentifier : il ne
                   detient aucun compte CoShift. Ce n'est pas un trou — la
                   requete est verifiee par sa SIGNATURE, calculee avec un secret
                   partage que personne d'autre ne possede, et le controleur
                   refuse tout ce qui ne la porte pas. Une authentification par
                   jeton serait ici impossible ET moins sure. */
                .requestMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()

                .requestMatchers(HttpMethod.GET, "/api/blog").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/blog/*").permitAll()

                // Plan du site : destiné aux moteurs, qui ne présentent pas de
                // jeton. Une adresse protégée serait simplement ignorée.
                .requestMatchers(HttpMethod.GET, "/sitemap.xml").permitAll()

                // Documentation de l'API. Ouverte en développement pour être
                // utilisable, éteinte en production par le profil « prod » :
                // ces routes ne répondent alors plus du tout.
                .requestMatchers(
                        "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml",
                        "/swagger-ui.html", "/swagger-ui/**").permitAll()

                // Sondes de disponibilité, interrogées par la supervision et,
                // en production, par l'orchestrateur : /health/liveness et
                // /health/readiness doivent donc rester joignables sans jeton.
                // Le niveau de détail est réglé par management.endpoint.health
                // .show-details, restreint aux appelants authentifiés en
                // production. Les métriques, elles, restent réservées à un
                // administrateur : elles renseignent sur l'infrastructure.
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")

                // Tout le reste nécessite d'être connecté
                .anyRequest().authenticated()
            )
            
            // 3 bis. En-têtes de sécurité.
            // Spring Security pose déjà X-Content-Type-Options et X-Frame-Options ;
            // les trois suivants ne sont pas fournis par défaut.
            .headers(headers -> headers
                    // L'API ne renvoie que du JSON et des images : elle n'a aucune
                    // raison de charger un script, un style ou une police. Une
                    // politique fermée neutralise l'exploitation d'une éventuelle
                    // injection dans une réponse servie au navigateur.
                    .contentSecurityPolicy(csp -> csp.policyDirectives(
                            "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'"))
                    // Ne pas divulguer l'URL d'origine à un site tiers : un chemin
                    // peut contenir un identifiant de ressource.
                    .referrerPolicy(referrer -> referrer.policy(
                            ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                    // HSTS : le navigateur refusera ensuite tout retour en HTTP clair.
                    // L'en-tête n'est émis que sur une requête déjà sécurisée, donc
                    // sans effet en développement.
                    .httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true)
                            .maxAgeInSeconds(31_536_000)))

            // 4. Gestion de session : Stateless
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 5. Fournisseur et Filtre
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // --- LA CONFIGURATION CORS GLOBALE ---
    // Seule autorité en matière de CORS : c'est ce filtre qui s'applique, avant
    // même que la requête n'atteigne un contrôleur. Les annotations
    // @CrossOrigin qui doublaient cette configuration ont été retirées.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        /* Origines lues dans application.properties, surchargeables par
           CORS_ALLOWED_ORIGINS : les mettre à jour ne demande pas de recompiler.

           setAllowedOriginPatterns et non setAllowedOrigins : la première
           accepte des motifs, la seconde exige des adresses exactes. La
           différence s'est payée en développement — la liste ne contenait que
           le port 5173, et un serveur Vite démarré alors que ce port était pris
           bascule tout seul sur 5174 ou 5175. Toutes ses requêtes se voyaient
           alors refusées en 403 avant d'atteindre le moindre contrôleur, et
           l'interface s'affichait vide sans un message d'erreur : le navigateur
           écarte silencieusement une réponse sans en-tête Access-Control.

           Un motif exact reste un motif valable : la valeur de production,
           https://coshift.be, continue de désigner cette seule origine. */
        configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}