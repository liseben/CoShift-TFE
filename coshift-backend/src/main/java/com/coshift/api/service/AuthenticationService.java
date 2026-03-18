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
import java.util.UUID;

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

                // 3. Extraire les infos fournies par Google
                String email = payload.getEmail();
                String firstName = (String) payload.get("given_name");
                String lastName = (String) payload.get("family_name");
                String pictureUrl = (String) payload.get("picture");

                // 4. Chercher l'utilisateur ou le CRÉER à la volée s'il n'existe pas
                User user = repository.findByEmail(email).orElseGet(() -> {
                    log.info("Création d'un nouveau compte via Google pour : {}", email);
                    User newUser = User.builder()
                            .firstname(firstName != null ? firstName : "Utilisateur")
                            .lastname(lastName != null ? lastName : "Google")
                            .email(email)
                            .pictureUrl(pictureUrl)
                            .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                            .role(Role.USER)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return repository.save(newUser);
                });

                if (pictureUrl != null && !pictureUrl.equals(user.getPictureUrl())) {
                    user.setPictureUrl(pictureUrl);
                    repository.save(user);
                }

                // 5. Générer NOTRE Token JWT CoShift
                var jwtToken = jwtService.generateToken(user);

                return AuthenticationResponse.builder()
                        .token(jwtToken)
                        .message("Connexion Google réussie")
                        .build();

            } else {
                throw new RuntimeException("Le Token Google est invalide.");
            }
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