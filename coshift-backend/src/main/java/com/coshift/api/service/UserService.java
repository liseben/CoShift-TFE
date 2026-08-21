package com.coshift.api.service;
import com.coshift.api.dto.AuthenticationResponse;
import com.coshift.api.dto.UserProfileUpdateRequest;
import com.coshift.api.entity.User;
import com.coshift.api.exception.ConflictException;
import com.coshift.api.exception.ResourceNotFoundException;
import com.coshift.api.repository.UserRepository;
import com.coshift.api.security.JwtService;
import com.coshift.api.security.SecurityAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final Messages messages;
    private final JwtService jwtService; // Ton service qui génère les tokens
    private final EmailService emailService;
    private final SecurityAuditService audit;

    private static final SecureRandom RANDOM = new SecureRandom();

    /* Constructeur écrit à la main plutôt que produit par Lombok : ajouter un
       champ final ici impose donc d'ajouter le paramètre correspondant, sans
       quoi la compilation échoue — ce qui est arrivé en introduisant
       `messages`. */
    public UserService(UserRepository userRepository, Messages messages,
                       JwtService jwtService, EmailService emailService,
                       SecurityAuditService audit) {
        this.userRepository = userRepository;
        this.messages = messages;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.audit = audit;
    }

    @Transactional
    public AuthenticationResponse updateUserProfile(String currentEmail, UserProfileUpdateRequest request,
                                                    String clientIp) {
        // 1. Récupérer l'utilisateur actuel
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("auth.utilisateurIntrouvable")));

        // 2. Vérifier si l'email a changé ET s'il est déjà pris par un autre utilisateur
        boolean emailChange = !user.getEmail().equals(request.getEmail());
        if (emailChange) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ConflictException(messages.get("profil.emailUtilise"));
            }
            user.setEmail(request.getEmail());

            /*
             * La nouvelle adresse n'a jamais été prouvée : le compte repasse donc
             * en attente de vérification, et un code part vers elle.
             *
             * Sans cela, emailVerified restait vrai sur une adresse quelconque. Il
             * suffisait de s'inscrire avec une adresse valide, de la vérifier, puis
             * d'en changer pour se retrouver « vérifié » sur une adresse qu'on ne
             * possède pas — F7 ne garantissait plus rien. Le filtre JWT contrôlant
             * isEnabled(), l'accès est coupé dès la requête suivante.
             */
            user.setEmailVerified(false);
            user.setVerificationCode(genererCode());
            user.setVerificationCodeExpiry(LocalDateTime.now().plusHours(24));
        }

        // 3. Mettre à jour les autres champs
        user.setFirstname(request.getFirstname());
        user.setLastname(request.getLastname());
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        // 4. Sauvegarder en base de données
        userRepository.save(user);

        if (emailChange) {
            // Un changement d'adresse est le premier geste d'une prise de contrôle
            // de compte : il doit laisser une trace, y compris pour permettre à la
            // personne d'établir plus tard ce qui s'est passé.
            audit.consigner(SecurityAuditService.Evenement.ADRESSE_MODIFIEE, currentEmail, clientIp,
                    "nouvelle adresse : " + user.getEmail());
            emailService.sendVerificationEmail(user.getEmail(), user.getFirstname(),
                    user.getVerificationCode(), messages.langueCourante());
            return AuthenticationResponse.builder()
                    .emailVerified(false)
                    .message(messages.get("profil.adresseModifiee", user.getEmail()))
                    .build();
        }

        // 5. Générer un NOUVEAU token JWT avec les informations mises à jour
        String newToken = jwtService.generateToken(user);

        // 6. Renvoyer le token (en utilisant le même DTO que pour le Login)
        return AuthenticationResponse.builder()
                .token(newToken)
                .message(messages.get("profil.misAJour"))
                .build();
    }

    private String genererCode() {
        return String.valueOf(100000 + RANDOM.nextInt(900000));
    }
}
