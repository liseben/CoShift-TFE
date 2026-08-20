import { Link } from "react-router-dom";
import Logo from "../Logo/Logo";
import { useConsent } from "../../context/ConsentContext";
import { useT } from "../../context/LangContext";
import { API_BASE } from "../../config/api";
import { EDITEUR } from "../../config/legal";
import "./SiteFooter.css";

/**
 * Pied de page du site.
 *
 * <h2>Ce qu'un pied de page doit porter</h2>
 *
 * L'article XII.6 du Code de droit économique impose que l'identification du
 * prestataire soit accessible « de manière facile, directe et permanente ». Le
 * pied de page est le seul emplacement qui satisfasse les trois adverbes à la
 * fois : présent sur toutes les pages, atteignable en un clic, au même endroit
 * partout.
 *
 * <p>Avant cette version, il ne contenait qu'une ligne de copyright. Les quatre
 * documents légaux existaient d'autant moins qu'aucun chemin n'y menait.</p>
 *
 * <h2>Le retrait du consentement y figure aussi</h2>
 *
 * L'article 7.3 du RGPD exige qu'il soit aussi simple de retirer son
 * consentement que de le donner. Le donner coûte un clic dans un bandeau ; le
 * retirer coûte donc un clic dans le pied de page, sans passer par une page
 * intermédiaire.
 */
export default function SiteFooter() {
  const { reinitialiser } = useConsent();
  const t = useT();
  const annee = new Date().getFullYear();

  return (
    <footer className="sf">
      <div className="sf__haut container container--wide">
        <div className="sf__marque">
          <Logo size={32} />
          <p className="sf__baseline">
            {t("pied.baseline")}
          </p>
        </div>

        <nav className="sf__col" aria-labelledby="sf-service">
          <h2 className="sf__titre" id="sf-service">{t("pied.service")}</h2>
          <ul>
            <li><Link to="/trips/search">{t("pied.chercherTrajet")}</Link></li>
            <li><Link to="/trips/create">{t("pied.proposerTrajet")}</Link></li>
            <li><Link to="/entreprises">{t("pied.espaceEntreprises")}</Link></li>
            <li><Link to="/a-propos">{t("nav.apropos")}</Link></li>
          </ul>
        </nav>

        <nav className="sf__col" aria-labelledby="sf-ressources">
          <h2 className="sf__titre" id="sf-ressources">{t("pied.ressources")}</h2>
          <ul>
            <li><Link to="/actus">{t("pied.actualites")}</Link></li>
            <li>
              <a href={`${API_BASE}/api/open-data`} target="_blank" rel="noopener noreferrer">
                {t("pied.donneesOuvertes")}
              </a>
            </li>
            <li>
              <a href={`${API_BASE}/swagger-ui.html`} target="_blank" rel="noopener noreferrer">
                {t("pied.documentationApi")}
              </a>
            </li>
            <li><Link to="/styleguide">{t("pied.charteGraphique")}</Link></li>
          </ul>
        </nav>

        <nav className="sf__col" aria-labelledby="sf-legal">
          <h2 className="sf__titre" id="sf-legal">{t("pied.legal")}</h2>
          <ul>
            <li><Link to="/mentions-legales">{t("pied.mentions")}</Link></li>
            <li><Link to="/confidentialite">{t("pied.confidentialite")}</Link></li>
            <li><Link to="/cgu">{t("pied.cgu")}</Link></li>
            <li><Link to="/cookies">{t("pied.cookies")}</Link></li>
            <li>
              <button type="button" className="sf__bouton is-inline" onClick={reinitialiser}>
                {t("pied.revoirChoix")}
              </button>
            </li>
            <li>
              <a href={`mailto:${EDITEUR.signalement}`}>{t("pied.signaler")}</a>
            </li>
          </ul>
        </nav>
      </div>

      <div className="sf__bas container container--wide">
        <p className="sf__copyright">
          © {annee} {EDITEUR.denomination} · {EDITEUR.siege} · BCE {EDITEUR.bce}
        </p>
        <p className="sf__note">
          {t("pied.projetAcademique")}{" "}
          <Link to="/mentions-legales">{t("pied.projetAcademiqueLien")}</Link>.
        </p>
      </div>
    </footer>
  );
}
