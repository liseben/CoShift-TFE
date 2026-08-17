package com.coshift.api.service;
import com.coshift.api.dto.AuthenticationResponse;
import com.coshift.api.dto.UserProfileUpdateRequest;
import com.coshift.api.entity.User;
import com.coshift.api.exception.ConflictException;
import com.coshift.api.exception.ResourceNotFoundException;
import com.coshift.api.repository.UserRepository;
import com.coshift.api.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final JwtService jwtService; // Ton service qui génère les tokens

    public UserService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthenticationResponse updateUserProfile(String currentEmail, UserProfileUpdateRequest request) {
        // 1. Récupérer l'utilisateur actuel
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));

        // 2. Vérifier si l'email a changé ET s'il est déjà pris par un autre utilisateur
        if (!user.getEmail().equals(request.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ConflictException("Cet email est déjà utilisé par un autre compte.");
            }
            user.setEmail(request.getEmail());
        }

        // 3. Mettre à jour les autres champs
        user.setFirstname(request.getFirstname());
        user.setLastname(request.getLastname());
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        // 4. Sauvegarder en base de données
        userRepository.save(user);

        // 5. Générer un NOUVEAU token JWT avec les informations mises à jour
        String newToken = jwtService.generateToken(user);

        // 6. Renvoyer le token (en utilisant le même DTO que pour le Login)
        return AuthenticationResponse.builder()
        .token(newToken) // Assure-toi que le nom de la variable dans ton DTO est bien "token"
        .build();
    }
}