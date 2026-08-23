import { useEffect, useState } from "react";
import { Link, Navigate, useParams } from "react-router-dom";
import { FiArrowLeft, FiClock } from "react-icons/fi";
import { chargerBillet, chargerBillets, cleRubrique, type Billet } from "../../config/blogApi";
import { Spinner } from "../../components/ui";
import { useLang } from "../../context/LangContext";
import { LANGUES } from "../../i18n";
import { useSeo } from "../../hooks/useSeo";
import "./BlogPage.css";

/** Un billet du blog. */
export default function BlogPostPage() {
  const { slug } = useParams();
  const { langue, t } = useLang();
  const balise = LANGUES[langue].balise;

  /* Un seul état, qui porte le fragment auquel il correspond.

     Deux états séparés — « le billet » et « en chargement » — obligeraient à
     rappeler `setChargement(true)` dans le corps de l'effet à chaque
     changement d'adresse, ce qui déclenche une cascade de rendus et que
     l'analyse statique refuse à juste titre. En rangeant le fragment avec le
     résultat, l'attente se déduit : tant que le résultat ne correspond pas au
     fragment demandé, c'est qu'on l'attend encore. */
  const [resultat, setResultat] = useState<{ slug: string; billet: Billet | null }>({
    slug: "",
    billet: null,
  });
  const [autres, setAutres] = useState<Billet[]>([]);

  useEffect(() => {
    if (!slug) return;
    let vivant = true;
    chargerBillet(slug)
      .then((b) => {
        if (!vivant) return;
        setResultat({ slug, billet: b });
        return chargerBillets().then((tous) => {
          if (vivant) setAutres(tous.filter((x) => x.slug !== b.slug).slice(0, 2));
        });
      })
      /* Un fragment inconnu, ou un brouillon, renvoie à la liste plutôt que
         d'afficher une page vide. Le serveur répond 404 dans les deux cas :
         pour le public, un billet non publié n'existe pas. */
      .catch(() => {
        if (vivant) setResultat({ slug, billet: null });
      });
    /* Deux billets ouverts coup sur coup : la première réponse ne doit pas
       écraser la seconde si elle arrive en retard. */
    return () => {
      vivant = false;
    };
  }, [slug, langue]);

  const billet = resultat.slug === slug ? resultat.billet : null;
  const introuvable = resultat.slug === slug && resultat.billet === null;
  const chargement = resultat.slug !== slug;

  // Le crochet doit être appelé à chaque rendu, y compris avant que le billet
  // soit chargé : on le renseigne au-dessus plutôt que de sortir avant.
  useSeo(
    billet
      ? {
          titre: billet.title ?? t("blog.titre"),
          description: billet.lead ?? t("blog.description"),
          chemin: `/blog/${billet.slug}`,
          type: "article" as const,
        }
      : { titre: t("blog.titre"), description: t("blog.description"), chemin: "/blog" },
  );

  /* `replace` évite de piéger le bouton Précédent sur une adresse morte. */
  if (introuvable) return <Navigate to="/blog" replace />;
  if (chargement || !billet) return <Spinner center label={t("commun.chargementEnCours")} />;

  return (
    <div className="container container--prose page stack-8">
      <Link className="blog__retour" to="/blog">
        <FiArrowLeft aria-hidden="true" />
        {t("blog.retour")}
      </Link>

      <article className="blog__billet">
        <header className="blog__billet-tete">
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
            {billet.auteur && <span className="blog__auteur">{billet.auteur}</span>}
          </div>

          <h1 className="blog__billet-titre">{billet.title}</h1>
          <p className="blog__billet-chapeau">{billet.lead}</p>

          {/* Le billet n'existe pas dans la langue choisie : on le dit, plutôt
              que de laisser croire à une traduction. */}
          {billet.locale && billet.locale !== langue && (
            <p className="blog__langue" lang={billet.locale}>
              {t("blog.langueAutre")}
            </p>
          )}
        </header>

        <div className="blog__billet-corps">
          {billet.paragraphes.map((p, i) => (
            <p key={i}>{p}</p>
          ))}
        </div>
      </article>

      {autres.length > 0 && (
        <aside className="blog__suite">
          <h2 className="blog__suite-titre">{t("blog.aLire")}</h2>
          <ul className="blog__suite-liste">
            {autres.map((b) => (
              <li key={b.uuid}>
                <Link to={`/blog/${b.slug}`}>{b.title}</Link>
              </li>
            ))}
          </ul>
        </aside>
      )}
    </div>
  );
}
