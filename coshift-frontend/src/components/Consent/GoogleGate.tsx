import type { ReactNode } from "react";
import { GoogleOAuthProvider } from "@react-oauth/google";
import { Link } from "react-router-dom";
import { useConsent } from "../../context/ConsentContext";
import { useT } from "../../context/LangContext";
import "./GoogleGate.css";

/**
 * Identifiant public de l'application auprès de Google.
 *
 * <p>Ce n'est pas un secret : il circule dans toute requête d'autorisation et
 * figure nécessairement dans le code servi au navigateur. Ce qui protège
 * l'application est la liste des origines autorisées déclarée côté Google, pas
 * la confidentialité de cette chaîne.</p>
 */
const GOOGLE_CLIENT_ID =
  "415112384949-i0jihhuatgp8hrnuvptqujenhmmn0kb1.apps.googleusercontent.com";

/**
 * Charge Google Identity Services, et seulement s'il a été autorisé.
 *
 * <h2>Pourquoi une barrière plutôt qu'un réglage</h2>
 *
 * `GoogleOAuthProvider` injecte le script de Google dès son montage. Le rendre
 * conditionnel à un état interne ne suffirait donc pas : le script serait déjà
 * parti. La seule façon de tenir la promesse « rien n'est chargé avant votre
 * réponse » est de ne pas monter le fournisseur du tout.
 *
 * <p>C'est aussi ce qui rend la promesse vérifiable de l'extérieur : sur une
 * visite sans consentement, l'onglet réseau ne montre aucune requête vers
 * <code>accounts.google.com</code>.</p>
 *
 * <h2>Le repli n'est pas une impasse</h2>
 *
 * Refuser Google ne doit pas empêcher de se connecter, sans quoi le
 * consentement serait la contrepartie d'un service — ce que l'article 7.4 du
 * RGPD invite précisément à écarter. Le repli explique la situation et laisse
 * le formulaire classique, entièrement fonctionnel, juste en dessous.
 */
export default function GoogleGate({ children }: { children: ReactNode }) {
  const { autorise, aRepondu, reinitialiser } = useConsent();
  const t = useT();

  if (autorise("google")) {
    return <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>{children}</GoogleOAuthProvider>;
  }

  return (
    <div className="gate" role="note">
      <p className="gate__texte">
        {aRepondu
          ? t("consentement.googleDesactive")
          : t("consentement.googleNonCharge")}
      </p>
      <p className="gate__texte gate__texte--sourdine">
        {t("consentement.googleRepli")}{" "}
        <button type="button" className="gate__lien is-inline" onClick={reinitialiser}>
          {t("consentement.revoirChoix")}
        </button>{" "}
        · <Link to="/cookies">{t("consentement.enSavoirPlus")}</Link>
      </p>
    </div>
  );
}
