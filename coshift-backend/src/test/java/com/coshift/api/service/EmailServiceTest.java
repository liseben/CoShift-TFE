package com.coshift.api.service;

import com.coshift.api.entity.User;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
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
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Locale;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Composition des courriels.
 *
 * <h2>Ce qui se teste ici, et ce qui ne s'y teste pas</h2>
 *
 * <p>Le rendu visuel d'un gabarit HTML ne se vérifie pas par un test : il se
 * regarde. La remise effective au serveur SMTP non plus — elle dépend d'un
 * tiers. Écrire des tests qui traversent ces lignes sans rien affirmer ferait
 * monter un pourcentage sans rien garantir.</p>
 *
 * <p>Trois choses, en revanche, sont des décisions du code et se cassent en
 * silence : le destinataire, la langue dans laquelle on lui écrit, et
 * l'échappement d'un texte qu'un membre a saisi et qu'on réinjecte dans un
 * document HTML envoyé à quelqu'un d'autre. Ce sont celles-là qui sont
 * couvertes.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EmailService — composition des courriels")
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;
    @Mock private Messages messages;

    @InjectMocks private EmailService service;

    private User francophone;
    private User anglophone;

    private static final String TRAJET = "Namur → Bruxelles<br>02/09/2026 à 08h00";

    @BeforeEach
    void preparer() {
        ReflectionTestUtils.setField(service, "fromAddress", "no-reply@coshift.be");
        ReflectionTestUtils.setField(service, "siteUrl", "https://coshift.be");

        // Un vrai MimeMessage, hors session réseau : il se laisse relire.
        when(mailSender.createMimeMessage())
                .thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));

        // Le catalogue rend la langue demandée : c'est ainsi qu'on vérifie
        // laquelle le service a choisie.
        when(messages.get(any(Locale.class), anyString()))
                .thenAnswer(i -> "[" + ((Locale) i.getArgument(0)).getLanguage() + "]");
        when(messages.get(any(Locale.class), anyString(), any()))
                .thenAnswer(i -> "[" + ((Locale) i.getArgument(0)).getLanguage() + "]");

        francophone = membre("camille@coshift.be", "Camille", "fr");
        anglophone = membre("sam@coshift.be", "Sam", "en");
    }

    private MimeMessage envoye() throws Exception {
        ArgumentCaptor<MimeMessage> capture = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(capture.capture());
        return capture.getValue();
    }

    /**
     * Texte du message.
     *
     * <p>{@code MimeMessageHelper} est construit en mode multipart : le contenu
     * n'est donc pas une chaîne mais un arbre de parties, éventuellement
     * imbriquées. On le parcourt pour retrouver le HTML.</p>
     */
    private String corps(MimeMessage m) throws Exception {
        StringBuilder texte = new StringBuilder();
        aplatir(m.getContent(), texte);
        return texte.toString();
    }

    private void aplatir(Object contenu, StringBuilder sortie) throws Exception {
        if (contenu instanceof jakarta.mail.Multipart parties) {
            for (int i = 0; i < parties.getCount(); i++) {
                aplatir(parties.getBodyPart(i).getContent(), sortie);
            }
        } else {
            sortie.append(contenu);
        }
    }

    @Nested
    @DisplayName("Destinataire")
    class Destinataire {

        @Test
        @DisplayName("écrit à l'adresse du membre passé en paramètre")
        void ecritAuBonMembre() throws Exception {
            service.notifierDemandeRecue(francophone, "Sam", TRAJET, 2);

            assertThat(envoye().getAllRecipients()[0].toString())
                    .isEqualTo("camille@coshift.be");
        }

        @Test
        @DisplayName("part de l'adresse d'expédition configurée")
        void partDeLadresseConfiguree() throws Exception {
            service.notifierReservationAcceptee(anglophone, "Camille", TRAJET);

            assertThat(envoye().getFrom()[0].toString()).contains("no-reply@coshift.be");
        }
    }

    @Nested
    @DisplayName("Langue")
    class Langue {

        @Test
        @DisplayName("écrit au francophone en français")
        void francophoneEnFrancais() throws Exception {
            service.notifierDemandeRecue(francophone, "Sam", TRAJET, 1);

            assertThat(envoye().getSubject()).isEqualTo("[fr]");
        }

        @Test
        @DisplayName("écrit à l'anglophone en anglais")
        void anglophoneEnAnglais() throws Exception {
            // Le déclencheur pourrait être francophone : c'est le destinataire
            // qui décide, et lui seul.
            service.notifierReservationAcceptee(anglophone, "Camille", TRAJET);

            assertThat(envoye().getSubject()).isEqualTo("[en]");
        }

        @Test
        @DisplayName("annonce la langue dans l'attribut lang du document")
        void annonceLaLangueDansLeDocument() throws Exception {
            // Les lecteurs de courrier qui proposent une traduction s'y fient,
            // et une synthèse vocale lit un texte anglais avec l'accent
            // français si on lui annonce du français.
            service.notifierTrajetAnnule(anglophone, TRAJET);

            assertThat(corps(envoye())).contains("<html lang=\"en\">");
        }

        @Test
        @DisplayName("retombe sur le français quand la langue n'a jamais été relevée")
        void repliSurLeFrancais() throws Exception {
            service.notifierTrajetAnnule(membre("ancien@coshift.be", "Dominique", null), TRAJET);

            assertThat(envoye().getSubject()).isEqualTo("[fr]");
        }
    }

    @Nested
    @DisplayName("Contenu d'une notification")
    class Contenu {

        @Test
        @DisplayName("rappelle le trajet concerné")
        void rappelleLeTrajet() throws Exception {
            service.notifierDemandeRecue(francophone, "Sam", TRAJET, 2);

            assertThat(corps(envoye())).contains("Namur → Bruxelles");
        }

        @Test
        @DisplayName("porte un lien qui ramène là où l'action se poursuit")
        void porteUnLienUtile() throws Exception {
            // Une notification qui ne dit pas quoi faire ensuite oblige à
            // retrouver l'écran soi-même, ce qui revient à ne pas l'avoir
            // envoyée.
            service.notifierDemandeRecue(francophone, "Sam", TRAJET, 1);

            assertThat(corps(envoye())).contains("https://coshift.be/dashboard?tab=requests");
        }

        @Test
        @DisplayName("renvoie le passager vers ses réservations après une acceptation")
        void lienVersLesReservations() throws Exception {
            service.notifierReservationAcceptee(anglophone, "Camille", TRAJET);

            assertThat(corps(envoye())).contains("https://coshift.be/bookings");
        }

        @Test
        @DisplayName("renvoie vers la recherche après un refus ou une annulation")
        void lienVersLaRecherche() throws Exception {
            service.notifierTrajetAnnule(anglophone, TRAJET);

            assertThat(corps(envoye())).contains("https://coshift.be/trips/search");
        }

        @Test
        @DisplayName("n'affiche aucun encart de code sur une notification")
        void pasDeCodeSurUneNotification() throws Exception {
            // Le gabarit à code et celui des notifications partagent la même
            // enveloppe : rien ne doit fuiter de l'un vers l'autre.
            service.notifierDemandeRecue(francophone, "Sam", TRAJET, 1);

            assertThat(corps(envoye())).doesNotContain("class=\"code-box\"");
        }
    }

    @Nested
    @DisplayName("Motif de refus")
    class MotifDeRefus {

        @Test
        @DisplayName("reprend le motif rédigé par le conducteur")
        void reprendLeMotif() throws Exception {
            service.notifierReservationRefusee(anglophone, TRAJET, "Voiture pleine");

            assertThat(corps(envoye())).contains("Voiture pleine");
        }

        @Test
        @DisplayName("échappe le HTML d'un motif malveillant")
        void echappeLeHtml() throws Exception {
            // Le motif est saisi librement par un membre et réinjecté dans un
            // document HTML envoyé à quelqu'un d'autre. Sans échappement, il y
            // ferait passer n'importe quelle balise.
            service.notifierReservationRefusee(anglophone, TRAJET,
                    "<script>alert(1)</script>");

            String html = corps(envoye());
            assertThat(html).doesNotContain("<script>");
            assertThat(html).contains("&lt;script&gt;");
        }

        @Test
        @DisplayName("échappe aussi les guillemets et les esperluettes")
        void echappeGuillemetsEtEsperluettes() throws Exception {
            service.notifierReservationRefusee(anglophone, TRAJET, "\"Trop\" tard & complet");

            String html = corps(envoye());
            assertThat(html).contains("&quot;Trop&quot;");
            assertThat(html).contains("&amp;");
        }

        @Test
        @DisplayName("se passe du bloc de motif quand il n'y en a pas")
        void toleUnMotifAbsent() throws Exception {
            assertThatCode(() -> service.notifierReservationRefusee(anglophone, TRAJET, null))
                    .doesNotThrowAnyException();

            assertThat(corps(envoye())).contains("Namur → Bruxelles");
        }

        @Test
        @DisplayName("ignore un motif réduit à des espaces")
        void ignoreUnMotifVide() throws Exception {
            service.notifierReservationRefusee(anglophone, TRAJET, "   ");

            assertThat(corps(envoye())).doesNotContain("<em>");
        }
    }

    @Nested
    @DisplayName("Panne du service de messagerie")
    class Panne {

        @Test
        @DisplayName("n'interrompt pas l'appelant")
        void nInterromptPasLappelant() {
            // Choix délibéré : une réservation ne doit pas échouer parce que le
            // courriel ne part pas. L'incident est journalisé, la sonde de
            // santé le signale.
            doThrow(new MailSendException("serveur injoignable"))
                    .when(mailSender).send(any(MimeMessage.class));

            assertThatCode(() -> service.notifierDemandeRecue(francophone, "Sam", TRAJET, 1))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("n'interrompt pas non plus l'annulation d'un trajet")
        void nInterromptPasLannulation() {
            doThrow(new MailSendException("serveur injoignable"))
                    .when(mailSender).send(any(MimeMessage.class));

            assertThatCode(() -> service.notifierTrajetAnnule(anglophone, TRAJET))
                    .doesNotThrowAnyException();
        }
    }

    // ────────────────────────────────── Fabriques ───────────────────────────────

    private User membre(String courriel, String prenom, String langue) {
        return User.builder()
                .id(1L).uuid("uuid-1")
                .email(courriel)
                .firstname(prenom).lastname("Nom")
                .password("peu-importe")
                .emailVerified(true)
                .preferredLanguage(langue)
                .build();
    }
}
