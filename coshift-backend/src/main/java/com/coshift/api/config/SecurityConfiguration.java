package com.coshift.api.config;

import com.coshift.api.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Désactiver CSRF (inutile pour les API REST stateless)
            .csrf(csrf -> csrf.disable())
            
            // 2. Configuration des accès URL
            .authorizeHttpRequests(auth -> auth
                // Les routes d'authentification sont publiques (Inscription, Login)
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/pwa/articles").permitAll()
                // Tout le reste nécessite d'être connecté
                .anyRequest().authenticated()
            )
            
            // 3. Gestion de session : Stateless (Pas de session serveur, tout est dans le Token)
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 4. Quel fournisseur d'auth utiliser (celui qu'on a créé dans ApplicationConfig)
            .authenticationProvider(authenticationProvider)
            
            // 5. Ajouter notre filtre JWT avant le filtre classique Username/Password
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}