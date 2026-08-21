import { useState, useEffect } from "react";
import { Link, useParams } from "react-router-dom";
import { FiArrowLeft, FiExternalLink } from "react-icons/fi";
import axios from "axios";
import { API_BASE } from "../../config/api";
import { Alert, Button, Card, Spinner } from "../../components/ui";
import { libelleCategorie, formatArticleDate, lienArticle, type Article } from "./ActusPage";
import { useSeo, useDonneesStructurees } from "../../hooks/useSeo";
import { useLang } from "../../context/LangContext";
import { LANGUES } from "../../i18n";
import "./ArticlePage.css";

/**
 * Page article : le détail d'une actualité du flux mobilité.
 *
 * L'article n'est qu'un résumé agrégé, jamais le texte intégral : reproduire
 * un article de presse en entier serait une contrefaçon. Le lien vers la
 * source d'origine est donc l'élément central de la page.
 */
export default function ArticlePage() {
  const { id: parametre } = useParams<{ id: string }>();

  /* L'adresse porte désormais un fragment lisible devant l'identifiant :
     /actus/pourquoi-le-covoiturage-progresse--288241c1-…
     Seul ce qui suit le dernier « -- » est lu. Les anciennes adresses, qui ne
     contenaient que l'identifiant, continuent donc de fonctionner. */
  const id = parametre?.includes("--")
    ? parametre.slice(parametre.lastIndexOf("--") + 2)
    : parametre;

  const [article, setArticle] = useState<Article | null>(null);
  const { langue, t } = useLang();
  const [related, setRelated] = useState<Article[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useSeo({
    titre: article ? article.title : t("article.titreDefaut"),
    description: article
      ? article.summary.slice(0, 300)
      : t("article.descriptionDefaut"),
    chemin: article ? lienArticle(article) : undefined,
    image: article?.imageUrl,
    type: "article",
  });

  /* Décrit l'article à un moteur : titre, date, image et — surtout —
     l'éditeur d'origine. CoShift agrège des résumés, il ne les écrit pas ;
     s'en attribuer la paternité serait faux autant qu'inutile. */
  useDonneesStructurees(
    article
      ? {
          "@context": "https://schema.org",
          "@type": "NewsArticle",
          headline: article.title,
          description: article.summary,
          datePublished: article.date,
          image: article.imageUrl ? [article.imageUrl] : undefined,
          articleSection: article.category,
          publisher: { "@type": "Organization", name: article.source },
          isBasedOn: article.url,
        }
      : null,
  );

  useEffect(() => {
    setLoading(true);
    axios
      .get<Article>(`${API_BASE}/api/pwa/articles/${encodeURIComponent(id ?? "")}`)
      .then((r) => {
        setArticle(r.data);
        // Suggestions de la même rubrique, chargées après l'article lui-même.
        return axios.get<Article[]>(`${API_BASE}/api/pwa/articles`).then((all) =>
          setRelated(
            all.data.filter((a) => a.category === r.data.category && a.id !== r.data.id).slice(0, 3),
          ),
        );
      })
      .catch(() => setError(t("article.introuvable")))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return (
      <div className="container page">
        <Spinner size="lg" center showLabel label={t("article.chargement")} />
      </div>
    );
  }

  if (!article) {
    return (
      <div className="container page stack-6">
        <Alert tone="danger">{error ?? t("article.introuvable")}</Alert>
        <Button variant="secondary" icon={<FiArrowLeft />} to="/actus">
          {t("article.retourActus")}
        </Button>
      </div>
    );
  }

  const categoryLabel = libelleCategorie(t, article.category);

  return (
    <div className="container page stack-8">
      <Button variant="ghost" size="sm" icon={<FiArrowLeft />} to="/actus">
        {t("article.toutesActus")}
      </Button>

      {/* Colonne de lecture étroite : au-delà de ~70 caractères par ligne,
          l'œil peine à retrouver le début de la ligne suivante. */}
      <article className="container container--prose container--flush ar">
        <p className="ar__meta">
          <Link to="/actus" className="ar__tag">{categoryLabel}</Link>
          <time dateTime={article.date}>
            {formatArticleDate(article.date, LANGUES[langue].balise)}
          </time>
        </p>

        <h1 className="ar__title">{article.title}</h1>

        <p className="ar__source">
          {t("article.source")} <strong>{article.source}</strong>
        </p>

        {article.imageUrl && (
          <img
            className="ar__image"
            src={article.imageUrl}
            alt=""
            loading="lazy"
            decoding="async"
          />
        )}

        <div className="ar__body">
          <p>{article.summary}</p>
        </div>

        <Card className="ar__cta" padding="lg">
          <p className="ar__cta-text">{t("article.avertissement")}</p>
          <Button
            variant="secondary"
            icon={<FiExternalLink />}
            onClick={() => window.open(article.url, "_blank", "noopener,noreferrer")}
          >
            {t("article.lireSur", { source: article.source })}
          </Button>
        </Card>
      </article>

      {related.length > 0 && (
        <section className="stack-6">
          <h2 className="ar__related-title">{t("article.memeRubrique")}</h2>
          <div className="grid-auto">
            {related.map((a) => (
              <Card key={a.id} to={lienArticle(a)}>
                <p className="ar__related-date">
                  <time dateTime={a.date}>
                    {formatArticleDate(a.date, LANGUES[langue].balise)}
                  </time>
                </p>
                <h3 className="ar__related-headline">{a.title}</h3>
              </Card>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
