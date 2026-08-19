package com.coshift.api.config;

import com.coshift.api.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
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
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Autoriser spécifiquement ton React (modifie le port si ce n'est pas 5173)
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}