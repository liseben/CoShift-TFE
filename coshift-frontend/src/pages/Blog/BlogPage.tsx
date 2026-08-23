import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { FiArrowRight, FiClock } from "react-icons/fi";
import { chargerBillets, cleRubrique, type Billet } from "../../config/blogApi";
import { Alert, Spinner } from "../../components/ui";
import { useLang } from "../../context/LangContext";
import { LANGUES } from "../../i18n";
import { useSeo } from "../../hooks/useSeo";
import "./BlogPage.css";

/**
 * Le blog de CoShift.
 *
 * <p>À ne pas confondre avec la rubrique Actus, qui agrège une revue de presse
 * extérieure. Ici, ce sont les textes de l'équipe : les choix de conception, ce
 * qui est fait des données, pourquoi telle décision plutôt que telle autre.
 * La distinction est celle qu'annonce la navigation depuis le début, et cette
 * page est ce qui la rend vraie.</p>
 *
 * <p>Les billets viennent désormais de l'API et non plus du catalogue de
 * traduction : ils se rédigent depuis l'administration, sans redéploiement. Le
 * rendu, lui, n'a pas changé — il recevait déjà un titre, un chapeau et une
 * suite de paragraphes.</p>
 */
export default function BlogPage() {
  const { langue, t } = useLang();
  const balise = LANGUES[langue].balise;

  useSeo({
    titre: t("blog.titre"),
    description: t("blog.description"),
    chemin: "/blog",
  });

  const [billets, setBillets] = useState<Billet[]>([]);
  const [chargement, setChargement] = useState(true);
  const [erreur, setErreur] = useState<string | null>(null);

  useEffect(() => {
    chargerBillets()
      .then(setBillets)
      .catch(() => setErreur(t("commun.erreurReseau")))
      .finally(() => setChargement(false));
    /* La langue est relue : l'en-tête Accept-Language change avec elle, et un
       billet peut n'exister que dans une des deux. */
  }, [langue]);

  return (
    <div className="container container--wide page stack-8">
      <header className="blog__header">
        <h1>{t("blog.heroTitre")}</h1>
        <p className="blog__lead">{t("blog.heroAccroche")}</p>
        <p className="blog__distinction">{t("blog.distinction")}</p>
      </header>

      {erreur && <Alert tone="danger">{erreur}</Alert>}
      {chargement && <Spinner center label={t("commun.chargementEnCours")} />}

      {!chargement && !erreur && (
        <div className="blog__liste">
          {billets.map((billet) => (
            <article className="blog__carte" key={billet.uuid}>
              <div className="blog__meta">
                <span className={`blog__rubrique blog__rubrique--${cleRubrique(billet.category)}`}>
                  {t(`blog.rubrique.${cleRubrique(billet.category)}`)}
                </span>
                {billet.publishedAt && (
                  <time dateTime={billet.publishedAt}>
                    {new Date(billet.publishedAt).toLocaleDateString(balise, {
                      day: "numeric",
                      month: "long",
                      year: "numeric",
                    })}
                  </time>
                )}
                <span className="blog__lecture">
                  <FiClock aria-hidden="true" />
                  {t("blog.minutes", { n: billet.readingMinutes })}
                </span>
              </div>

              <h2 className="blog__carte-titre">
                {/* Le lien porte le titre entier : un « Lire la suite » isolé ne
                    dit pas où il mène à qui parcourt la page au lecteur d'écran. */}
                <Link to={`/blog/${billet.slug}`}>{billet.title}</Link>
              </h2>

              <p className="blog__carte-chapeau">{billet.lead}</p>

              <Link className="blog__lire" to={`/blog/${billet.slug}`} tabIndex={-1}>
                {t("blog.lire")}
                <FiArrowRight aria-hidden="true" />
              </Link>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
