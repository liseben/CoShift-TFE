package com.coshift.api.controller.user;

import com.coshift.api.dto.UserProfileResponse;
import com.coshift.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins:*}")
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(Authentication authentication) {
        // La magie de Spring Security : l'objet Authentication contient l'email 
        // qui a été extrait du Token JWT par notre JwtAuthenticationFilter !
        String email = authentication.getName();

        // On va chercher l'utilisateur complet dans la base de données
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // On construit la réponse pour le Frontend
        var response = UserProfileResponse.builder()
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .email(user.getEmail())
                .pictureUrl(user.getPictureUrl())
                .role(user.getRole().name())
                .build();

        return ResponseEntity.ok(response);
    }
}