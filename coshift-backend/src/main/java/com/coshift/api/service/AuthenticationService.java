package com.coshift.api.service;

import com.coshift.api.dto.AuthenticationResponse;
import com.coshift.api.dto.GoogleLoginRequest;
import com.coshift.api.dto.LoginRequest;
import com.coshift.api.dto.RegisterRequest;
import com.coshift.api.dto.VerifyEmailRequest;
import com.coshift.api.entity.Role;
import com.coshift.api.entity.User;
import com.coshift.api.repository.UserRepository;
import com.coshift.api.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import java.security.SecureRandom;
import java.util.Collections;
import com.coshift.api.exception.BadRequestException;
import com.coshift.api.exception.ConflictException;
import com.coshift.api.exception.ResourceNotFoundException;
import com.coshift.api.exception.UnauthorizedException;
import org.springframework.security.authentication.BadCredentialsException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository repository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${api.google.client-id}")
    private String googleClientId;

    private static final SecureRandom RANDOM = new SecureRandom();

    private String generateVerificationCode() {
        int code = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(code);
    }

    // --- CONNEXION GOOGLE OAuth2 ---
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
                throw new UnauthorizedException("Le token Google est invalide.");
            }
        } catch (RuntimeException e) {
            // Permet de faire remonter notre message "Ce compte n'existe pas..." sans l'écraser
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la vérification du Token Google : ", e);
            throw new UnauthorizedException("Échec de l'authentification Google.");
        }
    }

    // --- INSCRIPTION (F4) ---
    public AuthenticationResponse register(RegisterRequest request) {
        if (repository.findByEmail(request.getEmail()).isPresent()) {
            throw new ConflictException("Un compte existe déjà avec cet email.");
        }

        String code = generateVerificationCode();

        var user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .emailVerified(false)
                .verificationCode(code)
                .verificationCodeExpiry(LocalDateTime.now().plusHours(24))
                .build();

        repository.save(user);

        // Envoi asynchrone — ne bloque pas la réponse
        emailService.sendVerificationEmail(user.getEmail(), user.getFirstname(), code);

        return AuthenticationResponse.builder()
                .message("Compte créé ! Vérifiez votre email pour activer votre compte.")
                .build();
    }

    // --- VÉRIFICATION EMAIL (F7) ---
    public AuthenticationResponse verifyEmail(VerifyEmailRequest request) {
        User user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Aucun compte associé à cet email."));

        if (user.isEmailVerified()) {
            var token = jwtService.generateToken(user);
            return AuthenticationResponse.builder()
                    .token(token)
                    .message("Compte déjà vérifié.")
                    .build();
        }

        if (user.getVerificationCode() == null
                || !user.getVerificationCode().equals(request.getCode())) {
            throw new BadRequestException("Code de vérification incorrect.");
        }

        if (user.getVerificationCodeExpiry() == null
                || user.getVerificationCodeExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Ce code a expiré. Veuillez en demander un nouveau.");
        }

        user.setEmailVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiry(null);
        repository.save(user);

        var token = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .token(token)
                .message("Compte vérifié avec succès ! Bienvenue sur CoShift.")
                .build();
    }

    // --- RENVOI DU CODE (F7) ---
    public AuthenticationResponse resendVerificationCode(String email) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun compte associé à cet email."));

        if (user.isEmailVerified()) {
            throw new ConflictException("Ce compte est déjà vérifié.");
        }

        String newCode = generateVerificationCode();
        user.setVerificationCode(newCode);
        user.setVerificationCodeExpiry(LocalDateTime.now().plusHours(24));
        repository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), user.getFirstname(), newCode);

        return AuthenticationResponse.builder()
                .message("Un nouveau code a été envoyé à votre adresse email.")
                .build();
    }

    // --- CONNEXION (F5) ---
    public AuthenticationResponse authenticate(LoginRequest request) {
        // Vérifier manuellement si le compte est activé avant de passer dans Spring Security
        // (pour renvoyer un message explicite plutôt qu'une erreur 401 générique)
        User user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Email ou mot de passe incorrect."));

        if (!user.isEmailVerified()) {
            throw new DisabledException("Votre compte n'est pas encore activé. Vérifiez votre boîte email.");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        var jwtToken = jwtService.generateToken(user);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .message("Connexion réussie")
                .build();
    }

    
}