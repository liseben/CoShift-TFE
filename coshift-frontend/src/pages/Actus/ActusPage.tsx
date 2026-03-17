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
  date: string;
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

export default function ActusPage() {
  const [articles, setArticles] = useState<Article[]>([]);
  const [activeCategory, setActiveCategory] = useState("toutes");
  const [isLoading, setIsLoading] = useState(true);

  // --- CONFIGURATION PAGINATION ---
  const [currentPage, setCurrentPage] = useState(1);
  const articlesPerPage = 5;

  useEffect(() => {
    const fetchNewsFromBackend = async () => {
      setIsLoading(true);
      try {
        const response = await axios.get(
          "http://localhost:8080/api/pwa/articles",
        );
        setArticles(response.data);
      } catch (error) {
        console.error("Erreur de connexion avec le serveur CoShift :", error);
      } finally {
        setIsLoading(false);
      }
    };
    fetchNewsFromBackend();
  }, []);

  // On revient à la page 1 si on change de catégorie
  useEffect(() => {
    setCurrentPage(1);
  }, [activeCategory]);

  const filteredNews = useMemo(() => {
    return activeCategory === "toutes"
      ? articles
      : articles.filter((a) => a.category === activeCategory);
  }, [activeCategory, articles]);

  // Logique pour n'afficher que 5 articles
  const indexOfLastArticle = currentPage * articlesPerPage;
  const indexOfFirstArticle = indexOfLastArticle - articlesPerPage;
  const currentArticles = filteredNews.slice(
    indexOfFirstArticle,
    indexOfLastArticle,
  );
  const totalPages = Math.ceil(filteredNews.length / articlesPerPage);

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
                  : activeCategory.toUpperCase()}
              </h2>
              <span className="article-count">
                {filteredNews.length} articles pertinents
              </span>
            </div>

            {isLoading ? (
              <div className="loading-spinner">Mise à jour du flux...</div>
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
                          {news.source} • {news.date}
                        </span>
                        <h3>{news.title}</h3>
                        <p>{news.summary.slice(0, 100)}...</p>
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

                {/* --- NAVIGATION PAGINATION --- */}
                {filteredNews.length > articlesPerPage && (
                  <div className="pagination-controls">
                    {currentPage > 1 && (
                      <button
                        className="pag-btn"
                        onClick={() => setCurrentPage(currentPage - 1)}
                      >
                        ← Précédent
                      </button>
                    )}

                    <span className="page-info">
                      Page <strong>{currentPage}</strong> sur {totalPages}
                    </span>

                    {currentPage < totalPages && (
                      <button
                        className="pag-btn"
                        onClick={() => setCurrentPage(currentPage + 1)}
                      >
                        Suivant →
                      </button>
                    )}
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
