package com.coshift.api.exception;

import com.coshift.api.security.SecurityAuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Traduction des exceptions en réponses HTTP.
 *
 * <p>Ce fichier est le contrat que le client consomme : le frontend lit
 * {@code error.response.status} pour décider quoi faire et
 * {@code error.response.data.message} pour l'afficher. Une exception mal
 * classée ne provoque aucune erreur côté serveur — elle produit simplement un
 * écran qui dit la mauvaise chose.</p>
 *
 * <p>L'histoire du fichier justifie ces tests. Un unique
 * {@code @ExceptionHandler(Exception.class)} interceptait tout et renvoyait
 * 500 « Une erreur inattendue est survenue » pour un trajet introuvable comme
 * pour une panne de base — en avalant au passage la trace, qui disparaissait
 * sans laisser de trace.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GlobalExceptionHandler — réponses HTTP")
class GlobalExceptionHandlerTest {

    @Mock private SecurityAuditService audit;
    @Mock private WebRequest webRequest;
    @Mock private HttpServletRequest httpRequest;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void preparer() {
        handler = new GlobalExceptionHandler(audit);
        when(webRequest.getDescription(false)).thenReturn("uri=/api/trips/abc");
        when(httpRequest.getRemoteAddr()).thenReturn("203.0.113.7");
        when(httpRequest.getMethod()).thenReturn("GET");
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Correspondance exception → statut")
    class Correspondance {

        @Test
        @DisplayName("400 sur une requête invalide")
        void quatreCentSurRequeteInvalide() {
            var reponse = handler.handleBadRequest(new BadRequestException("champ absent"), webRequest);

            assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(reponse.getBody().message()).isEqualTo("champ absent");
        }

        @Test
        @DisplayName("400 sur un argument illégal")
        void quatreCentSurArgumentIllegal() {
            var reponse = handler.handleBadRequest(new IllegalArgumentException("valeur"), webRequest);

            assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("400 sur un trajet complet")
        void quatreCentSurTrajetComplet() {
            var reponse = handler.handleNoSeats(
                    new NoSeatsAvailableException("plus de place"), webRequest);

            assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(reponse.getBody().error()).isEqualTo("Full Trip");
        }

        @Test
        @DisplayName("403 sur un compte non activé")
        void quatreCentTroisSurCompteNonActive() {
            // Et non 401 : le mot de passe était bon, c'est le compte qui n'est
            // pas prêt. Le client doit conduire vers la saisie du code, pas vers
            // une nouvelle tentative de connexion.
            var reponse = handler.handleDisabled(new DisabledException("non activé"), webRequest);

            assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("404 sur une ressource absente")
        void quatreCentQuatreSurRessourceAbsente() {
            var reponse = handler.handleNotFound(
                    new ResourceNotFoundException("introuvable"), webRequest);

            assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("404 sur un trajet absent")
        void quatreCentQuatreSurTrajetAbsent() {
            // Cette exception ne prend qu'un identifiant : elle compose son
            // message elle-meme.
            var reponse = handler.handleNotFound(new TripNotFoundException(42L), webRequest);

            assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(reponse.getBody().message()).contains("42");
        }

        @Test
        @DisplayName("409 sur un conflit d'état")
        void quatreCentNeufSurConflit() {
            var reponse = handler.handleConflict(new ConflictException("déjà pris"), webRequest);

            assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("409 sur un état illégal")
        void quatreCentNeufSurEtatIllegal() {
            var reponse = handler.handleConflict(new IllegalStateException("état"), webRequest);

            assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("413 sur un fichier trop lourd")
        void quatreCentTreizeSurFichierTropLourd() {
            var reponse = handler.handleUploadTooLarge(
                    new MaxUploadSizeExceededException(2_097_152L), webRequest);

            assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
        }

        @Test
        @DisplayName("429 quand le freinage des tentatives se déclenche")
        void quatreCentVingtNeufSurFreinage() {
            var reponse = handler.handleTooManyRequests(
                    new TooManyRequestsException("réessayez dans 15 minutes"), webRequest);

            assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            assertThat(reponse.getBody().message()).contains("15 minutes");
        }

        @Test
        @DisplayName("500 en dernier recours")
        void cinqCentEnDernierRecours() {
            var reponse = handler.handleUnexpected(new RuntimeException("panne"), webRequest);

            assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("Ce qui ne doit pas fuiter")
    class Confidentialite {

        @Test
        @DisplayName("ne dit pas si l'adresse existe")
        void neRevelePasLexistenceDunCompte() {
            // Message volontairement identique dans les deux cas : le distinguer
            // transformerait l'écran de connexion en annuaire des inscrits.
            var refusees = handler.handleBadCredentials(
                    new BadCredentialsException("mot de passe faux"), webRequest);
            var inconnue = handler.handleBadCredentials(
                    new UsernameNotFoundException("adresse inconnue"), webRequest);

            assertThat(refusees.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(inconnue.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(refusees.getBody().message()).isEqualTo(inconnue.getBody().message());
        }

        @Test
        @DisplayName("ne renvoie aucun détail d'implémentation sur une erreur inattendue")
        void neFuitPasLimplementation() {
            // Le message de l'exception peut contenir un nom de table, un chemin
            // de fichier, une requête. Il reste au journal, côté serveur.
            var reponse = handler.handleUnexpected(
                    new IllegalStateException("Table 'coshift_db.users' doesn't exist"), webRequest);

            assertThat(reponse.getBody().message()).doesNotContain("coshift_db");
            assertThat(reponse.getBody().message()).isEqualTo("Une erreur inattendue est survenue.");
        }
    }

    @Nested
    @DisplayName("Accès refusé")
    class AccesRefuse {

        @Test
        @DisplayName("répond 403 et consigne l'événement")
        void consigneLaccesRefuse() {
            // Neuf contrôles de propriété refusaient correctement l'accès sans
            // en garder trace : un compte méthodiquement sondé ne se distinguait
            // pas d'un utilisateur maladroit.
            var reponse = handler.handleForbidden(
                    new UnauthorizedException("pas à vous"), webRequest, httpRequest);

            assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            verify(audit).consigner(eq(SecurityAuditService.Evenement.ACCES_REFUSE),
                    any(), eq("203.0.113.7"), anyString());
        }

        @Test
        @DisplayName("consigne le compte quand il est connu")
        void consigneLeCompte() {
            Authentication auth = new org.springframework.security.authentication
                    .UsernamePasswordAuthenticationToken("membre@coshift.be", null);
            SecurityContextHolder.getContext().setAuthentication(auth);

            handler.handleForbidden(new AccessDeniedException("refusé"), webRequest, httpRequest);

            verify(audit).consigner(eq(SecurityAuditService.Evenement.ACCES_REFUSE),
                    eq("membre@coshift.be"), anyString(), anyString());
        }

        @Test
        @DisplayName("consigne la méthode et le chemin visés")
        void consigneLeChemin() {
            handler.handleForbidden(new SecurityException("refusé"), webRequest, httpRequest);

            verify(audit).consigner(any(), any(), anyString(),
                    eq("GET /api/trips/abc"));
        }
    }

    @Nested
    @DisplayName("Validation des formulaires")
    class Validation {

        @Test
        @DisplayName("remonte les messages de chaque champ fautif")
        void remonteLesMessagesDeChamp() {
            var ex = validationAvec(
                    new FieldError("objet", "email", "L'adresse est invalide."),
                    new FieldError("objet", "password", "Le mot de passe est trop court."));

            var reponse = handler.handleValidation(ex, webRequest);

            assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(reponse.getBody().message())
                    .contains("L'adresse est invalide.")
                    .contains("Le mot de passe est trop court.");
        }

        @Test
        @DisplayName("ne répète pas un message porté par plusieurs champs")
        void neRepetePasUnMessage() {
            var ex = validationAvec(
                    new FieldError("objet", "a", "Ce champ est requis."),
                    new FieldError("objet", "b", "Ce champ est requis."));

            String message = handler.handleValidation(ex, webRequest).getBody().message();

            assertThat(message).isEqualTo("Ce champ est requis.");
        }

        @Test
        @DisplayName("rend un message par défaut si aucun champ n'en porte")
        void messageParDefaut() {
            var ex = validationAvec(new FieldError("objet", "a", (String) null));

            assertThat(handler.handleValidation(ex, webRequest).getBody().message())
                    .isEqualTo("Certains champs du formulaire sont invalides.");
        }
    }

    @Nested
    @DisplayName("Forme du corps d'erreur")
    class FormeDuCorps {

        @Test
        @DisplayName("porte l'horodatage, le statut, le libellé et le chemin")
        void porteLesQuatreChamps() {
            ErrorResponse corps = handler.handleNotFound(
                    new ResourceNotFoundException("absent"), webRequest).getBody();

            assertThat(corps.timestamp()).isNotNull();
            assertThat(corps.status()).isEqualTo(404);
            assertThat(corps.error()).isEqualTo("Not Found");
            assertThat(corps.message()).isEqualTo("absent");
        }

        @Test
        @DisplayName("retire le préfixe technique du chemin")
        void retireLePrefixeTechnique() {
            // WebRequest rend « uri=/api/... » : le préfixe fuitait tel quel dans
            // le champ path de chaque erreur, obligeant les clients à le retirer
            // eux-mêmes.
            ErrorResponse corps = handler.handleNotFound(
                    new ResourceNotFoundException("absent"), webRequest).getBody();

            assertThat(corps.path()).isEqualTo("/api/trips/abc");
            assertThat(corps.path()).doesNotStartWith("uri=");
        }

        @Test
        @DisplayName("retombe sur le libellé du statut quand l'exception n'a pas de message")
        void libelleParDefaut() {
            ErrorResponse corps = handler.handleConflict(
                    new ConflictException("   "), webRequest).getBody();

            assertThat(corps.message()).isEqualTo(HttpStatus.CONFLICT.getReasonPhrase());
        }
    }

    // ────────────────────────────────── Fabriques ───────────────────────────────

    private MethodArgumentNotValidException validationAvec(FieldError... erreurs) {
        BindingResult liaison = new BeanPropertyBindingResult(new Object(), "objet");
        for (FieldError e : erreurs) liaison.addError(e);
        return new MethodArgumentNotValidException((MethodParameter) null, liaison);
    }
}
