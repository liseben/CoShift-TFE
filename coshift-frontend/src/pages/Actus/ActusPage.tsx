import { useState, useEffect, useMemo } from "react";
import axios from "axios";
import "./ActusPage.css";
import MobilityTagline from "../../components/MobilityTagline/MobilityTagline";

interface Article {
  id: string;
  category: string;
  title: string;
  summary: string;
  source: string;
  date: string; // Le format reçu du backend sera "YYYY-MM-DD"
  imageUrl: string;
  url: string;
}

const CATEGORIES = [
  { id: "toutes", label: "Toutes les actus", icon: "📰" },
  { id: "mobilite", label: "Mobilité & Transport", icon: "🚗" },
  { id: "ecologie", label: "Écologie & Climat", icon: "🌍" },
  { id: "entreprises", label: "Entreprises & RH", icon: "🏢" },
  { id: "technologie", label: "Tech & Innovation", icon: "🛠️" },
  { id: "autre", label: "Autres", icon: "🔗" },
];

import { API_BASE } from "../../config/api";
const ARTICLES_PER_PAGE = 5;

// Fonction propre pour couper le texte sans casser un mot
function truncateAtWord(text: string, maxLen: number): string {
  if (text.length <= maxLen) return text;
  const cut = text.lastIndexOf(" ", maxLen);
  return (cut > 0 ? text.slice(0, cut) : text.slice(0, maxLen)) + "…";
}

export default function ActusPage() {
  const [articles, setArticles] = useState<Article[]>([]);
  const [activeCategory, setActiveCategory] = useState("toutes");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(1);

  useEffect(() => {
    const fetchNews = async () => {
      setIsLoading(true);
      setError(null);
      try {
        const response = await axios.get<Article[]>(
          `${API_BASE}/api/pwa/articles`,
        );
        setArticles(response.data);
      } catch (err) {
        console.error("Erreur API:", err);
        setError(
          "Impossible de contacter le serveur CoShift. Réessaie dans un moment.",
        );
      } finally {
        setIsLoading(false);
      }
    };
    fetchNews();
  }, []);

  // Remise à zéro de la page lors d'un changement de catégorie
  useEffect(() => {
    setCurrentPage(1);
  }, [activeCategory]);

  const filteredNews = useMemo(
    () =>
      activeCategory === "toutes"
        ? articles
        : articles.filter((a) => a.category === activeCategory),
    [activeCategory, articles],
  );

  const totalPages = Math.ceil(filteredNews.length / ARTICLES_PER_PAGE);
  const currentArticles = filteredNews.slice(
    (currentPage - 1) * ARTICLES_PER_PAGE,
    currentPage * ARTICLES_PER_PAGE,
  );

  return (
    <div className="actus-page-wrapper">
      <div className="tagline-hero-zone">
        <MobilityTagline />
      </div>

      <div className="actus-floating-container">
        <header className="actus-header">
          <h1>Info Mobilité & CoShift</h1>
          <p>
            L'essentiel de l'actu transport et écologie, filtré par notre
            algorithme.
          </p>
        </header>

        <div className="actus-layout">
          <aside className="actus-sidebar">
            <h3>Flux thématiques</h3>
            <nav className="category-list">
              {CATEGORIES.map((cat) => (
                <button
                  key={cat.id}
                  className={`category-btn ${activeCategory === cat.id ? "active" : ""}`}
                  onClick={() => setActiveCategory(cat.id)}
                >
                  <span className="cat-icon">{cat.icon}</span>
                  <span className="cat-label">{cat.label}</span>
                </button>
              ))}
            </nav>
          </aside>

          <main className="actus-feed">
            <div className="feed-header">
              <h2>
                {activeCategory === "toutes"
                  ? "Dernières actualités"
                  : (CATEGORIES.find((c) => c.id === activeCategory)?.label ??
                    activeCategory)}
              </h2>
              {!isLoading && !error && (
                <span className="article-count">
                  {filteredNews.length} articles pertinents
                </span>
              )}
            </div>

            {/* GESTION DES 3 ÉTATS : CHARGEMENT, ERREUR, OU SUCCÈS */}
            {isLoading ? (
              <div className="skeleton-list">
                {Array.from({ length: 3 }).map((_, i) => (
                  <div key={i} className="news-card skeleton">
                    <div className="skeleton-img" />
                    <div className="skeleton-content">
                      <div className="skeleton-line short" />
                      <div className="skeleton-line" />
                      <div className="skeleton-line medium" />
                    </div>
                  </div>
                ))}
              </div>
            ) : error ? (
              <div className="error-state">⚠️ {error}</div>
            ) : currentArticles.length > 0 ? (
              <>
                <div className="news-list">
                  {currentArticles.map((news) => (
                    <article key={news.id} className="news-card">
                      <div
                        className="news-image"
                        style={{ backgroundImage: `url(${news.imageUrl})` }}
                      />
                      <div className="news-content">
                        <div className={`badge ${news.category}`}>
                          {news.category}
                        </div>
                        <span className="news-meta">
                          {news.source} •{" "}
                          {new Date(news.date).toLocaleDateString("fr-FR")}
                        </span>
                        <h3>{news.title}</h3>
                        <p>{truncateAtWord(news.summary, 120)}</p>
                        <a
                          href={news.url}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="read-btn"
                        >
                          Lire l'article
                        </a>
                      </div>
                    </article>
                  ))}
                </div>

                {/* PAGINATION */}
                {totalPages > 1 && (
                  <div className="pagination-controls">
                    <button
                      className="pag-btn"
                      disabled={currentPage === 1}
                      onClick={() => setCurrentPage((p) => p - 1)}
                    >
                      ← Précédent
                    </button>
                    <span className="page-info">
                      Page <strong>{currentPage}</strong> sur {totalPages}
                    </span>
                    <button
                      className="pag-btn"
                      disabled={currentPage === totalPages}
                      onClick={() => setCurrentPage((p) => p + 1)}
                    >
                      Suivant →
                    </button>
                  </div>
                )}
              </>
            ) : (
              <div className="empty-state">
                Aucun article trouvé pour cette catégorie.
              </div>
            )}
          </main>
        </div>
      </div>
    </div>
  );
}
