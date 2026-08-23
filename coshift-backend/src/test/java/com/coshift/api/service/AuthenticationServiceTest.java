package com.coshift.api.service;

import com.coshift.api.dto.LoginRequest;
import com.coshift.api.dto.RegisterRequest;
import com.coshift.api.dto.VerifyEmailRequest;
import com.coshift.api.entity.Role;
import com.coshift.api.entity.User;
import com.coshift.api.exception.BadRequestException;
import com.coshift.api.exception.ConflictException;
import com.coshift.api.exception.ResourceNotFoundException;
import com.coshift.api.exception.TooManyRequestsException;
import com.coshift.api.repository.UserRepository;
import com.coshift.api.security.JwtService;
import com.coshift.api.security.LoginAttemptService;
import com.coshift.api.security.SecurityAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Inscription, activation, connexion et réinitialisation.
 *
 * <p>Ce service est la porte d'entrée de l'application : tout ce qui le
 * traverse à tort traverse ensuite tout le reste. Les tests portent donc
 * autant sur ce qu'il accepte que sur ce qu'il refuse, et sur ce qu'il
 * <em>ne dit pas</em> — plusieurs de ses règles existent précisément pour
 * qu'un appelant ne puisse rien déduire de la réponse.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthenticationService — authentification")
class AuthenticationServiceTest {

    @Mock private UserRepository repository;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private LoginAttemptService loginAttemptService;
    @Mock private SecurityAuditService audit;
    @Mock private Messages messages;

    @InjectMocks private AuthenticationService service;

    private User membre;

    private static final String COURRIEL = "membre@coshift.be";
    private static final String IP = "203.0.113.7";

