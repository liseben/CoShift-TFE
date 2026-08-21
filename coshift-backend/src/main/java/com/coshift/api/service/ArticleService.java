package com.coshift.api.service;

import com.coshift.api.entity.Article;
import com.coshift.api.exception.ResourceNotFoundException;
import com.coshift.api.repository.ArticleRepository;
import com.coshift.api.util.TitleNormalizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final Messages messages;
    private final ObjectMapper objectMapper = new ObjectMapper();
    // Utilisation du nouveau RestClient (moderne) au lieu de RestTemplate
    private final RestClient restClient = RestClient.create();

    @Value("${api.gnews.key}")
    private String gnewsApiKey;

    @Value("${api.newsdata.key}")
    private String newsdataApiKey;

    // --- Mots-clés de classification (toujours en minuscules) ---
    private static final List<String> MOBILITE = Arrays.asList(
        "vélo", "velo", "train", "stib", "sncb", "trafic", "bus", "transport",
        "voiture", "covoiturage", "mobilité", "mobilite", "péage", "peage",
        "villo", "autoroute", "route", "tec", "de lijn", "e411", "ring"
    );
    private static final List<String> ECOLOGIE = Arrays.asList(
        "climat", "co2", "pollution", "durable", "écologie", "ecologie",
        "carbone", "énergie", "energie", "planète", "planete", "transition"
    );
    private static final List<String> ENTREPRISES = Arrays.asList(
        "rh", "rse", "télétravail", "teletravail", "salaire", "bureau",
        "management", "emploi", "recrutement", "entreprise"
    );
    private static final List<String> TECHNOLOGIE = Arrays.asList(
        "ia", "app", "startup", "algorithme", "digital", "innovation", "tech", "logiciel"
    );

    /**
     * Requête adressée aux deux agrégateurs.
     *
     * <h2>Pourquoi les parenthèses</h2>
     *
     * <p>La formulation précédente enchaînait huit termes séparés par
     * {@code OR}, dont une locution entre guillemets, sans parenthèses. GNews
     * la refusait avec un {@code 400} et le message
     * « The query has a syntax error » : à chaque passage, la moitié de
     * l'aspiration échouait silencieusement, seul NewsData alimentait la
     * rubrique.</p>
     *
     * <p>Une disjonction mêlant termes simples et locution doit être
     * parenthésée pour que l'analyseur de GNews sache où elle commence et où
     * elle finit.</p>
     */
    private static final String REQUETE =
            "(mobilité OR covoiturage OR SNCB OR STIB OR autoroute OR TEC OR E411 OR \"De Lijn\")";

    /**
     * Nombre d'articles demandés par appel.
     *
     * <p>La valeur était fixée à 100 dans l'URL. Les formules gratuites de
     * GNews plafonnent à dix : demander davantage n'apportait rien et exposait
     * à un refus. Externalisée pour suivre un éventuel changement de formule
     * sans toucher au code.</p>
     */
    @Value("${app.news.max-articles:10}")
    private int maxArticles;

    /**
     * Profondeur de l'historique interrogé, en jours.
     *
     * <p>La date de début était écrite en dur au 1er janvier 2026. Une borne
     * fixe se périme : elle élargit la fenêtre indéfiniment et finit par
     * ramener des articles d'un an d'âge dans une rubrique d'actualité.</p>
     */
    @Value("${app.news.window-days:30}")
    private int fenetreJours;

    /**
     * L'Aspirateur Automatique !
     * Démarre 5 secondes après le lancement de l'application,
     * puis se relance tout seul toutes les 6 heures.
     *
     * <p>Les deux délais sont externalisés pour que les tests puissent repousser
     * le premier déclenchement : sinon le planificateur part pendant l'exécution
     * de la suite et appelle réellement GNews et NewsData.</p>
     */
    @Scheduled(
            initialDelayString = "${app.news.initial-delay:5000}",
            fixedRateString    = "${app.news.fixed-rate:21600000}")
    public void fetchAllNews() {
        log.info("Démarrage de l'aspiration automatique des actualités...");
        try {
            String encodedQuery = URLEncoder.encode(REQUETE, StandardCharsets.UTF_8);

            /* L'index est construit une fois pour tout le passage, puis tenu à
               jour au fur et à mesure des enregistrements. Auparavant, chaque
               article candidat déclenchait un rechargement complet de la table. */
            Index index = new Index(
                    new HashSet<>(articleRepository.findAllUrls()),
                    new ArrayList<>(articleRepository.findAllNormalizedTitles()));

            fetchGNews(encodedQuery, index);
            fetchNewsData(encodedQuery, index);

            log.info("Aspiration terminée : {} article(s) connu(s) en base.", index.titres.size());
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'aspiration : ", e);
        }
    }

    private void fetchGNews(String encodedQuery, Index index) {
        try {
            String depuis = LocalDateTime.now().minusDays(fenetreJours)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
            String url = String.format(
                "https://gnews.io/api/v4/search?q=%s&lang=fr&country=be&max=%d&sortby=relevance&from=%s&apikey=%s",
                encodedQuery, maxArticles, depuis, gnewsApiKey
            );

            String responseJson = restClient.get().uri(url).retrieve().body(String.class);
            if (responseJson == null) return;

            JsonNode articlesNode = objectMapper.readTree(responseJson).path("articles");
            int saved = 0;
            
            for (JsonNode node : articlesNode) {
                String articleUrl = node.path("url").asText();
                String title = node.path("title").asText("").trim();
                if (title.isEmpty() || articleUrl.isEmpty()) continue;

                String description = node.path("description").asText("");
                if (description.isEmpty() || "null".equals(description)) {
                    description = "Aucune description disponible.";
                }

                String category = classifyArticle(title, description);
                if (category == null) continue;

                // Vérification anti-doublon via URL et via l'algorithme Levenshtein
                if (!index.connait(articleUrl, title)) {
                    saveArticle(
                        category, title, description,
                        node.path("source").path("name").asText("Inconnu"),
                        parseDate(node.path("publishedAt").asText()),
                        node.path("image").asText(""),
                        articleUrl,
                        index
                    );
                    saved++;
                }
            }
            log.info("GNews : {} nouveaux articles sauvegardés.", saved);
        } catch (Exception e) {
            log.error("Erreur GNews : {}", e.getMessage());
        }
    }

    private void fetchNewsData(String encodedQuery, Index index) {
        try {
            String url = String.format(
                "https://newsdata.io/api/1/news?apikey=%s&q=%s&language=fr&country=be",
                newsdataApiKey, encodedQuery
            );

            String responseJson = restClient.get().uri(url).retrieve().body(String.class);
            if (responseJson == null) return;

            JsonNode resultsNode = objectMapper.readTree(responseJson).path("results");
            int saved = 0;
            
            for (JsonNode node : resultsNode) {
                String articleUrl = node.path("link").asText();
                String title = node.path("title").asText("").trim();
                if (title.isEmpty() || articleUrl.isEmpty()) continue;

                String description = node.path("description").asText("");
                if (description.isEmpty() || "null".equals(description)) {
                    description = "Information complémentaire sur la source.";
                }

                String category = classifyArticle(title, description);
                if (category == null) continue;

                if (!index.connait(articleUrl, title)) {
                    String imageUrl = node.path("image_url").asText("");
                    if ("null".equals(imageUrl) || imageUrl.isEmpty()) {
                        imageUrl = "https://images.unsplash.com/photo-1596484552834-6a58f850e0a1?w=400";
                    }

                    saveArticle(
                        category, title, description,
                        node.path("source_id").asText("Inconnu"),
                        parseDate(node.path("pubDate").asText()),
                        imageUrl,
                        articleUrl,
                        index
                    );
                    saved++;
                }
            }
            log.info("NewsData : {} nouveaux articles sauvegardés.", saved);
        } catch (Exception e) {
            log.error("Erreur NewsData : {}", e.getMessage());
        }
    }

    /**
     * Index des articles déjà connus, tenu en mémoire le temps d'un passage.
     *
     * <p>La détection de doublons combine deux tests : l'adresse exacte, et la
     * proximité des titres normalisés au sens de Levenshtein. Le second impose
     * de parcourir tous les titres connus ; le faire depuis la base pour chaque
     * candidat revenait à recharger la table entière à chaque article.</p>
     *
     * <p>Les adresses sont dans un ensemble — le test est exact, donc immédiat.
     * Les titres restent dans une liste : la comparaison est floue et se
     * parcourt de toute façon, mais en mémoire et sur une seule colonne.</p>
     */
    private static final class Index {
        private final Set<String> urls;
        private final List<String> titres;

        Index(Set<String> urls, List<String> titres) {
            this.urls = urls;
            this.titres = titres;
        }

        boolean connait(String url, String titreBrut) {
            if (urls.contains(url)) return true;
            String normalise = TitleNormalizer.normalize(titreBrut);
            return titres.stream().anyMatch(t -> TitleNormalizer.areSimilar(t, normalise));
        }

        void ajouter(String url, String titreNormalise) {
            urls.add(url);
            titres.add(titreNormalise);
        }
    }

    private void saveArticle(String category, String title, String summary,
                              String source, LocalDate date, String imageUrl, String url,
                              Index index) {
        String normalizedTitle = TitleNormalizer.normalize(title);
        Article article = Article.builder()
            .id(UUID.randomUUID().toString())
            .category(category)
            .title(title)
            .normalizedTitle(normalizedTitle)
            .summary(summary.length() > 1000 ? summary.substring(0, 997) + "..." : summary)
            .source(source)
            .date(date) // Maintenant en type LocalDate propre
            .imageUrl(imageUrl)
            .url(url)
            .createdAt(LocalDateTime.now())
            .build();
        articleRepository.save(article);
        /* Sans cette ligne, deux articles identiques présents dans le même lot
           seraient tous deux enregistrés : l'index ne connaîtrait que l'état
           de la base au début du passage. */
        index.ajouter(url, normalizedTitle);
    }

    private String classifyArticle(String title, String description) {
        String fullText = TitleNormalizer.normalize(title + " " + description);

        if (MOBILITE.stream().anyMatch(fullText::contains)) return "mobilite";
        if (ECOLOGIE.stream().anyMatch(fullText::contains)) return "ecologie";
        if (ENTREPRISES.stream().anyMatch(fullText::contains)) return "entreprises";
        if (TECHNOLOGIE.stream().anyMatch(fullText::contains)) return "technologie";
        
        return null;
    }

    private LocalDate parseDate(String raw) {
        try {
            if (raw == null || raw.length() < 10) return LocalDate.now();
            return LocalDate.parse(raw.substring(0, 10)); // Extrait "YYYY-MM-DD"
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    // Utilisé par le Controller !
    public List<Article> getAllArticles() {
        return articleRepository.findAllByOrderByDateDesc();
    }

    /** Un article isolé, pour la page de détail. */
    public Article getArticleById(String id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("article.introuvable")));
    }
}