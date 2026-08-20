package com.coshift.api.controller.auth;

import com.coshift.api.dto.AuthenticationResponse;
import com.coshift.api.dto.ForgotPasswordRequest;
import com.coshift.api.dto.GoogleLoginRequest;
import com.coshift.api.dto.LoginRequest;
import com.coshift.api.dto.RegisterRequest;
import com.coshift.api.dto.ResetPasswordRequest;
import com.coshift.api.dto.VerifyEmailRequest;
import com.coshift.api.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;

    @PostMapping("/google")
    public ResponseEntity<AuthenticationResponse> authenticateWithGoogle(
            @Valid @RequestBody GoogleLoginRequest request
    ) {
        return ResponseEntity.ok(service.authenticateWithGoogle(request));
    }

    // --- LA NOUVELLE ROUTE D'INSCRIPTION ---
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(service.register(request));
    }

    // --- LA ROUTE DE CONNEXION ---
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(service.authenticate(request));
    }

    // --- F7 : VÉRIFICATION DE L'EMAIL ---
    @PostMapping("/verify-email")
    public ResponseEntity<AuthenticationResponse> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        return ResponseEntity.ok(service.verifyEmail(request));
    }

    // --- F7 : RENVOI DU CODE ---
    @PostMapping("/resend-verification")
    public ResponseEntity<AuthenticationResponse> resendVerification(
            @RequestBody Map<String, String> body
    ) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(AuthenticationResponse.builder().message("Email requis.").build());
        }
        return ResponseEntity.ok(service.resendVerificationCode(email));
    }

    // --- F6 : DEMANDE DE RÉINITIALISATION DU MOT DE PASSE ---
    @PostMapping("/forgot-password")
    public ResponseEntity<AuthenticationResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        return ResponseEntity.ok(service.forgotPassword(request.getEmail()));
    }

    // --- F6 : CHOIX DU NOUVEAU MOT DE PASSE ---
    @PostMapping("/reset-password")
    public ResponseEntity<AuthenticationResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        return ResponseEntity.ok(service.resetPassword(
                request.getEmail(), request.getCode(), request.getNewPassword()));
    }
}