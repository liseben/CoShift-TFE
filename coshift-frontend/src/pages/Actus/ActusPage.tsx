import { useState, useEffect, useMemo } from "react";
import { Link } from "react-router-dom";
import { FiArrowRight, FiRss } from "react-icons/fi";
import axios from "axios";
import { API_BASE } from "../../config/api";
import { Alert, Button, Card, EmptyState, Spinner } from "../../components/ui";
import { useSeo, slug } from "../../hooks/useSeo";
import "./ActusPage.css";

export interface Article {
  id: string;
  category: string;
  title: string;
  summary: string;
  source: string;
  date: string; // "YYYY-MM-DD"
  imageUrl?: string;
  url: string;
}

/**
 * Adresse publique d'un article : un fragment lisible, puis l'identifiant.
 *
 * `/actus/288241c1-5887-41fd-812f-66256edda9c3` ne dit rien — ni à un moteur,
 * ni à quelqu'un qui reçoit le lien par message. Le titre en tête change cela
 * sans rien coûter : l'identifiant reste seul lu par le code, et les anciennes
 * adresses continuent de fonctionner.
 */
export function lienArticle(a: { id: string; title: string }): string {
  return `/actus/${slug(a.title)}--${encodeURIComponent(a.id)}`;
}

/**
 * Catégories reprises telles quelles de ArticleService.classifyArticle().
 * Les inventer côté front produirait des filtres qui ne trouvent rien.
 */
export const CATEGORIES = [
  { id: "toutes", label: "Toutes les actus" },
  { id: "mobilite", label: "Mobilité & transport" },
  { id: "ecologie", label: "Écologie & climat" },
  { id: "entreprises", label: "Entreprises & RH" },
  { id: "technologie", label: "Tech & innovation" },
] as const;

const PAR_PAGE = 9;

export function formatArticleDate(iso: string) {
  return new Date(iso).toLocaleDateString("fr-FR", {
    day: "numeric", month: "long", year: "numeric",
  });
}

function truncate(text: string, max: number) {
  if (text.length <= max) return text;
  const cut = text.lastIndexOf(" ", max);
  return (cut > 0 ? text.slice(0, cut) : text.slice(0, max)) + "…";
}

/** Page rubrique : liste filtrable et paginée des articles. */
export default function ActusPage() {
  useSeo({
    titre: "Actus mobilité — covoiturage, transports et mobilité durable",
    description:
      "Le flux d'actualités CoShift sur la mobilité : covoiturage, transports en commun, mobilité douce et politiques de déplacement en Belgique et en Europe.",
    chemin: "/actus",
  });

  const [articles, setArticles] = useState<Article[]>([]);
  const [category, setCategory] = useState<string>("toutes");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(1);

  useEffect(() => {
    axios
      .get<Article[]>(`${API_BASE}/api/pwa/articles`)
      .then((r) => setArticles(r.data))
      .catch(() => setError("Impossible de contacter le serveur. Réessayez dans un moment."))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => setPage(1), [category]);

  const filtered = useMemo(
    () => (category === "toutes" ? articles : articles.filter((a) => a.category === category)),
    [category, articles],
  );

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAR_PAGE));
  const visible = filtered.slice((page - 1) * PAR_PAGE, page * PAR_PAGE);

  /* Compteur par catégorie : l'utilisateur voit avant de cliquer si un
     filtre a du contenu. */
  const counts = useMemo(() => {
    const map: Record<string, number> = { toutes: articles.length };
    for (const a of articles) map[a.category] = (map[a.category] ?? 0) + 1;
    return map;
  }, [articles]);

  return (
    <div className="container container--wide page stack-8">
      <header className="ac__header">
        <h1>Info mobilité</h1>
        <p className="ac__lead">
          L'essentiel de l'actualité transport et écologie, filtré pour les
          navetteurs.
        </p>
      </header>

      {error && <Alert tone="danger">{error}</Alert>}

      {/* Filtres : une vraie liste de boutons, pas une barre de navigation. */}
      <div className="ac__filters" role="group" aria-label="Filtrer par thème">
        {CATEGORIES.map((c) => (
          <button
            key={c.id}
            className={`ac__filter ${category === c.id ? "is-active" : ""}`}
            onClick={() => setCategory(c.id)}
            aria-pressed={category === c.id}
          >
            {c.label}
            <span className="ac__filter-count">{counts[c.id] ?? 0}</span>
          </button>
        ))}
      </div>

      {loading ? (
        <Spinner size="lg" center showLabel label="Chargement des actualités" />
      ) : visible.length === 0 ? (
        <EmptyState
          icon={<FiRss />}
          title="Aucun article dans ce thème"
          description="Choisissez un autre filtre pour retrouver le flux complet."
          action={<Button variant="secondary" onClick={() => setCategory("toutes")}>Voir tout</Button>}
        />
      ) : (
        <>
          <div className="grid-auto">
            {visible.map((a) => (
              <Card key={a.id} to={lienArticle(a)}>
                <p className="ac__meta">
                  <span className="ac__tag">{
                    CATEGORIES.find((c) => c.id === a.category)?.label ?? a.category
                  }</span>
                  <time dateTime={a.date}>{formatArticleDate(a.date)}</time>
                </p>

                <h2 className="ac__title">{a.title}</h2>
                <p className="ac__summary">{truncate(a.summary, 150)}</p>

                <p className="ac__foot">
                  <span className="ac__source">{a.source}</span>
                  <span className="ac__more" aria-hidden="true">
                    Lire <FiArrowRight />
                  </span>
                </p>
              </Card>
            ))}
          </div>

          {totalPages > 1 && (
            <nav className="ac__pagination" aria-label="Pages d'articles">
              <Button variant="secondary" size="sm" disabled={page === 1}
                      onClick={() => setPage((p) => p - 1)}>
                Précédent
              </Button>
              <p aria-live="polite">Page {page} sur {totalPages}</p>
              <Button variant="secondary" size="sm" disabled={page === totalPages}
                      onClick={() => setPage((p) => p + 1)}>
                Suivant
              </Button>
            </nav>
          )}
        </>
      )}

      <p className="ac__note">
        <Link to="/">Retour à l'accueil</Link>
      </p>
    </div>
  );
}
