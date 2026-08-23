package com.coshift.api.security;

import com.coshift.api.exception.TooManyRequestsException;
import com.coshift.api.service.Messages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Freinage des tentatives répétées.
 *
 * <p>Le choix de la clé est ce qui se teste ici, plus encore que le seuil. La
 * compter par compte seul permettrait à n'importe qui de verrouiller le compte
 * d'un tiers en échouant volontairement cinq fois — un déni de service à la
 * portée du premier venu. La compter par adresse IP seule bloquerait tout un
 * site derrière une même sortie NAT dès que cinq collègues se trompent, cas
 * courant pour une plateforme d'entreprise. Les deux derniers tests
 * garantissent qu'aucune de ces deux régressions ne peut passer inaperçue.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LoginAttemptService — freinage des tentatives")
class LoginAttemptServiceTest {

    @Mock private Messages messages;
    @Mock private SecurityAuditService audit;

    @InjectMocks private LoginAttemptService service;

    private static final String IP = "203.0.113.7";
    private static final String COMPTE = "victime@coshift.be";

    @BeforeEach
    void preparer() {
        when(messages.get(anyString(), any())).thenReturn("Trop de tentatives.");
        when(messages.get(anyString())).thenReturn("Trop de tentatives.");
    }

    @Test
    @DisplayName("laisse passer tant que le seuil n'est pas atteint")
    void laissePasserSousLeSeuil() {
        String cle = service.key(IP, COMPTE);

        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS - 1; i++) {
            service.recordFailure(cle);
        }

        assertThatCode(() -> service.assertNotBlocked(cle)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("bloque au cinquième échec")
    void bloqueAuSeuil() {
        String cle = service.key(IP, COMPTE);

        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            service.recordFailure(cle);
        }

        assertThatThrownBy(() -> service.assertNotBlocked(cle))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    @DisplayName("consigne le blocage au journal de sécurité")
    void consigneLeBlocage() {
        String cle = service.key(IP, COMPTE);

        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            service.recordFailure(cle);
        }

        verify(audit, atLeastOnce()).consigner(
                org.mockito.ArgumentMatchers.eq(SecurityAuditService.Evenement.BLOCAGE_TENTATIVES),
                anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("consigne l'acharnement sur une clé déjà bloquée")
    void consigneLacharnement() {
        String cle = service.key(IP, COMPTE);
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            service.recordFailure(cle);
        }

        assertThatThrownBy(() -> service.assertNotBlocked(cle))
                .isInstanceOf(TooManyRequestsException.class);

        verify(audit, atLeastOnce()).consigner(
                org.mockito.ArgumentMatchers.eq(SecurityAuditService.Evenement.TENTATIVE_PENDANT_BLOCAGE),
                anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("une connexion réussie remet le compteur à zéro")
    void reussiteRemetAZero() {
        String cle = service.key(IP, COMPTE);
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS - 1; i++) {
            service.recordFailure(cle);
        }

        service.reset(cle);
        service.recordFailure(cle);

        // Sans la remise à zéro, ce sixième échec cumulé aurait bloqué.
        assertThatCode(() -> service.assertNotBlocked(cle)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("bloquer un compte n'en bloque pas un autre depuis la même adresse")
    void nePenalisePasLesVoisinsDeNat() {
        String cible = service.key(IP, COMPTE);
        String collegue = service.key(IP, "collegue@coshift.be");

        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            service.recordFailure(cible);
        }

        assertThatThrownBy(() -> service.assertNotBlocked(cible))
                .isInstanceOf(TooManyRequestsException.class);
        assertThatCode(() -> service.assertNotBlocked(collegue)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("un tiers ne peut pas verrouiller le compte d'autrui depuis son adresse")
    void nePermetPasLeDeniDeService() {
        String depuisLattaquant = service.key("198.51.100.9", COMPTE);
        String depuisLaVictime = service.key(IP, COMPTE);

        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            service.recordFailure(depuisLattaquant);
        }

        // La victime, depuis sa propre adresse, doit pouvoir se connecter.
        assertThatCode(() -> service.assertNotBlocked(depuisLaVictime)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("la clé ignore la casse de l'adresse électronique")
    void cleInsensibleALaCasse() {
        assertThat(service.key(IP, "Victime@CoShift.BE"))
                .isEqualTo(service.key(IP, "victime@coshift.be"));
    }

    @Test
    @DisplayName("la clé tolère une adresse absente")
    void cleToleranteAUneAdresseAbsente() {
        assertThatCode(() -> service.key(IP, null)).doesNotThrowAnyException();
        assertThat(service.key(IP, null)).startsWith(IP);
    }

    @Test
    @DisplayName("une clé jamais vue n'est pas bloquée")
    void cleInconnueNonBloquee() {
        assertThatCode(() -> service.assertNotBlocked(service.key(IP, "inconnu@coshift.be")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("la purge conserve les clés encore bloquées")
    void purgeConserveLesBlocages() {
        String cle = service.key(IP, COMPTE);
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            service.recordFailure(cle);
        }

        service.purgeExpired();

        // Purger ne doit pas relâcher un blocage en cours : ce serait une porte
        // de sortie gratuite pour qui attend le passage de la tâche.
        assertThatThrownBy(() -> service.assertNotBlocked(cle))
                .isInstanceOf(TooManyRequestsException.class);
    }
}
