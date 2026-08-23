import { Link } from "react-router-dom";
import { FiArrowRight, FiClock } from "react-icons/fi";
import { BILLETS } from "../../config/blog";
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
 */
export default function BlogPage() {
  const { langue, t } = useLang();
  const balise = LANGUES[langue].balise;

  useSeo({
    titre: t("blog.titre"),
    description: t("blog.description"),
    chemin: "/blog",
  });

  return (
    <div className="container container--wide page stack-8">
      <header className="blog__header">
        <h1>{t("blog.heroTitre")}</h1>
        <p className="blog__lead">{t("blog.heroAccroche")}</p>
        <p className="blog__distinction">{t("blog.distinction")}</p>
      </header>

      <div className="blog__liste">
        {BILLETS.map((billet) => (
          <article className="blog__carte" key={billet.slug}>
            <div className="blog__meta">
              <span className={`blog__rubrique blog__rubrique--${billet.rubrique}`}>
                {t(`blog.rubrique.${billet.rubrique}`)}
              </span>
              <time dateTime={billet.date}>
                {new Date(billet.date).toLocaleDateString(balise, {
                  day: "numeric",
                  month: "long",
                  year: "numeric",
                })}
              </time>
              <span className="blog__lecture">
                <FiClock aria-hidden="true" />
                {t("blog.minutes", { n: billet.lecture })}
              </span>
            </div>

            <h2 className="blog__carte-titre">
              {/* Le lien porte le titre entier : un « Lire la suite » isolé ne
                  dit pas où il mène à qui parcourt la page au lecteur d'écran. */}
              <Link to={`/blog/${billet.slug}`}>{t(`blog.${billet.slug}.titre`)}</Link>
            </h2>

            <p className="blog__carte-chapeau">{t(`blog.${billet.slug}.chapeau`)}</p>

            <Link className="blog__lire" to={`/blog/${billet.slug}`} tabIndex={-1}>
              {t("blog.lire")}
              <FiArrowRight aria-hidden="true" />
            </Link>
          </article>
        ))}
      </div>
    </div>
  );
}
