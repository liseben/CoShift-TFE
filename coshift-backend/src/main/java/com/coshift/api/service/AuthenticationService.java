package com.coshift.api.service;

import com.coshift.api.dto.AuthenticationResponse;
import com.coshift.api.dto.LoginRequest;
import com.coshift.api.repository.UserRepository;
import com.coshift.api.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository repository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse authenticate(LoginRequest request) {
        // 1. Spring Security vérifie si le mot de passe correspond à l'email (hash bcrypt)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // 2. Si on arrive ici, l'utilisateur existe et le mot de passe est bon
        var user = repository.findByEmail(request.getEmail())
                .orElseThrow(); // Devrait toujours être trouvé car authenticate a réussi

        // 3. On génère le token JWT
        var jwtToken = jwtService.generateToken(user);
        
        // 4. On retourne la réponse au Frontend
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .message("Connexion réussie")
                .build();
    }
}