package com.coshift.api.service;

import com.coshift.api.dto.UserProfileUpdateRequest;
import com.coshift.api.entity.Role;
import com.coshift.api.entity.User;
import com.coshift.api.exception.ConflictException;
import com.coshift.api.exception.ResourceNotFoundException;
import com.coshift.api.repository.UserRepository;
import com.coshift.api.security.JwtService;
import com.coshift.api.security.SecurityAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Modification du profil.
 *
 * <p>Le point délicat est le changement d'adresse. Sans re-vérification,
 * {@code emailVerified} restait vrai sur une adresse quelconque : il suffisait
 * de s'inscrire avec une adresse qu'on possède, de la vérifier, puis d'en
 * changer pour se retrouver « vérifié » sur une adresse dont on n'a jamais rien
 * prouvé. F7 ne garantissait alors plus rien.</p>
 *
 * <p>Les tests distinguent donc systématiquement les deux chemins — avec et
 * sans changement d'adresse — parce qu'ils ne rendent ni la même chose, ni les
 * mêmes effets de bord.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserService — profil")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private EmailService emailService;
    @Mock private SecurityAuditService audit;
    @Mock private Messages messages;

    private UserService service;

    private User membre;

    private static final String COURRIEL = "membre@coshift.be";
    private static final String NOUVELLE = "nouvelle@coshift.be";
    private static final String IP = "203.0.113.7";

    @BeforeEach
    void preparer() {
        // Constructeur explicite : ce service n'utilise pas @RequiredArgsConstructor.
        service = new UserService(userRepository, messages, jwtService, emailService, audit);

        membre = User.builder()
                .id(1L).uuid("uuid-membre")
                .email(COURRIEL)
                .firstname("Camille").lastname("Dupont")
                .phoneNumber("+32470000000")
                .password("empreinte")
                .role(Role.USER)
                .emailVerified(true)
                .build();

        when(userRepository.findByEmail(COURRIEL)).thenReturn(Optional.of(membre));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(jwtService.generateToken(any(User.class))).thenReturn("jeton-jwt");
        when(messages.get(anyString())).thenReturn("message");
        when(messages.get(anyString(), any())).thenReturn("message");
        when(messages.langueCourante()).thenReturn(Locale.FRENCH);
    }

    @Nested
    @DisplayName("Sans changement d'adresse")
    class SansChangementDadresse {

        @Test
        @DisplayName("met à jour l'identité et rend un nouveau jeton")
        void metAJourEtRendUnJeton() {
            // L'ancien jeton porte encore les informations précédentes : il est
            // remplacé pour que le client n'ait pas à se reconnecter.
            var reponse = service.updateUserProfile(COURRIEL, demande(COURRIEL, "Alex", "Martin"), IP);

            assertThat(membre.getFirstname()).isEqualTo("Alex");
            assertThat(membre.getLastname()).isEqualTo("Martin");
            assertThat(reponse.getToken()).isEqualTo("jeton-jwt");
        }

        @Test
        @DisplayName("laisse le compte vérifié")
        void laisseLeCompteVerifie() {
            service.updateUserProfile(COURRIEL, demande(COURRIEL, "Alex", "Martin"), IP);

            assertThat(membre.isEmailVerified()).isTrue();
            assertThat(membre.getVerificationCode()).isNull();
        }

        @Test
        @DisplayName("n'envoie aucun courriel et ne consigne rien")
        void nEnvoieRien() {
            service.updateUserProfile(COURRIEL, demande(COURRIEL, "Alex", "Martin"), IP);

            verify(emailService, never()).sendVerificationEmail(
                    anyString(), anyString(), anyString(), any());
            verify(audit, never()).consigner(any(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("conserve le téléphone existant si aucun n'est fourni")
        void conserveLeTelephone() {
            // Une modification qui n'aborde pas le téléphone ne doit pas
            // l'effacer.
            UserProfileUpdateRequest r = demande(COURRIEL, "Alex", "Martin");
            r.setPhoneNumber(null);

            service.updateUserProfile(COURRIEL, r, IP);

            assertThat(membre.getPhoneNumber()).isEqualTo("+32470000000");
        }

        @Test
        @DisplayName("applique un nouveau téléphone quand il est fourni")
        void appliqueLeNouveauTelephone() {
            UserProfileUpdateRequest r = demande(COURRIEL, "Alex", "Martin");
            r.setPhoneNumber("+32499999999");

            service.updateUserProfile(COURRIEL, r, IP);

            assertThat(membre.getPhoneNumber()).isEqualTo("+32499999999");
        }
    }

    @Nested
    @DisplayName("Avec changement d'adresse")
    class AvecChangementDadresse {

        @Test
        @DisplayName("repasse le compte en attente de vérification")
        void repasseEnAttente() {
            // La nouvelle adresse n'a jamais été prouvée. Rester « vérifié » sur
            // elle viderait l'activation de son sens.
            service.updateUserProfile(COURRIEL, demande(NOUVELLE, "Camille", "Dupont"), IP);

            assertThat(membre.getEmail()).isEqualTo(NOUVELLE);
            assertThat(membre.isEmailVerified()).isFalse();
        }

        @Test
        @DisplayName("génère un code à six chiffres avec une expiration")
        void genereUnCode() {
            service.updateUserProfile(COURRIEL, demande(NOUVELLE, "Camille", "Dupont"), IP);

            assertThat(membre.getVerificationCode()).hasSize(6);
            assertThat(membre.getVerificationCode()).containsOnlyDigits();
            assertThat(membre.getVerificationCodeExpiry()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("ne rend AUCUN jeton")
        void neRendAucunJeton() {
            // C'est le point décisif : rendre un jeton laisserait la session
            // ouverte sur une adresse non prouvée. Le client doit conduire la
            // personne vers la saisie du code.
            var reponse = service.updateUserProfile(COURRIEL, demande(NOUVELLE, "Camille", "Dupont"), IP);

            assertThat(reponse.getToken()).isNull();
            assertThat(reponse.getEmailVerified()).isFalse();
            verify(jwtService, never()).generateToken(any(User.class));
        }

        @Test
        @DisplayName("envoie le code à la nouvelle adresse, pas à l'ancienne")
        void envoieALaNouvelleAdresse() {
            service.updateUserProfile(COURRIEL, demande(NOUVELLE, "Camille", "Dupont"), IP);

            verify(emailService).sendVerificationEmail(
                    eq(NOUVELLE), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("consigne le changement au journal de sécurité")
        void consigneLeChangement() {
            // Un changement d'adresse est le premier geste d'une prise de
            // contrôle de compte : il doit laisser une trace, y compris pour
            // permettre à la personne d'établir plus tard ce qui s'est passé.
            service.updateUserProfile(COURRIEL, demande(NOUVELLE, "Camille", "Dupont"), IP);

            verify(audit).consigner(eq(SecurityAuditService.Evenement.ADRESSE_MODIFIEE),
                    eq(COURRIEL), eq(IP), anyString());
        }

        @Test
        @DisplayName("refuse une adresse déjà prise par quelqu'un d'autre")
        void refuseUneAdresseDejaPrise() {
            when(userRepository.existsByEmail(NOUVELLE)).thenReturn(true);

            assertThatThrownBy(() ->
                    service.updateUserProfile(COURRIEL, demande(NOUVELLE, "Camille", "Dupont"), IP))
                    .isInstanceOf(ConflictException.class);

            assertThat(membre.getEmail()).isEqualTo(COURRIEL);
            assertThat(membre.isEmailVerified()).isTrue();
            verify(userRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("refuse un compte inconnu")
    void refuseUnCompteInconnu() {
        when(userRepository.findByEmail("fantome@coshift.be")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.updateUserProfile("fantome@coshift.be", demande(COURRIEL, "A", "B"), IP))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ────────────────────────────────── Fabriques ───────────────────────────────

    private UserProfileUpdateRequest demande(String courriel, String prenom, String nom) {
        UserProfileUpdateRequest r = new UserProfileUpdateRequest();
        r.setEmail(courriel);
        r.setFirstname(prenom);
        r.setLastname(nom);
        r.setPhoneNumber("+32470000000");
        return r;
    }
}
