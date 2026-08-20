import { Link } from "react-router-dom";
import Logo from "../Logo/Logo";
import { useConsent } from "../../context/ConsentContext";
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
  const annee = new Date().getFullYear();

  return (
    <footer className="sf">
      <div className="sf__haut container container--wide">
        <div className="sf__marque">
          <Logo size={32} />
          <p className="sf__baseline">
            Le covoiturage qui commence à la porte de votre organisation.
          </p>
        </div>

        <nav className="sf__col" aria-labelledby="sf-service">
          <h2 className="sf__titre" id="sf-service">Le service</h2>
          <ul>
            <li><Link to="/trips/search">Rechercher un trajet</Link></li>
            <li><Link to="/trips/create">Proposer un trajet</Link></li>
            <li><Link to="/entreprises">Espace entreprises</Link></li>
            <li><Link to="/a-propos">À propos</Link></li>
          </ul>
        </nav>

        <nav className="sf__col" aria-labelledby="sf-ressources">
          <h2 className="sf__titre" id="sf-ressources">Ressources</h2>
          <ul>
            <li><Link to="/actus">Actualités mobilité</Link></li>
            <li>
              <a href={`${API_BASE}/api/open-data`} target="_blank" rel="noopener noreferrer">
                Données ouvertes
              </a>
            </li>
            <li>
              <a href={`${API_BASE}/swagger-ui.html`} target="_blank" rel="noopener noreferrer">
                Documentation de l'API
              </a>
            </li>
            <li><Link to="/styleguide">Charte graphique</Link></li>
          </ul>
        </nav>

        <nav className="sf__col" aria-labelledby="sf-legal">
          <h2 className="sf__titre" id="sf-legal">Informations légales</h2>
          <ul>
            <li><Link to="/mentions-legales">Mentions légales</Link></li>
            <li><Link to="/confidentialite">Politique de confidentialité</Link></li>
            <li><Link to="/cgu">Conditions générales</Link></li>
            <li><Link to="/cookies">Cookies et traceurs</Link></li>
            <li>
              <button type="button" className="sf__bouton is-inline" onClick={reinitialiser}>
                Revoir mon choix de traceurs
              </button>
            </li>
            <li>
              <a href={`mailto:${EDITEUR.signalement}`}>Signaler un contenu</a>
            </li>
          </ul>
        </nav>
      </div>

      <div className="sf__bas container container--wide">
        <p className="sf__copyright">
          © {annee} {EDITEUR.denomination} · {EDITEUR.siege} · BCE {EDITEUR.bce}
        </p>
        <p className="sf__note">
          Projet de fin d'études — données d'identification fictives, détaillées
          dans les <Link to="/mentions-legales">mentions légales</Link>.
        </p>
      </div>
    </footer>
  );
}
