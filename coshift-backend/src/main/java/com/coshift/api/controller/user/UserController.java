package com.coshift.api.controller.user;

import com.coshift.api.dto.AuthenticationResponse;
import com.coshift.api.dto.UserProfileResponse;
import com.coshift.api.dto.UserProfileUpdateRequest;
import com.coshift.api.entity.User;
import com.coshift.api.repository.UserRepository;
import com.coshift.api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins:*}")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    @Value("${app.upload.dir:uploads/avatars}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // F8 — Récupérer le profil complet
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        return ResponseEntity.ok(UserProfileResponse.builder()
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .email(user.getEmail())
                .pictureUrl(user.getPictureUrl())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().name())
                .emailVerified(user.isEmailVerified())
                .averageRating(user.getAverageRating())
                .tripsCount(user.getTripsCount())
                .build());
    }

    // F9 — Modifier le profil (nom, email, téléphone)
    @PutMapping("/profile")
    public ResponseEntity<AuthenticationResponse> updateProfile(
            @Valid @RequestBody UserProfileUpdateRequest request,
            Authentication authentication) {
        AuthenticationResponse response = userService.updateUserProfile(authentication.getName(), request);
        return ResponseEntity.ok(response);
    }

    // F9 — Upload de la photo de profil
    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadPhoto(
            @RequestParam MultipartFile file,
            Authentication authentication) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Fichier vide."));
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            return ResponseEntity.badRequest().body(Map.of("message", "Seuls les formats JPG et PNG sont acceptés."));
        }

        if (file.getSize() > 2 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("message", "La photo ne doit pas dépasser 2 Mo."));
        }

        // Créer le dossier si nécessaire
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Nom de fichier unique basé sur l'UUID
        String extension = contentType.equals("image/png") ? ".png" : ".jpg";
        String filename = UUID.randomUUID() + extension;
        Path filePath = uploadPath.resolve(filename);
        Files.write(filePath, file.getBytes());

        // Mettre à jour la DB
        String pictureUrl = baseUrl + "/uploads/avatars/" + filename;
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // Supprimer l'ancienne photo locale si elle existe
        if (user.getPictureUrl() != null && user.getPictureUrl().startsWith(baseUrl)) {
            String oldFilename = user.getPictureUrl().substring(user.getPictureUrl().lastIndexOf('/') + 1);
            Path oldPath = uploadPath.resolve(oldFilename);
            Files.deleteIfExists(oldPath);
        }

        user.setPictureUrl(pictureUrl);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("pictureUrl", pictureUrl));
    }
}