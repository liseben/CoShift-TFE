import { Link, Navigate, useParams } from "react-router-dom";
import { FiArrowLeft, FiClock } from "react-icons/fi";
import { BILLETS, billetParSlug } from "../../config/blog";
import { useLang } from "../../context/LangContext";
import { LANGUES } from "../../i18n";
import { useSeo } from "../../hooks/useSeo";
import "./BlogPage.css";

/** Un billet du blog. */
export default function BlogPostPage() {
  const { slug } = useParams();
  const { langue, t } = useLang();
  const balise = LANGUES[langue].balise;

  const billet = billetParSlug(slug);

  /* Un fragment inconnu renvoie à la liste plutôt que d'afficher une page vide.
     `replace` évite de piéger le bouton Précédent sur une adresse morte. */
  const seo = billet
    ? {
        titre: t(`blog.${billet.slug}.titre`),
        description: t(`blog.${billet.slug}.chapeau`),
        chemin: `/blog/${billet.slug}`,
        type: "article" as const,
      }
    : { titre: t("blog.titre"), description: t("blog.description"), chemin: "/blog" };

  // Le crochet doit être appelé à chaque rendu, y compris quand le billet est
  // introuvable : on le renseigne au-dessus plutôt que de sortir avant.
  useSeo(seo);

  if (!billet) return <Navigate to="/blog" replace />;

  const autres = BILLETS.filter((b) => b.slug !== billet.slug).slice(0, 2);

  return (
    <div className="container container--prose page stack-8">
      <Link className="blog__retour" to="/blog">
        <FiArrowLeft aria-hidden="true" />
        {t("blog.retour")}
      </Link>

      <article className="blog__billet">
        <header className="blog__billet-tete">
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

          <h1 className="blog__billet-titre">{t(`blog.${billet.slug}.titre`)}</h1>
          <p className="blog__billet-chapeau">{t(`blog.${billet.slug}.chapeau`)}</p>
        </header>

        <div className="blog__billet-corps">
          {/* Le nombre de paragraphes vient de l'index : ajouter un paragraphe
              au catalogue français sans l'ajouter à l'anglais devient une
              erreur de compilation. */}
          {Array.from({ length: billet.paragraphes }, (_, i) => (
            <p key={i}>{t(`blog.${billet.slug}.p${i + 1}`)}</p>
          ))}
        </div>
      </article>

      {autres.length > 0 && (
        <aside className="blog__suite">
          <h2 className="blog__suite-titre">{t("blog.aLire")}</h2>
          <ul className="blog__suite-liste">
            {autres.map((b) => (
              <li key={b.slug}>
                <Link to={`/blog/${b.slug}`}>{t(`blog.${b.slug}.titre`)}</Link>
              </li>
            ))}
          </ul>
        </aside>
      )}
    </div>
  );
}
