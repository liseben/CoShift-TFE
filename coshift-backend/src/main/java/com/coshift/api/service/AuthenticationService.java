package com.coshift.api.service;

import com.coshift.api.dto.AuthenticationResponse;
import com.coshift.api.dto.GoogleLoginRequest;
import com.coshift.api.dto.LoginRequest;
import com.coshift.api.dto.RegisterRequest; 
import com.coshift.api.entity.Role; 
import com.coshift.api.entity.User; 
import com.coshift.api.repository.UserRepository;
import com.coshift.api.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import java.util.Collections;
import com.coshift.api.exception.UnauthorizedException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository repository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    @Value("${api.google.client-id}")
    private String googleClientId;

    // --- LA NOUVELLE MÉTHODE GOOGLE ---
    // --- LA NOUVELLE MÉTHODE GOOGLE ---
    public AuthenticationResponse authenticateWithGoogle(GoogleLoginRequest request) {
        // 1. Initialiser le vérificateur Google
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        try {
            // 2. Vérifier la validité du Token
            GoogleIdToken idToken = verifier.verify(request.getToken());
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();

                // 3. Chercher l'utilisateur. S'il n'existe pas, on bloque la connexion.
                User user = repository.findByEmail(email).orElseThrow(() -> {
                    log.warn("Tentative de connexion Google avec un email non inscrit : {}", email);
                    // On utilise ton exception personnalisée ici !
                    return new UnauthorizedException("Cet utilisateur n'existe pas. Veuillez créer un compte.");
                });

                // NOTE: On ne met plus à jour le `pictureUrl` avec celui de Google.
                // L'utilisateur garde ses données telles qu'elles sont en base,
                // ce qui permettra au frontend d'afficher l'icône par défaut.

                // 4. Générer NOTRE Token JWT CoShift
                var jwtToken = jwtService.generateToken(user);

                return AuthenticationResponse.builder()
                        .token(jwtToken)
                        .message("Connexion Google réussie")
                        .build();

            } else {
                throw new RuntimeException("Le Token Google est invalide.");
            }
        } catch (RuntimeException e) {
            // Permet de faire remonter notre message "Ce compte n'existe pas..." sans l'écraser
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la vérification du Token Google : ", e);
            throw new RuntimeException("Échec de l'authentification Google.");
        }
    }

    // --- LA NOUVELLE MÉTHODE D'INSCRIPTION ---
    public AuthenticationResponse register(RegisterRequest request) {
        // 1. Vérifier si l'email existe déjà
        if (repository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Un compte existe déjà avec cet email"); 
            // Idéalement, on utiliserait une exception personnalisée (ex: UserAlreadyExistsException)
        }

        // 2. Créer l'entité User (le password est haché ici !)
        var user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER) // Par défaut, c'est un utilisateur normal
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 3. Sauvegarder dans la DB
        repository.save(user);

        // 4. Générer le JWT
        var jwtToken = jwtService.generateToken(user);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .message("Inscription réussie")
                .build();
    }

    // --- LA MÉTHODE DE CONNEXION (Inchangée) ---
    public AuthenticationResponse authenticate(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = repository.findByEmail(request.getEmail())
                .orElseThrow();

        var jwtToken = jwtService.generateToken(user);
        
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .message("Connexion réussie")
                .build();
    }

    
}