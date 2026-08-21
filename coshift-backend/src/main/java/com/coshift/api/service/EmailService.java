package com.coshift.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.util.Locale;

/**
 * Courriels transactionnels.
 *
 * <h2>La langue voyage avec l'appel</h2>
 *
 * <p>L'envoi est asynchrone : il quitte le fil de la requête, et avec lui le
 * contexte qui porte la langue. Une méthode qui interrogerait
 * {@code LocaleContextHolder} au moment de composer le message trouverait donc
 * un contexte vide et retomberait sur le français, quelle que soit la langue
 * demandée par la personne.</p>
 *
 * <p>La langue est par conséquent un <strong>paramètre explicite</strong>,
 * capturé par l'appelant pendant qu'il est encore dans le fil de la requête.
 * C'est plus verbeux et c'est la seule façon d'obtenir le bon résultat.</p>
 *
 * <h2>Un seul gabarit</h2>
 *
 * <p>Les deux courriels partageaient soixante lignes de style identiques,
 * recopiées. Toute retouche de charte devait donc être faite deux fois, et le
 * second exemplaire a fini par diverger du premier. Le gabarit est unique et
 * reçoit ce qui change : un titre, un texte d'introduction, un code, une
 * mention de validité et un avertissement.</p>
 *
 * <h2>Ce qui ne fonctionne pas aujourd'hui</h2>
 *
 * <p>L'envoi échoue : le service de messagerie refuse les identifiants
 * configurés. L'échec est capturé et journalisé, donc silencieux pour
 * l'appelant — un choix délibéré, une inscription ne devant pas échouer parce
 * que le courriel ne part pas, mais qui masque le problème. La sonde de santé
 * le signale.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final Messages messages;

    @Value("${spring.mail.username}")
    private String fromAddress;

    /** Vérification d'adresse à l'inscription. */
    @Async
    public void sendVerificationEmail(String toEmail, String firstname, String code, Locale langue) {
        envoyer(
                toEmail,
                langue,
                messages.get(langue, "courriel.verification.sujet") + " (" + code + ")",
                gabarit(langue, code,
                        messages.get(langue, "courriel.verification.titre"),
                        messages.get(langue, "courriel.verification.bonjour", firstname),
                        messages.get(langue, "courriel.verification.intro"),
                        messages.get(langue, "courriel.verification.validite"),
                        messages.get(langue, "courriel.verification.ignorer")),
                "vérification");
    }

    /** F6 — code à six chiffres permettant de choisir un nouveau mot de passe. */
    @Async
    public void sendPasswordResetEmail(String toEmail, String firstname, String code, Locale langue) {
        envoyer(
                toEmail,
                langue,
                messages.get(langue, "courriel.reset.sujet") + " (" + code + ")",
                gabarit(langue, code,
                        messages.get(langue, "courriel.reset.titre"),
                        messages.get(langue, "courriel.verification.bonjour", firstname),
                        messages.get(langue, "courriel.reset.intro"),
                        messages.get(langue, "courriel.reset.validite"),
                        messages.get(langue, "courriel.reset.ignorer")),
                "réinitialisation");
    }

    private void envoyer(String destinataire, Locale langue, String sujet, String corps, String nature) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, "CoShift");
            helper.setTo(destinataire);
            helper.setSubject(sujet);
            helper.setText(corps, true);

            mailSender.send(message);
            log.info("Courriel de {} envoyé à {} en {}", nature, destinataire, langue.getLanguage());

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Courriel de {} non envoyé à {} : {}", nature, destinataire, e.getMessage());
        }
    }

    /**
     * Gabarit commun aux deux courriels.
     *
     * <p>L'attribut {@code lang} du document suit la langue du message : les
     * lecteurs de courrier qui proposent une traduction automatique s'y fient,
     * et une synthèse vocale lit un texte anglais avec l'accent français si on
     * lui annonce du français.</p>
     *
     * <p>Le style reste en ligne : les clients de messagerie ignorent
     * largement les feuilles de style externes, et beaucoup rognent même les
     * blocs {@code <style>} — d'où la duplication apparente entre l'en-tête et
     * les attributs.</p>
     */
    private String gabarit(Locale langue, String code, String titre, String bonjour,
                           String intro, String validite, String avertissement) {
        return """
            <!DOCTYPE html>
            <html lang="%s">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background: #0f172a; color: #e2e8f0; margin: 0; padding: 20px; }
                .container { max-width: 520px; margin: 0 auto; background: rgba(255,255,255,0.05);
                             border: 1px solid rgba(255,255,255,0.1); border-radius: 16px; padding: 40px; }
                .logo { font-size: 28px; font-weight: 800; color: #60a5fa; margin-bottom: 8px; }
                .logo span { color: #e2e8f0; }
                h2 { font-size: 22px; margin-bottom: 8px; color: #e2e8f0; }
                p { color: #94a3b8; line-height: 1.6; }
                .code-box { background: rgba(96,165,250,0.1); border: 2px solid #60a5fa;
                            border-radius: 12px; text-align: center; padding: 24px; margin: 28px 0; }
                .code { font-size: 42px; font-weight: 900; letter-spacing: 12px; color: #60a5fa; }
                .expiry { font-size: 13px; color: #64748b; margin-top: 8px; }
                .warn { color: #fcd34d; }
                .footer { margin-top: 32px; font-size: 12px; color: #475569;
                          border-top: 1px solid rgba(255,255,255,0.08); padding-top: 16px; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="logo">Co<span>Shift</span></div>
                <h2>%s</h2>
                <p>%s</p>
                <p>%s</p>
                <div class="code-box">
                  <div class="code">%s</div>
                  <div class="expiry">%s</div>
                </div>
                <p class="warn">%s</p>
                <div class="footer">
                  %s<br>
                  %s
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                langue.getLanguage(),
                titre,
                bonjour,
                intro,
                code,
                validite,
                avertissement,
                messages.get(langue, "courriel.pied"),
                messages.get(langue, "courriel.piedAuto"));
    }
}
