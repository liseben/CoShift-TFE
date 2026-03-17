package com.coshift.api.service;

import com.coshift.api.dto.AuthenticationResponse;
import com.coshift.api.dto.LoginRequest;
import com.coshift.api.dto.RegisterRequest; // NOUVEAU
import com.coshift.api.entity.Role; // NOUVEAU
import com.coshift.api.entity.User; // NOUVEAU
import com.coshift.api.repository.UserRepository;
import com.coshift.api.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder; // NOUVEAU
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository repository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder; // Injection du bean pour hasher le mot de passe

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