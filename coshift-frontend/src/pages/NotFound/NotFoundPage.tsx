import { Link, useLocation, useNavigate } from "react-router-dom";
import { FiArrowLeft, FiHome, FiSearch, FiBriefcase, FiFileText, FiInfo } from "react-icons/fi";
import { Button } from "../../components/ui";
import { useLang } from "../../context/LangContext";
import { useSeo } from "../../hooks/useSeo";
import "./NotFoundPage.css";

/** Au-delà, l'adresse est repliée par le CSS ; on la coupe pour ne pas
 *  transformer un écran d'erreur en mur de texte collé par un robot. */
const LONGUEUR_MAX = 120;

/**
 * Écran affiché pour une adresse inconnue.
 *
 * <h2>Ce n'est pas un vrai 404</h2>
 *
 * <p>L'application est une page unique servie en statique : le serveur renvoie
 * `index.html` avec un code <strong>200</strong> pour n'importe quel chemin,
 * puisqu'il ne connaît pas la table des routes — elle vit dans le JavaScript.
 * Ce composant est donc un « 404 mou » : le message est juste, le code HTTP ne
 * l'est pas. La conséquence pratique est qu'un moteur de recherche indexerait
 * volontiers ces pages ; c'est `horsIndex` qui l'en empêche, et c'est la seule
 * chose qui tienne lieu de statut ici.</p>
 *
 * <h2>Pourquoi l'adresse demandée est affichée</h2>
 *
 * <p>Neuf fois sur dix l'erreur vient d'une adresse tronquée par un client de
 * messagerie ou d'une faute de frappe, et la voir suffit à comprendre. Elle est
 * rendue dans un `code`, jamais dans une phrase : React échappe le texte, donc
 * rien ne s'exécute, mais une adresse fabriquée reste un texte que l'auteur du
 * lien contrôle. La présenter comme une donnée citée plutôt que comme un propos
 * de CoShift évite qu'on lui prête notre voix.</p>
 */
export default function NotFoundPage() {
  const { t } = useLang();
  const location = useLocation();
  const navigate = useNavigate();

  useSeo({
    titre: t("introuvable.titre"),
    description: t("introuvable.description"),
    horsIndex: true,
  });

  const demandee = location.pathname + location.search;
  const affichee =
    demandee.length > LONGUEUR_MAX ? demandee.slice(0, LONGUEUR_MAX) + "…" : demandee;

  /* `key` vaut "default" sur la première entrée de l'historique du routeur :
     la page a été ouverte directement, par une adresse tapée ou un lien
     extérieur. Reculer renverrait alors hors du site, voire nulle part. Le
     bouton n'apparaît que s'il y a réellement une page où revenir. */
  const peutReculer = location.key !== "default";

  return (
    <div className="container page stack-8 introuvable">
      {/* Un `div`, pas un `header` : Chrome expose tout `header` en repère
          « banner », même imbriqué dans `main`. La page en aurait alors deux, et
          un lecteur d'écran qui saute de repère en repère atterrirait sur celui
          qui n'est pas l'en-tête du site. Le reste du projet emploie encore
          `header` dans cette position — c'est le même défaut, à traiter à part. */}
      <div className="introuvable__tete">
        <p className="introuvable__code" aria-hidden="true">
          404
        </p>
        <h1>{t("introuvable.heroTitre")}</h1>
        <p className="introuvable__accroche">{t("introuvable.accroche")}</p>

        <p className="introuvable__adresse">
          <span className="introuvable__adresse-libelle">{t("introuvable.adresse")}</span>
          <code>{affichee}</code>
        </p>
      </div>

      <div className="introuvable__actions">
        {/* Un lien pour l'accueil, un bouton pour reculer : le premier a une
            adresse et s'ouvre dans un nouvel onglet, le second agit sur
            l'historique et n'en a pas. Le composant respecte la distinction. */}
        <Button to="/" icon={<FiHome />}>
          {t("introuvable.accueil")}
        </Button>

        {peutReculer && (
          <Button variant="secondary" icon={<FiArrowLeft />} onClick={() => navigate(-1)}>
            {t("introuvable.precedent")}
          </Button>
        )}
      </div>

      <nav className="introuvable__pistes" aria-labelledby="introuvable-pistes">
        <h2 id="introuvable-pistes" className="introuvable__pistes-titre">
          {t("introuvable.pistes")}
        </h2>
        <ul>
          <li>
            <Link to="/trips/search">
              <FiSearch aria-hidden="true" />
              {t("pied.chercherTrajet")}
            </Link>
          </li>
          <li>
            <Link to="/entreprises">
              <FiBriefcase aria-hidden="true" />
              {t("pied.espaceEntreprises")}
            </Link>
          </li>
          <li>
            <Link to="/blog">
              <FiFileText aria-hidden="true" />
              {t("nav.blog")}
            </Link>
          </li>
          <li>
            <Link to="/a-propos">
              <FiInfo aria-hidden="true" />
              {t("nav.apropos")}
            </Link>
          </li>
        </ul>
      </nav>
    </div>
  );
}
