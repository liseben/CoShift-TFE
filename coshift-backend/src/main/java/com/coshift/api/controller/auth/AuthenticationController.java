package com.coshift.api.controller.auth;

import com.coshift.api.dto.AuthenticationResponse;
import com.coshift.api.service.Messages;
import com.coshift.api.dto.ForgotPasswordRequest;
import com.coshift.api.dto.GoogleLoginRequest;
import com.coshift.api.dto.LoginRequest;
import com.coshift.api.dto.RegisterRequest;
import com.coshift.api.dto.ResetPasswordRequest;
import com.coshift.api.dto.VerifyEmailRequest;
import com.coshift.api.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Entrée et sortie du système : création de compte, activation, connexion et
 * réinitialisation du mot de passe.
 *
 * <p>Tout le contrôleur est public — {@code @SecurityRequirements} lève
 * l'exigence de jeton posée globalement dans {@code OpenApiConfig} : on ne peut
 * évidemment pas présenter un jeton pour aller le chercher.</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "Authentification",
     description = "Inscription, activation du compte, connexion et réinitialisation du mot de passe.")
public class AuthenticationController {

    private final AuthenticationService service;
    private final Messages messages;

    @Operation(
            summary = "Se connecter avec un compte Google",
            description = """
                    Vérifie le jeton d'identité Google puis délivre un jeton CoShift.

                    Le compte doit **exister au préalable** : ce point d'entrée connecte,
                    il n'inscrit pas. Un compte dont l'adresse n'a jamais été vérifiée est
                    refusé ici comme ailleurs, faute de quoi passer par Google suffirait à
                    contourner l'activation.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connexion réussie, jeton délivré."),
            @ApiResponse(responseCode = "401", description = "Jeton Google invalide, ou aucun compte pour cette adresse.", content = @Content()),
            @ApiResponse(responseCode = "403", description = "Compte existant mais adresse non vérifiée.", content = @Content())
    })
    @PostMapping("/google")
    public ResponseEntity<AuthenticationResponse> authenticateWithGoogle(
            @Valid @RequestBody GoogleLoginRequest request
    ) {
        return ResponseEntity.ok(service.authenticateWithGoogle(request));
    }

    @Operation(
            summary = "Créer un compte",
            description = """
                    Enregistre un nouveau membre et lui envoie par courriel un code
                    d'activation à six chiffres, valable 24 heures.

                    **Aucun jeton n'est renvoyé** : le compte reste inutilisable tant que
                    l'adresse n'a pas été vérifiée par `POST /api/auth/verify-email`.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte créé, code envoyé."),
            @ApiResponse(responseCode = "400", description = "Champ manquant, adresse mal formée ou mot de passe de moins de 6 caractères.", content = @Content()),
            @ApiResponse(responseCode = "409", description = "Un compte existe déjà pour cette adresse.", content = @Content())
    })
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(service.register(request));
    }

    @Operation(
            summary = "Se connecter",
            description = """
                    Vérifie les identifiants et délivre un jeton JWT valable 24 heures.

                    Après **cinq échecs**, la combinaison adresse IP + compte visé est
                    bloquée quinze minutes et l'API répond `429`. Le message d'échec est
                    identique que l'adresse existe ou non : les distinguer permettrait
                    d'énumérer les comptes inscrits.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connexion réussie, jeton délivré."),
            @ApiResponse(responseCode = "401", description = "Adresse ou mot de passe incorrect.", content = @Content()),
            @ApiResponse(responseCode = "403", description = "Compte non activé.", content = @Content()),
            @ApiResponse(responseCode = "429", description = "Trop de tentatives ; réessayer plus tard.", content = @Content())
    })
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.ok(service.authenticate(request, clientIp(http)));
    }

    @Operation(
            summary = "Activer un compte avec le code reçu",
            description = """
                    Valide le code à six chiffres reçu par courriel et active le compte.
                    Un jeton est délivré dans la foulée : l'utilisateur n'a pas à se
                    reconnecter juste après.

                    Le code est également protégé contre les essais successifs — un
                    million de combinaisons se parcourt sans peine si rien ne l'en
                    empêche.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte activé, jeton délivré."),
            @ApiResponse(responseCode = "400", description = "Code incorrect ou expiré.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Aucun compte pour cette adresse.", content = @Content()),
            @ApiResponse(responseCode = "429", description = "Trop de tentatives.", content = @Content())
    })
    @PostMapping("/verify-email")
    public ResponseEntity<AuthenticationResponse> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.ok(service.verifyEmail(request, clientIp(http)));
    }

    @Operation(
            summary = "Renvoyer le code d'activation",
            description = "Génère un nouveau code à six chiffres et l'envoie. L'ancien cesse d'être valable.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nouveau code envoyé."),
            @ApiResponse(responseCode = "400", description = "Adresse absente de la requête.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Aucun compte pour cette adresse.", content = @Content()),
            @ApiResponse(responseCode = "409", description = "Ce compte est déjà activé.", content = @Content())
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<AuthenticationResponse> resendVerification(
            @RequestBody Map<String, String> body
    ) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(AuthenticationResponse.builder().message(messages.get("auth.emailRequis")).build());
        }
        return ResponseEntity.ok(service.resendVerificationCode(email));
    }

    @Operation(
            summary = "Demander un code de réinitialisation",
            description = """
                    Envoie un code à six chiffres valable **une heure**, utilisable une
                    seule fois.

                    La réponse est **volontairement identique** que le compte existe ou
                    non. Répondre « adresse inconnue » ferait de ce point d'entrée, ouvert
                    sans authentification, un moyen commode de tester qui est inscrit.""")
    @ApiResponse(responseCode = "200", description = "Message neutre, qu'un envoi ait eu lieu ou non.")
    @PostMapping("/forgot-password")
    public ResponseEntity<AuthenticationResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        return ResponseEntity.ok(service.forgotPassword(request.getEmail()));
    }

    @Operation(
            summary = "Choisir un nouveau mot de passe",
            description = """
                    Applique le nouveau mot de passe après contrôle du code reçu, puis
                    efface le code — une seconde tentative avec le même code échoue.

                    **Aucun jeton n'est renvoyé** : l'utilisateur repasse volontairement
                    par l'écran de connexion. Le statut de vérification de l'adresse n'est
                    pas modifié.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mot de passe modifié."),
            @ApiResponse(responseCode = "400", description = "Code invalide ou expiré, ou mot de passe trop court.", content = @Content()),
            @ApiResponse(responseCode = "429", description = "Trop de tentatives.", content = @Content())
    })
    @PostMapping("/reset-password")
    public ResponseEntity<AuthenticationResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest http
    ) {
        return ResponseEntity.ok(service.resetPassword(
                request.getEmail(), request.getCode(), request.getNewPassword(), clientIp(http)));
    }

    /**
     * Adresse de l'appelant, servant de clé au freinage des tentatives.
     *
     * <p>Volontairement {@code getRemoteAddr()} et non l'en-tête
     * {@code X-Forwarded-For} : sans reverse proxy de confiance en amont, cet
     * en-tête est fourni par le client lui-même, qui n'aurait qu'à le faire
     * varier pour se donner une adresse neuve à chaque essai — le freinage
     * deviendrait purement décoratif. Derrière un proxy, il faudra le lire, mais
     * seulement après avoir déclaré ce proxy comme digne de confiance.</p>
     */
    private String clientIp(HttpServletRequest http) {
        String remote = http.getRemoteAddr();
        return (remote == null || remote.isBlank()) ? "inconnue" : remote;
    }
}
