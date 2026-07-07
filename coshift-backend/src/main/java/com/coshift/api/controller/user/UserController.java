package com.coshift.api.controller.user;

import com.coshift.api.dto.AuthenticationResponse;
import com.coshift.api.dto.UserProfileResponse;
import com.coshift.api.dto.UserProfileUpdateRequest;
import com.coshift.api.repository.UserRepository;
import com.coshift.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins:*}")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService; // 👈 AJOUT : On injecte le service !

    // 1. RÉCUPÉRER LE PROFIL (Ce que tu avais déjà)
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();

        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        var response = UserProfileResponse.builder()
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .email(user.getEmail())
                .pictureUrl(user.getPictureUrl())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().name())
                .emailVerified(user.isEmailVerified())
                .averageRating(user.getAverageRating())
                .tripsCount(user.getTripsCount())
                .build();

        return ResponseEntity.ok(response);
    }

    // 2. METTRE À JOUR LE PROFIL (Ce qu'il manquait !)
    @PutMapping("/profile")
    public ResponseEntity<AuthenticationResponse> updateProfile(
            @Valid @RequestBody UserProfileUpdateRequest request, 
            Authentication authentication) {
        
        // On récupère l'email actuel via le token JWT
        String currentEmail = authentication.getName();
        
        // On délègue la logique lourde au UserService
        AuthenticationResponse response = userService.updateUserProfile(currentEmail, request);
        
        // On renvoie le nouveau token JWT généré
        return ResponseEntity.ok(response);
    }
}