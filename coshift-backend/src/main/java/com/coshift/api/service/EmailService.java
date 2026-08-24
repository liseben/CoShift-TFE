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

import com.coshift.api.entity.User;

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
 *
 * <p>La capture porte volontairement sur {@code Exception} et non sur les
 * seules exceptions vérifiées : l'échec réel vient de l'envoi lui-même, qui
 * lève une {@code MailException} non vérifiée.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final Messages messages;

    @Value("${spring.mail.username}")
    private String fromAddress;

    /** Domaine du site, pour que le bouton d'une notification mène quelque part. */
    @Value("${app.public-base-url:http://localhost:5173}")
    private String siteUrl;

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

    /* ═══════════════════════ Notifications (F19, F20, F29) ══════════════════════
     *
     * Jusqu'ici, CoShift n'écrivait qu'à propos du compte lui-même : vérifier
     * une adresse, réinitialiser un mot de passe. Rien ne prévenait des
     * événements qui font pourtant vivre la plateforme. Un conducteur devait
     * ouvrir son tableau de bord et regarder pour découvrir qu'on l'avait
     * sollicité ; un passager n'apprenait pas que son trajet du lendemain était
     * annulé. Les données circulaient, la mise en relation ne se refermait
     * jamais.
     *
     * Toutes ces méthodes reçoivent le destinataire en entier plutôt que son
     * adresse : c'est lui qui porte la langue dans laquelle il faut écrire, et
     * elle n'a plus rien à voir avec celle de la requête qui déclenche l'envoi.
     */

    /** F19 — une demande de place vient d'arriver sur un trajet. */
    @Async
    public void notifierDemandeRecue(User conducteur, String prenomPassager, String trajet, int places) {
        Locale l = conducteur.langue();
        envoyer(conducteur.getEmail(), l,
                messages.get(l, "courriel.demandeRecue.sujet"),
                notification(l,
                        messages.get(l, "courriel.demandeRecue.titre"),
                        messages.get(l, "courriel.bonjour", conducteur.getFirstname()),
                        messages.get(l, "courriel.demandeRecue.intro", prenomPassager, places),
                        trajet,
                        messages.get(l, "courriel.lien.demandes"),
                        siteUrl + "/dashboard?tab=requests"),
                "demande reçue");
    }

    /** F20 — le conducteur a accepté. */
    @Async
    public void notifierReservationAcceptee(User passager, String prenomConducteur, String trajet) {
        Locale l = passager.langue();
        envoyer(passager.getEmail(), l,
                messages.get(l, "courriel.acceptee.sujet"),
                notification(l,
                        messages.get(l, "courriel.acceptee.titre"),
                        messages.get(l, "courriel.bonjour", passager.getFirstname()),
                        messages.get(l, "courriel.acceptee.intro", prenomConducteur),
                        trajet,
                        messages.get(l, "courriel.lien.reservations"),
                        siteUrl + "/bookings"),
                "réservation acceptée");
    }

    /**
     * F20 — le conducteur a refusé.
     *
     * <p>Le motif est repris tel quel quand il existe. Refuser sans un mot
     * laisse le passager sans explication ; le taire dans le courriel
     * l'obligerait à revenir le chercher.</p>
     */
    @Async
    public void notifierReservationRefusee(User passager, String trajet, String motif) {
        Locale l = passager.langue();
        String details = (motif == null || motif.isBlank())
                ? trajet
                : trajet + "<br><br><em>" + echapperHtml(motif) + "</em>";

        envoyer(passager.getEmail(), l,
                messages.get(l, "courriel.refusee.sujet"),
                notification(l,
                        messages.get(l, "courriel.refusee.titre"),
                        messages.get(l, "courriel.bonjour", passager.getFirstname()),
                        messages.get(l, "courriel.refusee.intro"),
                        details,
                        messages.get(l, "courriel.lien.chercher"),
                        siteUrl + "/trips/search"),
                "réservation refusée");
    }

    /** F29 — le passager s'est désisté. */
    @Async
    public void notifierAnnulationParPassager(User conducteur, String prenomPassager, String trajet) {
        Locale l = conducteur.langue();
        envoyer(conducteur.getEmail(), l,
                messages.get(l, "courriel.annulPassager.sujet"),
                notification(l,
                        messages.get(l, "courriel.annulPassager.titre"),
                        messages.get(l, "courriel.bonjour", conducteur.getFirstname()),
                        messages.get(l, "courriel.annulPassager.intro", prenomPassager),
                        trajet,
                        messages.get(l, "courriel.lien.demandes"),
                        siteUrl + "/dashboard?tab=requests"),
                "annulation par le passager");
    }

    /**
     * F18 — le conducteur a annulé le trajet.
     *
     * <p>C'est la notification la plus importante des cinq : sans elle,
     * quelqu'un attend à un point de rendez-vous où personne ne viendra.</p>
     */
    @Async
    public void notifierTrajetAnnule(User passager, String trajet) {
        Locale l = passager.langue();
        envoyer(passager.getEmail(), l,
                messages.get(l, "courriel.trajetAnnule.sujet"),
                notification(l,
                        messages.get(l, "courriel.trajetAnnule.titre"),
                        messages.get(l, "courriel.bonjour", passager.getFirstname()),
                        messages.get(l, "courriel.trajetAnnule.intro"),
                        trajet,
                        messages.get(l, "courriel.lien.chercher"),
                        siteUrl + "/trips/search"),
                "trajet annulé");
    }

    /**
     * Bienvenue, une fois l'adresse confirmée.
     *
     * <h2>Pourquoi après la vérification et non à l'inscription</h2>
     *
     * <p>Le courriel d'inscription porte le code d'activation : y ajouter un
     * mot d'accueil noierait la seule chose que la personne doit y trouver.
     * Et tant que l'adresse n'est pas prouvée, rien ne dit qu'elle appartient
     * à qui l'a saisie — souhaiter la bienvenue à un inconnu serait au mieux
     * inutile, au pire une nuisance envoyée à sa place.</p>
     *
     * <p>C'est aussi le moment où le rattachement à une organisation vient
     * d'avoir lieu : le message peut donc nommer le cercle rejoint, ce qui est
     * l'information la plus utile de tout le courriel.</p>
     */
    @Async
    public void notifierBienvenue(User membre, String organisation) {
        Locale l = membre.langue();
        String details = (organisation == null)
                ? messages.get(l, "courriel.bienvenue.sansOrganisation")
                : messages.get(l, "courriel.bienvenue.organisation", echapperHtml(organisation));

        envoyer(membre.getEmail(), l,
                messages.get(l, "courriel.bienvenue.sujet"),
                notification(l,
                        messages.get(l, "courriel.bienvenue.titre"),
                        messages.get(l, "courriel.bonjour", membre.getFirstname()),
                        messages.get(l, "courriel.bienvenue.intro"),
                        details,
                        messages.get(l, "courriel.lien.chercher"),
                        siteUrl + "/trips/search"),
                "bienvenue");
    }

    /**
     * Reçu d'un paiement.
     *
     * <h2>Ce qu'un reçu doit contenir</h2>
     *
     * <p>Non pas « votre paiement a été accepté », mais <em>ce pour quoi</em> on
     * a payé : le trajet, la date, le nombre de places, le montant, et la
     * référence de l'opération. C'est ce qu'on cherche trois semaines plus tard
     * en relisant son relevé bancaire, et c'est la seule façon de rapprocher
     * une ligne de banque d'un déplacement.</p>
     *
     * <p>La référence du prestataire y figure pour la même raison : c'est le
     * seul identifiant commun entre ce courriel, la base de CoShift et le relevé
     * de la banque.</p>
     */
    @Async
    public void notifierPaiementRecu(User passager, String trajet, String montant,
                                     int places, String reference) {
        Locale l = passager.langue();
        String details = messages.get(l, "courriel.paiement.details",
                trajet, places, montant, echapperHtml(reference));

        envoyer(passager.getEmail(), l,
                messages.get(l, "courriel.paiement.sujet", montant),
                notification(l,
                        messages.get(l, "courriel.paiement.titre"),
                        messages.get(l, "courriel.bonjour", passager.getFirstname()),
                        messages.get(l, "courriel.paiement.intro"),
                        details,
                        messages.get(l, "courriel.lien.reservations"),
                        siteUrl + "/bookings"),
                "reçu de paiement");
    }

    /**
     * Neutralise le HTML d'un texte rédigé par un membre.
     *
     * <p>Le motif d'un refus est saisi librement par le conducteur et réinjecté
     * dans un document HTML envoyé à quelqu'un d'autre. Sans échappement, il y
     * ferait passer n'importe quelle balise.</p>
     */
    private String echapperHtml(String texte) {
        return texte.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
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

        } catch (Exception e) {
            /* Exception et non MessagingException. La capture ne visait que les
               exceptions VERIFIEES levees par les accesseurs du helper, alors
               que l'echec attendu — serveur injoignable, identifiants refuses —
               vient de mailSender.send(), qui leve MailSendException : une
               RuntimeException de Spring, qui traversait donc ce bloc.

               La consequence n'etait pas visible parce que ces methodes sont
               @Async : l'executeur avalait l'exception et journalisait une
               trace brute, a la place du message lisible prevu ici. Mais la
               garantie annoncee — « l'echec est silencieux pour l'appelant » —
               ne tenait que par cet effet de bord, et tombait des qu'un appel
               contournait le proxy. */
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
    /**
     * Corps des courriels portant un code à six chiffres.
     *
     * <p>Vérification d'adresse et réinitialisation : les deux affichent un
     * code bien visible, une durée de validité, et un avertissement à
     * l'intention de qui n'a rien demandé.</p>
     */
    private String gabarit(Locale langue, String code, String titre, String bonjour,
                           String intro, String validite, String avertissement) {
        String corps = """
                <p>%s</p>
                <div class="code-box">
                  <div class="code">%s</div>
                  <div class="expiry">%s</div>
                </div>
                <p class="warn">%s</p>
                """.formatted(intro, code, validite, avertissement);

        return enveloppe(langue, titre, bonjour, corps);
    }

    /**
     * Corps des notifications.
     *
     * <p>Pas de code, mais un rappel du trajet concerné et un lien qui ramène
     * là où l'action se poursuit. Une notification qui ne dit pas quoi faire
     * ensuite oblige à retrouver l'écran soi-même, ce qui revient à ne pas
     * l'avoir envoyée.</p>
     *
     * @param details   rappel du trajet, déjà mis en forme
     * @param lienTexte libellé du bouton
     * @param lienUrl   destination du bouton
     */
    private String notification(Locale langue, String titre, String bonjour, String intro,
                                String details, String lienTexte, String lienUrl) {
        String corps = """
                <p>%s</p>
                <div class="detail-box">%s</div>
                <p style="text-align:center; margin: 28px 0;">
                  <a class="cta" href="%s">%s</a>
                </p>
                """.formatted(intro, details, lienUrl, lienTexte);

        return enveloppe(langue, titre, bonjour, corps);
    }

    /**
     * Enveloppe commune à tous les courriels.
     *
     * <p>L'attribut {@code lang} du document suit la langue du message : les
     * lecteurs de courrier qui proposent une traduction automatique s'y fient,
     * et une synthèse vocale lit un texte anglais avec l'accent français si on
     * lui annonce du français.</p>
     *
     * <p>Le style reste en ligne : les clients de messagerie ignorent
     * largement les feuilles de style externes, et beaucoup rognent même les
     * blocs {@code <style>}.</p>
     */
    private String enveloppe(Locale langue, String titre, String bonjour, String corps) {
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
                .detail-box { background: rgba(255,255,255,0.04); border-left: 3px solid #60a5fa;
                              border-radius: 8px; padding: 16px 20px; margin: 24px 0;
                              color: #cbd5e1; line-height: 1.7; }
                .cta { display: inline-block; background: #60a5fa; color: #0f172a;
                       text-decoration: none; font-weight: 700; padding: 14px 28px;
                       border-radius: 10px; }
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
                %s
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
                corps,
                messages.get(langue, "courriel.pied"),
                messages.get(langue, "courriel.piedAuto"));
    }
}
