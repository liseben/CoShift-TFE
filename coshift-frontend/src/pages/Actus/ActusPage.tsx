import { useState, useEffect, useMemo } from "react";
import axios from "axios";
import "./ActusPage.css";
import MobilityTagline from "../../components/MobilityTagline/MobilityTagline";

// --- CONFIGURATION ---
const API_KEYS = {
  GNEWS: "224cd1fc5f341b31301a497e19ac1d70",
  NEWSDATA: "pub_8b36222c19624593b36abfe97538b095",
};

// Mots-clés pour le tri et la classification
const RELEVANT_KEYWORDS = {
  mobilite: [
    "vélo",
    "train",
    "stib",
    "sncb",
    "trafic",
    "bus",
    "transport",
    "voiture",
    "covoiturage",
    "mobilité",
    "péage",
    "villo",
  ],
  ecologie: [
    "climat",
    "co2",
    "pollution",
    "durable",
    "écologie",
    "carbone",
    "énergie",
    "planète",
    "transition",
  ],
  entreprises: [
    "rh",
    "rse",
    "télétravail",
    "salaire",
    "bureau",
    "management",
    "emploi",
    "recrutement",
    "entreprises",
  ],
  technologie: [
    "ia",
    "app",
    "startup",
    "algorithme",
    "digital",
    "innovation",
    "tech",
    "logiciel",
  ],
};

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

// --- FONCTIONS UTILITAIRES ---

const classifyAndFilter = (title: string, desc: string): string | null => {
  const fullText = `${title} ${desc || ""}`.toLowerCase();

  // 1. CRITÈRE PLUS SOUPLE : On vérifie si le texte complet est pertinent
  const allKeywords = Object.values(RELEVANT_KEYWORDS).flat();
  const isRelevant = allKeywords.some((keyword) => fullText.includes(keyword));

  // 👇 L'erreur était ici : on utilise bien "isRelevant" maintenant !
  if (!isRelevant) return null;

  // 2. CLASSIFICATION
  for (const [category, words] of Object.entries(RELEVANT_KEYWORDS)) {
    if (words.some((word) => fullText.includes(word))) return category;
  }
  return "autre";
};
export default function ActusPage() {
  const [articles, setArticles] = useState<Article[]>([]);
  const [activeCategory, setActiveCategory] = useState("toutes");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchAllNews = async () => {
      // Changement de la clé de cache pour forcer la mise à jour
      const CACHE_KEY = "coshift_news_cache_v5";
      const cachedData = sessionStorage.getItem(CACHE_KEY);

      if (cachedData) {
        setArticles(JSON.parse(cachedData));
        setIsLoading(false);
        return;
      }

      setIsLoading(true);

      // 📅 On calcule la date du 1er Janvier de l'année en cours
      const currentYear = new Date().getFullYear();
      const startOfYear = `${currentYear}-01-01T00:00:00Z`;

      const query = "mobilité OR covoiturage OR écologie OR SNCB OR STIB";

      try {
        // 🚀 On ajoute max=100 (le max autorisé par les APIs), in=title, et sortby=relevance
        const [resGNews, resNewsData] = await Promise.all([
          axios.get(
            `https://gnews.io/api/v4/search?q=${encodeURIComponent(query)}&lang=fr&country=be&max=100&sortby=relevance&from=${startOfYear}&apikey=${API_KEYS.GNEWS}`,
          ),
          axios.get(
            `https://newsdata.io/api/1/news?apikey=${API_KEYS.NEWSDATA}&q=${encodeURIComponent(query)}&language=fr&country=be`,
          ),
        ]);

        const standardized: Article[] = [];

        // Traitement GNews
        resGNews.data.articles?.forEach((a: any) => {
          const category = classifyAndFilter(a.title, a.description || "");
          if (category) {
            standardized.push({
              id: a.url,
              category,
              title: a.title,
              summary: a.description || "Aucune description.",
              source: a.source.name,
              date: new Date(a.publishedAt).toLocaleDateString("fr-FR"),
              imageUrl: a.image,
              url: a.url,
            });
          }
        });

        // Traitement NewsData
        resNewsData.data.results?.forEach((a: any) => {
          const category = classifyAndFilter(a.title, a.description || "");
          if (category) {
            standardized.push({
              id: a.link,
              category,
              title: a.title,
              summary:
                a.description || "Information complémentaire sur la source.",
              source: a.source_id,
              date: new Date(a.pubDate).toLocaleDateString("fr-FR"),
              imageUrl:
                a.image_url ||
                "https://images.unsplash.com/photo-1596484552834-6a58f850e0a1?w=400",
              url: a.link,
            });
          }
        });

        // Suppression des doublons par titre et tri par date (plus récent d'abord)
        const uniqueArticles = standardized
          .filter(
            (v, i, a) =>
              a.findIndex(
                (t) =>
                  t.title.toLowerCase().trim() === v.title.toLowerCase().trim(),
              ) === i,
          )
          .sort(
            (a, b) => new Date(b.date).getTime() - new Date(a.date).getTime(),
          );

        setArticles(uniqueArticles);
        sessionStorage.setItem(CACHE_KEY, JSON.stringify(uniqueArticles));
      } catch (error) {
        console.error("Erreur APIs :", error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchAllNews();
  }, []);

  const filteredNews = useMemo(() => {
    return activeCategory === "toutes"
      ? articles
      : articles.filter((a) => a.category === activeCategory);
  }, [activeCategory, articles]);

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
              <div className="loading-spinner">
                Mise à jour du flux en cours...
              </div>
            ) : filteredNews.length > 0 ? (
              <div className="news-list">
                {filteredNews.map((news) => (
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
            ) : (
              <div className="empty-state">
                Aucun article pertinent trouvé pour cette catégorie.
              </div>
            )}
          </main>
        </div>
      </div>
    </div>
  );
}
