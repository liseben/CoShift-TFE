import { useState, useEffect } from "react";
import { Link, useParams } from "react-router-dom";
import { FiArrowLeft, FiExternalLink } from "react-icons/fi";
import axios from "axios";
import { API_BASE } from "../../config/api";
import { Alert, Button, Card, Spinner } from "../../components/ui";
import { CATEGORIES, formatArticleDate, type Article } from "./ActusPage";
import "./ArticlePage.css";

/**
 * Page article : le détail d'une actualité du flux mobilité.
 *
 * L'article n'est qu'un résumé agrégé, jamais le texte intégral : reproduire
 * un article de presse en entier serait une contrefaçon. Le lien vers la
 * source d'origine est donc l'élément central de la page.
 */
export default function ArticlePage() {
  const { id } = useParams<{ id: string }>();
  const [article, setArticle] = useState<Article | null>(null);
  const [related, setRelated] = useState<Article[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

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
      .catch(() => setError("Cet article est introuvable."))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return (
      <div className="container page">
        <Spinner size="lg" center showLabel label="Chargement de l'article" />
      </div>
    );
  }

  if (!article) {
    return (
      <div className="container page stack-6">
        <Alert tone="danger">{error ?? "Cet article est introuvable."}</Alert>
        <Button variant="secondary" icon={<FiArrowLeft />} to="/actus">
          Retour aux actualités
        </Button>
      </div>
    );
  }

  const categoryLabel =
    CATEGORIES.find((c) => c.id === article.category)?.label ?? article.category;

  return (
    <div className="container page stack-8">
      <Button variant="ghost" size="sm" icon={<FiArrowLeft />} to="/actus">
        Toutes les actualités
      </Button>

      {/* Colonne de lecture étroite : au-delà de ~70 caractères par ligne,
          l'œil peine à retrouver le début de la ligne suivante. */}
      <article className="container container--prose container--flush ar">
        <p className="ar__meta">
          <Link to="/actus" className="ar__tag">{categoryLabel}</Link>
          <time dateTime={article.date}>{formatArticleDate(article.date)}</time>
        </p>

        <h1 className="ar__title">{article.title}</h1>

        <p className="ar__source">
          Source : <strong>{article.source}</strong>
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
          <p className="ar__cta-text">
            CoShift agrège et résume l'actualité mobilité. L'article complet
            reste chez son éditeur.
          </p>
          <Button
            variant="secondary"
            icon={<FiExternalLink />}
            onClick={() => window.open(article.url, "_blank", "noopener,noreferrer")}
          >
            Lire l'article sur {article.source}
          </Button>
        </Card>
      </article>

      {related.length > 0 && (
        <section className="stack-6">
          <h2 className="ar__related-title">Dans la même rubrique</h2>
          <div className="grid-auto">
            {related.map((a) => (
              <Card key={a.id} to={`/actus/${encodeURIComponent(a.id)}`}>
                <p className="ar__related-date">
                  <time dateTime={a.date}>{formatArticleDate(a.date)}</time>
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