    @BeforeEach
    void preparer() {
        membre = User.builder()
                .id(1L).uuid("uuid-membre")
                .email(COURRIEL)
                .firstname("Camille").lastname("Dupont")
                .password("empreinte")
                .role(Role.USER)
                .emailVerified(true)
                .build();

        when(repository.findByEmail(COURRIEL)).thenReturn(Optional.of(membre));
        when(repository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("nouvelle-empreinte");
        when(jwtService.generateToken(any(User.class))).thenReturn("jeton-jwt");
        when(messages.get(anyString())).thenReturn("message");
        when(messages.langueCourante()).thenReturn(Locale.FRENCH);
        when(loginAttemptService.key(anyString(), anyString())).thenReturn("cle");
    }

    // ═══════════════════════════ F4 — Inscription ═══════════════════════════════

    @Nested
    @DisplayName("Inscription")
    class Inscription {

        @Test
        @DisplayName("crée un compte non vérifié, sans délivrer de jeton")
        void creeUnCompteNonVerifie() {
            // L'inscription ne connecte pas : le jeton n'arrive qu'après la
            // saisie du code. C'est ce qui garantit qu'aucun compte non validé
            // n'accède à l'application.
            when(repository.findByEmail("neuf@coshift.be")).thenReturn(Optional.empty());

            var reponse = service.register(inscription("neuf@coshift.be"));

            assertThat(reponse.getToken()).isNull();

            ArgumentCaptor<User> capture = ArgumentCaptor.forClass(User.class);
            verify(repository).save(capture.capture());
            assertThat(capture.getValue().isEmailVerified()).isFalse();
            assertThat(capture.getValue().getRole()).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("chiffre le mot de passe avant de l'enregistrer")
        void chiffreLeMotDePasse() {
            when(repository.findByEmail("neuf@coshift.be")).thenReturn(Optional.empty());

            service.register(inscription("neuf@coshift.be"));

            ArgumentCaptor<User> capture = ArgumentCaptor.forClass(User.class);
            verify(repository).save(capture.capture());
            assertThat(capture.getValue().getPassword()).isEqualTo("nouvelle-empreinte");
            assertThat(capture.getValue().getPassword()).isNotEqualTo("MotDePasse123!");
        }

        @Test
        @DisplayName("consigne la preuve de l'acceptation des conditions")
        void consigneLacceptationDesConditions() {
            // Sans date ni version, il serait impossible d'établir à quoi la
            // personne a consenti, ni de savoir qui prévenir lors d'une
            // modification substantielle.
            when(repository.findByEmail("neuf@coshift.be")).thenReturn(Optional.empty());

            service.register(inscription("neuf@coshift.be"));

            ArgumentCaptor<User> capture = ArgumentCaptor.forClass(User.class);
            verify(repository).save(capture.capture());
            assertThat(capture.getValue().getCguAcceptedAt()).isNotNull();
            assertThat(capture.getValue().getCguVersion())
                    .isEqualTo(AuthenticationService.VERSION_CGU);
        }

        @Test
        @DisplayName("génère un code à six chiffres, valable un temps borné")
        void genereUnCodeBorne() {
            when(repository.findByEmail("neuf@coshift.be")).thenReturn(Optional.empty());

            service.register(inscription("neuf@coshift.be"));

            ArgumentCaptor<User> capture = ArgumentCaptor.forClass(User.class);
            verify(repository).save(capture.capture());
            assertThat(capture.getValue().getVerificationCode()).hasSize(6);
            assertThat(capture.getValue().getVerificationCode()).containsOnlyDigits();
            assertThat(capture.getValue().getVerificationCodeExpiry())
                    .isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("envoie le code à l'adresse déclarée")
        void envoieLeCode() {
            when(repository.findByEmail("neuf@coshift.be")).thenReturn(Optional.empty());

            service.register(inscription("neuf@coshift.be"));

            verify(emailService).sendVerificationEmail(
                    eq("neuf@coshift.be"), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("refuse une adresse déjà inscrite")
        void refuseUneAdresseDejaInscrite() {
            assertThatThrownBy(() -> service.register(inscription(COURRIEL)))
                    .isInstanceOf(ConflictException.class);

            verify(repository, never()).save(any());
        }
    }

    // ═══════════════════════ F7 — Vérification de l'adresse ═════════════════════

    @Nested
    @DisplayName("Vérification de l'adresse")
    class Verification {

        @BeforeEach
        void compteEnAttente() {
            membre.setEmailVerified(false);
            membre.setVerificationCode("123456");
            membre.setVerificationCodeExpiry(LocalDateTime.now().plusHours(1));
        }

        @Test
        @DisplayName("active le compte et délivre un jeton sur le bon code")
        void activeSurLeBonCode() {
            var reponse = service.verifyEmail(verification("123456"), IP);

            assertThat(membre.isEmailVerified()).isTrue();
            assertThat(reponse.getToken()).isEqualTo("jeton-jwt");
        }

        @Test
        @DisplayName("efface le code après usage")
        void effaceLeCodeApresUsage() {
            // Un code qui traîne reste utilisable jusqu'à son expiration, y
            // compris par un tiers.
            service.verifyEmail(verification("123456"), IP);

            assertThat(membre.getVerificationCode()).isNull();
            assertThat(membre.getVerificationCodeExpiry()).isNull();
        }

        @Test
        @DisplayName("refuse un code erroné et le comptabilise")
        void refuseUnCodeErrone() {
            // Un code à six chiffres se parcourt entièrement par essais
            // successifs : sans freinage, activer le compte d'un tiers n'est
            // qu'une question de temps machine.
            assertThatThrownBy(() -> service.verifyEmail(verification("999999"), IP))
                    .isInstanceOf(BadRequestException.class);

            assertThat(membre.isEmailVerified()).isFalse();
            verify(loginAttemptService).recordFailure("cle");
            verify(audit).consigner(eq(SecurityAuditService.Evenement.CODE_INVALIDE),
                    anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("refuse un code expiré")
        void refuseUnCodeExpire() {
            membre.setVerificationCodeExpiry(LocalDateTime.now().minusMinutes(1));

            assertThatThrownBy(() -> service.verifyEmail(verification("123456"), IP))
                    .isInstanceOf(BadRequestException.class);

            assertThat(membre.isEmailVerified()).isFalse();
        }

        @Test
        @DisplayName("refuse quand aucun code n'a été demandé")
        void refuseSansCodeEnCours() {
            membre.setVerificationCode(null);

            assertThatThrownBy(() -> service.verifyEmail(verification("123456"), IP))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("remet le compteur à zéro après une vérification réussie")
        void remetLeCompteurAZero() {
            service.verifyEmail(verification("123456"), IP);

            verify(loginAttemptService).reset("cle");
        }

        @Test
        @DisplayName("refuse tant que le blocage court")
        void refusePendantLeBlocage() {
            doThrow(new TooManyRequestsException("bloque"))
                    .when(loginAttemptService).assertNotBlocked("cle");

            assertThatThrownBy(() -> service.verifyEmail(verification("123456"), IP))
                    .isInstanceOf(TooManyRequestsException.class);

            assertThat(membre.isEmailVerified()).isFalse();
        }

        @Test
        @DisplayName("délivre un jeton si le compte était déjà vérifié")
        void tolereUnCompteDejaVerifie() {
            // Rejouer la vérification n'est pas une attaque : c'est un double
            // clic sur le lien reçu. On rend un jeton plutôt qu'une erreur.
            membre.setEmailVerified(true);

            var reponse = service.verifyEmail(verification("123456"), IP);

            assertThat(reponse.getToken()).isEqualTo("jeton-jwt");
        }

        @Test
        @DisplayName("refuse une adresse inconnue")
        void refuseUneAdresseInconnue() {
            when(repository.findByEmail("fantome@coshift.be")).thenReturn(Optional.empty());
            VerifyEmailRequest r = new VerifyEmailRequest();
            r.setEmail("fantome@coshift.be");
            r.setCode("123456");

            assertThatThrownBy(() -> service.verifyEmail(r, IP))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ═══════════════════════════ F5 — Connexion ═════════════════════════════════

    @Nested
    @DisplayName("Connexion")
    class Connexion {

        @Test
        @DisplayName("délivre un jeton sur des identifiants valides")
        void delivreUnJeton() {
            var reponse = service.authenticate(connexion(COURRIEL), IP);

            assertThat(reponse.getToken()).isEqualTo("jeton-jwt");
            verify(loginAttemptService).reset("cle");
            verify(audit).consigner(eq(SecurityAuditService.Evenement.CONNEXION_REUSSIE),
                    anyString(), anyString());
        }

        @Test
        @DisplayName("comptabilise une adresse inconnue comme un échec")
        void comptabiliseUneAdresseInconnue() {
            // Sans cela, essayer des adresses au hasard resterait entièrement
            // gratuit pour qui cherche à savoir qui est inscrit.
            when(repository.findByEmail("inconnu@coshift.be")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.authenticate(connexion("inconnu@coshift.be"), IP))
                    .isInstanceOf(BadCredentialsException.class);

            verify(loginAttemptService).recordFailure("cle");
        }

        @Test
        @DisplayName("comptabilise un mot de passe erroné")
        void comptabiliseUnMotDePasseErrone() {
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("refuse"));

            assertThatThrownBy(() -> service.authenticate(connexion(COURRIEL), IP))
                    .isInstanceOf(BadCredentialsException.class);

            verify(loginAttemptService).recordFailure("cle");
            verify(audit).consigner(eq(SecurityAuditService.Evenement.CONNEXION_ECHOUEE),
                    anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("refuse un compte non activé sans le comptabiliser")
        void refuseUnCompteNonActiveSansPenaliser() {
            // Ce n'est pas une erreur de mot de passe : comptabiliser ces essais
            // bloquerait un utilisateur légitime qui insiste avant d'avoir lu
            // son courriel de vérification.
            membre.setEmailVerified(false);

            assertThatThrownBy(() -> service.authenticate(connexion(COURRIEL), IP))
                    .isInstanceOf(DisabledException.class);

            verify(loginAttemptService, never()).recordFailure(anyString());
            verify(audit).consigner(eq(SecurityAuditService.Evenement.COMPTE_NON_ACTIVE),
                    anyString(), anyString());
        }

        @Test
        @DisplayName("refuse tant que le blocage court")
        void refusePendantLeBlocage() {
            doThrow(new TooManyRequestsException("bloque"))
                    .when(loginAttemptService).assertNotBlocked("cle");

            assertThatThrownBy(() -> service.authenticate(connexion(COURRIEL), IP))
                    .isInstanceOf(TooManyRequestsException.class);

            verify(jwtService, never()).generateToken(any(User.class));
        }
    }

    // ═════════════════════ F6 — Mot de passe oublié ═════════════════════════════

    @Nested
    @DisplayName("Mot de passe oublié")
    class MotDePasseOublie {

        @Test
        @DisplayName("enregistre un code et l'envoie sur une adresse connue")
        void envoieUnCode() {
            service.forgotPassword(COURRIEL);

            assertThat(membre.getPasswordResetCode()).hasSize(6);
            assertThat(membre.getPasswordResetExpiry()).isAfter(LocalDateTime.now());
            verify(emailService).sendPasswordResetEmail(
                    eq(COURRIEL), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("répond la même chose sur une adresse inconnue")
        void neRevelePasLexistenceDunCompte() {
            // Répondre 404 sur une adresse inconnue transformerait ce point
            // d'entrée, ouvert sans authentification, en annuaire des inscrits.
            when(repository.findByEmail("inconnu@coshift.be")).thenReturn(Optional.empty());

            assertThatCode(() -> service.forgotPassword("inconnu@coshift.be"))
                    .doesNotThrowAnyException();

            verify(emailService, never()).sendPasswordResetEmail(
                    anyString(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("applique le nouveau mot de passe sur le bon code")
        void appliqueLeNouveauMotDePasse() {
            membre.setPasswordResetCode("654321");
            membre.setPasswordResetExpiry(LocalDateTime.now().plusMinutes(30));

            service.resetPassword(COURRIEL, "654321", "NouveauMdp123!", IP);

            assertThat(membre.getPassword()).isEqualTo("nouvelle-empreinte");
            verify(audit).consigner(eq(SecurityAuditService.Evenement.MOT_DE_PASSE_REINITIALISE),
                    anyString(), anyString());
        }

        @Test
        @DisplayName("consomme le code : il ne sert qu'une fois")
        void consommeLeCode() {
            membre.setPasswordResetCode("654321");
            membre.setPasswordResetExpiry(LocalDateTime.now().plusMinutes(30));

            service.resetPassword(COURRIEL, "654321", "NouveauMdp123!", IP);

            assertThat(membre.getPasswordResetCode()).isNull();
            assertThat(membre.getPasswordResetExpiry()).isNull();
        }

        @Test
        @DisplayName("ne modifie pas le statut de vérification de l'adresse")
        void neValidePasLadresse() {
            // Réinitialiser un mot de passe ne prouve pas qu'on possède
            // l'adresse au sens de F7 : un compte jamais activé le reste.
            membre.setEmailVerified(false);
            membre.setPasswordResetCode("654321");
            membre.setPasswordResetExpiry(LocalDateTime.now().plusMinutes(30));

            service.resetPassword(COURRIEL, "654321", "NouveauMdp123!", IP);

            assertThat(membre.isEmailVerified()).isFalse();
        }

        @Test
        @DisplayName("refuse un code erroné")
        void refuseUnCodeErrone() {
            membre.setPasswordResetCode("654321");
            membre.setPasswordResetExpiry(LocalDateTime.now().plusMinutes(30));

            assertThatThrownBy(() ->
                    service.resetPassword(COURRIEL, "000000", "NouveauMdp123!", IP))
                    .isInstanceOf(BadRequestException.class);

            assertThat(membre.getPassword()).isEqualTo("empreinte");
            verify(loginAttemptService).recordFailure("cle");
        }

        @Test
        @DisplayName("refuse un code expiré")
        void refuseUnCodeExpire() {
            membre.setPasswordResetCode("654321");
            membre.setPasswordResetExpiry(LocalDateTime.now().minusMinutes(1));

            assertThatThrownBy(() ->
                    service.resetPassword(COURRIEL, "654321", "NouveauMdp123!", IP))
                    .isInstanceOf(BadRequestException.class);

            assertThat(membre.getPassword()).isEqualTo("empreinte");
        }

        @Test
        @DisplayName("refuse quand aucune demande n'est en cours")
        void refuseSansDemandeEnCours() {
            membre.setPasswordResetCode(null);

            assertThatThrownBy(() ->
                    service.resetPassword(COURRIEL, "654321", "NouveauMdp123!", IP))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("répond la même erreur sur une adresse inconnue")
        void memeErreurSurAdresseInconnue() {
            // Distinguer « adresse inconnue » de « code faux » renseignerait un
            // appelant sur l'existence du compte.
            when(repository.findByEmail("inconnu@coshift.be")).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.resetPassword("inconnu@coshift.be", "654321", "Mdp123!", IP))
                    .isInstanceOf(BadRequestException.class);

            verify(loginAttemptService).recordFailure("cle");
        }
    }

    // ═════════════════════ F7 — Renvoi du code ══════════════════════════════════

    @Nested
    @DisplayName("Renvoi du code d'activation")
    class RenvoiDuCode {

        @Test
        @DisplayName("génère un nouveau code et le renvoie")
        void renvoieUnNouveauCode() {
            membre.setEmailVerified(false);
            membre.setVerificationCode("111111");

            service.resendVerificationCode(COURRIEL);

            assertThat(membre.getVerificationCode()).hasSize(6);
            assertThat(membre.getVerificationCode()).isNotEqualTo("111111");
            verify(emailService).sendVerificationEmail(
                    eq(COURRIEL), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("refuse pour un compte déjà activé")
        void refusePourUnCompteDejaActive() {
            assertThatThrownBy(() -> service.resendVerificationCode(COURRIEL))
                    .isInstanceOf(ConflictException.class);

            verify(emailService, never()).sendVerificationEmail(
                    anyString(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("refuse une adresse inconnue")
        void refuseUneAdresseInconnue() {
            when(repository.findByEmail("inconnu@coshift.be")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resendVerificationCode("inconnu@coshift.be"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ────────────────────────────────── Fabriques ───────────────────────────────

    private RegisterRequest inscription(String courriel) {
        RegisterRequest r = new RegisterRequest();
        r.setFirstname("Alex");
        r.setLastname("Martin");
        r.setEmail(courriel);
        r.setPassword("MotDePasse123!");
        r.setAcceptedTerms(true);
        return r;
    }

    private LoginRequest connexion(String courriel) {
        LoginRequest r = new LoginRequest();
        r.setEmail(courriel);
        r.setPassword("MotDePasse123!");
        return r;
    }

    private VerifyEmailRequest verification(String code) {
        VerifyEmailRequest r = new VerifyEmailRequest();
        r.setEmail(COURRIEL);
        r.setCode(code);
        return r;
    }
}
