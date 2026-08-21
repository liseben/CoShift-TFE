package com.coshift.api.controller.user;

import com.coshift.api.dto.AuthenticationResponse;
import com.coshift.api.service.Messages;
import com.coshift.api.dto.DeleteAccountRequest;
import com.coshift.api.dto.UserProfileResponse;
import com.coshift.api.dto.UserProfileUpdateRequest;
import com.coshift.api.entity.User;
import com.coshift.api.exception.BadRequestException;
import com.coshift.api.exception.ResourceNotFoundException;
import com.coshift.api.repository.UserRepository;
import com.coshift.api.service.PersonalDataService;
import com.coshift.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
@Tag(name = "Utilisateurs",
     description = "Profil du membre connecté et photo de profil. Un membre n'accède jamais qu'à ses propres données.")
public class UserController {

    private final UserRepository userRepository;
    private final Messages messages;
    private final UserService userService;
    private final PersonalDataService personalDataService;

    @Value("${app.upload.dir:uploads/avatars}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // F8 — Récupérer le profil complet
    @Operation(
            summary = "Consulter mon profil",
            description = """
                    Le profil complet du membre connecté : identité, adresse, téléphone,
                    rôle, statut de vérification, note moyenne et nombre de trajets.

                    L'identité est déduite du jeton, jamais d'un paramètre : c'est ce qui
                    interdit de consulter le profil d'un autre membre en changeant un
                    identifiant dans l'URL.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil du membre connecté."),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable.", content = @Content())
    })
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("auth.utilisateurIntrouvable")));

        return ResponseEntity.ok(UserProfileResponse.builder()
                .uuid(user.getUuid())
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
    @Operation(
            summary = "Modifier mon profil",
            description = """
                    Met à jour prénom, nom, adresse et téléphone.

                    **Deux réponses possibles selon que l'adresse change ou non :**

                    - *Adresse inchangée* — un nouveau jeton est renvoyé, l'ancien portant
                      encore les informations précédentes.
                    - *Adresse modifiée* — le compte **repasse en attente de vérification**,
                      un code part vers la nouvelle adresse, et la réponse porte
                      `emailVerified: false` **sans jeton**. L'accès est coupé dès la
                      requête suivante : la nouvelle adresse n'a jamais été prouvée, et
                      rester « vérifié » sur elle viderait l'activation de son sens. Le
                      client doit conduire l'utilisateur vers la saisie du code.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil mis à jour. Jeton renvoyé, sauf si l'adresse a changé."),
            @ApiResponse(responseCode = "400", description = "Champ invalide.", content = @Content()),
            @ApiResponse(responseCode = "409", description = "Cette adresse est déjà utilisée par un autre compte.", content = @Content())
    })
    @PutMapping("/profile")
    public ResponseEntity<AuthenticationResponse> updateProfile(
            @Valid @RequestBody UserProfileUpdateRequest request,
            Authentication authentication,
            HttpServletRequest http) {
        AuthenticationResponse response = userService.updateUserProfile(
                authentication.getName(), request, http.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    // F9 — Upload de la photo de profil
    @Operation(
            summary = "Envoyer ma photo de profil",
            description = """
                    Envoi en `multipart/form-data`, champ `file`. JPEG ou PNG, 2 Mo au
                    maximum — le type déclaré et la taille sont tous deux contrôlés côté
                    serveur.

                    Le nom du fichier écrit sur le disque est **généré par le serveur** à
                    partir d'un UUID, jamais repris de celui envoyé par le client : un nom
                    contenant `../` permettrait sinon d'écrire ailleurs que dans le dossier
                    prévu. L'ancienne photo est supprimée dans la foulée.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Photo enregistrée ; l'URL publique est renvoyée."),
            @ApiResponse(responseCode = "400", description = "Fichier vide, format autre que JPEG/PNG, ou taille supérieure à 2 Mo.", content = @Content()),
            @ApiResponse(responseCode = "413", description = "Fichier dépassant la limite acceptée par le serveur.", content = @Content())
    })
    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadPhoto(
            @RequestParam MultipartFile file,
            Authentication authentication) throws IOException {

        if (file.isEmpty()) {
            throw new BadRequestException(messages.get("profil.fichierVide"));
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            throw new BadRequestException(messages.get("profil.formatImage"));
        }

        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BadRequestException(messages.get("profil.tailleImage"));
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
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("auth.utilisateurIntrouvable")));

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

    // RGPD, articles 15 et 20 — accès et portabilité
    @Operation(
            summary = "Exporter mes données personnelles",
            description = """
                    Renvoie l'ensemble des données à caractère personnel détenues sur le
                    membre connecté : compte, organisations, véhicules, trajets proposés
                    et réservations demandées.

                    Le format est JSON parce que l'article 20 du RGPD exige un format
                    « structuré, couramment utilisé et lisible par machine ». La réponse
                    porte un en-tête `Content-Disposition` : le navigateur propose un
                    téléchargement plutôt que d'afficher le contenu.

                    **Les données des autres membres en sont exclues.** Un trajet réservé
                    chez un tiers apparaît avec son itinéraire et son horaire, jamais avec
                    le téléphone ni l'adresse du conducteur : le droit à la portabilité
                    porte sur ses propres données, pas sur celles d'autrui.

                    Un bloc `_nonInclus` énumère ce qui est volontairement absent et
                    pourquoi.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Export complet, en JSON."),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable.", content = @Content())
    })
    @GetMapping("/me/export")
    public ResponseEntity<Map<String, Object>> exporterMesDonnees(Authentication authentication) {
        Map<String, Object> donnees = personalDataService.exporter(authentication.getName());

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"coshift-mes-donnees.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(donnees);
    }

    // RGPD, article 17 — droit à l'effacement
    @Operation(
            summary = "Supprimer mon compte",
            description = """
                    Efface le compte du membre connecté. **L'opération est irréversible.**

                    Elle exige de retaper l'adresse électronique du compte dans le corps de
                    la requête : le jeton seul ne suffit pas, sans quoi une requête rejouée
                    ou un clic de trop deviendrait une perte définitive.

                    **Ce qui est effacé :** nom, prénom, adresse, téléphone, photographie,
                    mot de passe, plaques d'immatriculation et rattachement à
                    l'organisation.

                    **Ce qui est anonymisé plutôt qu'effacé :** les trajets et réservations
                    passés. Ils engagent d'autres membres, dont l'historique ne peut pas
                    être détruit par la demande d'un tiers. Une fois détachés de toute
                    personne identifiable, ils ne relèvent plus du RGPD (considérant 26).

                    **Ce qui est annulé :** les trajets futurs du conducteur et les
                    réservations en cours, afin que personne ne se présente à un
                    rendez-vous qui n'aura pas lieu.""")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Compte effacé."),
            @ApiResponse(responseCode = "400", description = "Adresse de confirmation absente ou différente de celle du compte.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable.", content = @Content())
    })
    @DeleteMapping("/me")
    public ResponseEntity<Void> supprimerMonCompte(
            @Valid @RequestBody DeleteAccountRequest request,
            Authentication authentication,
            HttpServletRequest http) {
        personalDataService.effacer(
                authentication.getName(), request.getConfirmationEmail(), http.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}