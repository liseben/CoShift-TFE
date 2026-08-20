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
import com.coshift.api.security.LoginAttemptService;
import com.coshift.api.security.SecurityAuditService;
import com.coshift.api.security.SecurityAuditService.Evenement;
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
import org.springframework.security.core.AuthenticationException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    /**
     * Version des conditions générales en vigueur.
     *
     * <p>Elle doit suivre {@code VERSION_CGU} du fichier
     * {@code coshift-frontend/src/config/legal.ts}, qui la présente à la
     * personne au moment de l'acceptation : consigner une version différente
     * de celle affichée priverait la preuve de tout intérêt.</p>
     */
    public static final String VERSION_CGU = "1.0";

    private final UserRepository repository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final LoginAttemptService loginAttemptService;
    private final SecurityAuditService audit;

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

                // 4. Un compte non activé ne doit pas obtenir de token, quel que soit
                //    le canal de connexion : sans ce contrôle, un utilisateur inscrit
                //    par le formulaire classique et jamais validé accédait à toute
                //    l'application via Google, contournant complètement F7.
                if (!user.isEmailVerified()) {
                    throw new DisabledException(
                            "Votre compte n'est pas encore activé. Vérifiez votre boîte email.");
                }

                // 5. Générer NOTRE Token JWT CoShift
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
                /* Preuve de l'accord : sans date ni version, il serait
                   impossible d'établir à quoi la personne a consenti, ni de
                   savoir qui prévenir lors d'une modification substantielle.
                   La contrainte @AssertTrue du DTO garantit qu'on n'arrive
                   ici qu'après une acceptation explicite. */
                .cguAcceptedAt(LocalDateTime.now())
                .cguVersion(VERSION_CGU)
                .build();

        repository.save(user);

        // Envoi asynchrone — ne bloque pas la réponse
        emailService.sendVerificationEmail(user.getEmail(), user.getFirstname(), code);

        return AuthenticationResponse.builder()
                .message("Compte créé ! Vérifiez votre email pour activer votre compte.")
                .build();
    }

    // --- VÉRIFICATION EMAIL (F7) ---
    public AuthenticationResponse verifyEmail(VerifyEmailRequest request, String clientIp) {
        // Un code à six chiffres se parcourt en entier par essais successifs :
        // sans freinage, l'activation d'un compte tiers n'est qu'une question
        // de temps machine.
        String attemptKey = loginAttemptService.key(clientIp, request.getEmail());
        loginAttemptService.assertNotBlocked(attemptKey);

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
            loginAttemptService.recordFailure(attemptKey);
            audit.consigner(Evenement.CODE_INVALIDE, user.getEmail(), clientIp, "activation du compte");
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
        loginAttemptService.reset(attemptKey);

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
    public AuthenticationResponse authenticate(LoginRequest request, String clientIp) {
        String attemptKey = loginAttemptService.key(clientIp, request.getEmail());
        loginAttemptService.assertNotBlocked(attemptKey);

        // Vérifier manuellement si le compte est activé avant de passer dans Spring Security
        // (pour renvoyer un message explicite plutôt qu'une erreur 401 générique)
        User user = repository.findByEmail(request.getEmail()).orElseGet(() -> {
            // Une adresse inconnue compte comme un échec : sans cela, essayer des
            // adresses au hasard resterait entièrement gratuit.
            loginAttemptService.recordFailure(attemptKey);
            audit.consigner(Evenement.CONNEXION_ECHOUEE, request.getEmail(), clientIp, "compte inconnu");
            throw new BadCredentialsException("Email ou mot de passe incorrect.");
        });

        // Compte non activé : ce n'est pas une erreur de mot de passe, la
        // comptabiliser bloquerait un utilisateur légitime qui insiste avant
        // d'avoir lu son courriel de vérification.
        if (!user.isEmailVerified()) {
            audit.consigner(Evenement.COMPTE_NON_ACTIVE, user.getEmail(), clientIp);
            throw new DisabledException("Votre compte n'est pas encore activé. Vérifiez votre boîte email.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            loginAttemptService.recordFailure(attemptKey);
            audit.consigner(Evenement.CONNEXION_ECHOUEE, user.getEmail(), clientIp, "mot de passe incorrect");
            throw e;
        }

        loginAttemptService.reset(attemptKey);
        audit.consigner(Evenement.CONNEXION_REUSSIE, user.getEmail(), clientIp);

        var jwtToken = jwtService.generateToken(user);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .message("Connexion réussie")
                .build();
    }

    // --- MOT DE PASSE OUBLIÉ (F6) ---

    /** Durée de validité du code de réinitialisation, volontairement courte. */
    private static final int RESET_CODE_VALIDITY_HOURS = 1;

    /**
     * Envoie un code de réinitialisation à l'adresse indiquée.
     *
     * <p>La réponse est identique que le compte existe ou non. Répondre 404 sur
     * une adresse inconnue transformerait ce point d'entrée, ouvert sans
     * authentification, en oracle permettant d'énumérer les comptes inscrits.</p>
     */
    public AuthenticationResponse forgotPassword(String email) {
        repository.findByEmail(email).ifPresent(user -> {
            String code = generateVerificationCode();
            user.setPasswordResetCode(code);
            user.setPasswordResetExpiry(LocalDateTime.now().plusHours(RESET_CODE_VALIDITY_HOURS));
            repository.save(user);

            emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstname(), code);
        });

        return AuthenticationResponse.builder()
                .message("Si un compte existe pour cette adresse, un code vient d'y être envoyé.")
                .build();
    }

    /**
     * Applique le nouveau mot de passe après contrôle du code reçu par courriel.
     *
     * <p>Aucun jeton n'est renvoyé : l'utilisateur repasse par l'écran de
     * connexion. Le statut de vérification de l'adresse n'est pas modifié — un
     * compte jamais activé le reste, F7 gardant son propre chemin.</p>
     */
    public AuthenticationResponse resetPassword(String email, String code, String newPassword,
                                                String clientIp) {
        String attemptKey = loginAttemptService.key(clientIp, email);
        loginAttemptService.assertNotBlocked(attemptKey);

        User user = repository.findByEmail(email).orElseGet(() -> {
            loginAttemptService.recordFailure(attemptKey);
            throw new BadRequestException("Code invalide ou expiré.");
        });

        // Message unique pour un code absent, faux ou périmé : le distinguer
        // renseignerait un attaquant sur l'existence d'une demande en cours.
        if (user.getPasswordResetCode() == null
                || !user.getPasswordResetCode().equals(code)
                || user.getPasswordResetExpiry() == null
                || user.getPasswordResetExpiry().isBefore(LocalDateTime.now())) {
            loginAttemptService.recordFailure(attemptKey);
            audit.consigner(Evenement.CODE_INVALIDE, user.getEmail(), clientIp, "reinitialisation du mot de passe");
            throw new BadRequestException("Code invalide ou expiré.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        // Un code ne sert qu'une fois : sans cette remise à zéro, il resterait
        // utilisable jusqu'à son expiration, y compris par un tiers.
        user.setPasswordResetCode(null);
        user.setPasswordResetExpiry(null);
        repository.save(user);
        loginAttemptService.reset(attemptKey);

        audit.consigner(Evenement.MOT_DE_PASSE_REINITIALISE, user.getEmail(), clientIp);

        return AuthenticationResponse.builder()
                .message("Mot de passe modifié. Vous pouvez maintenant vous connecter.")
                .build();
    }
}