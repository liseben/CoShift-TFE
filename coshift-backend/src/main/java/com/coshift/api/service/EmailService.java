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

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Async
    public void sendVerificationEmail(String toEmail, String firstname, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, "CoShift");
            helper.setTo(toEmail);
            helper.setSubject("CoShift - Validation de votre compte (" + code + ")");
            helper.setText(buildVerificationEmailHtml(firstname, code), true);

            mailSender.send(message);
            log.info("Email de vérification envoyé à {}", toEmail);

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Impossible d'envoyer l'email de vérification à {} : {}", toEmail, e.getMessage());
        }
    }

    private String buildVerificationEmailHtml(String firstname, String code) {
        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
              <meta charset="UTF-8">
              <style>
                body { font-family: 'Segoe UI', sans-serif; background: #0f172a; color: #e2e8f0; margin: 0; padding: 20px; }
                .container { max-width: 520px; margin: 0 auto; background: rgba(255,255,255,0.05);
                             border: 1px solid rgba(255,255,255,0.1); border-radius: 16px; padding: 40px; }
                .logo { font-size: 28px; font-weight: 800; color: #60a5fa; margin-bottom: 8px; }
                .logo span { color: #e2e8f0; }
                h2 { font-size: 22px; margin-bottom: 8px; }
                p { color: #94a3b8; line-height: 1.6; }
                .code-box { background: rgba(96,165,250,0.1); border: 2px solid #60a5fa;
                            border-radius: 12px; text-align: center; padding: 24px; margin: 28px 0; }
                .code { font-size: 42px; font-weight: 900; letter-spacing: 12px; color: #60a5fa; }
                .expiry { font-size: 13px; color: #64748b; margin-top: 8px; }
                .footer { margin-top: 32px; font-size: 12px; color: #475569; border-top: 1px solid rgba(255,255,255,0.08); padding-top: 16px; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="logo">Co<span>Shift</span></div>
                <h2>Bienvenue, %s ! 👋</h2>
                <p>Votre compte CoShift a été créé avec succès. Pour l'activer, entrez le code ci-dessous dans l'application :</p>
                <div class="code-box">
                  <div class="code">%s</div>
                  <div class="expiry">Ce code expire dans 24 heures</div>
                </div>
                <p>Si vous n'avez pas créé de compte, ignorez cet email.</p>
                <div class="footer">
                  © 2026 CoShift — Plateforme de covoiturage organisationnel<br>
                  Cet email a été envoyé automatiquement, merci de ne pas y répondre.
                </div>
              </div>
            </body>
            </html>
            """.formatted(firstname, code);
    }
}
