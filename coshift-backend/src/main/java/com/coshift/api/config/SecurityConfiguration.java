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
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
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
        // Origines lues dans application.properties, surchargeables par
        // CORS_ALLOWED_ORIGINS : les mettre à jour ne demande plus de recompiler.
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}