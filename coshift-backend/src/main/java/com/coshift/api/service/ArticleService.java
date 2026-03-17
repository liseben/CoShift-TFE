package com.coshift.api.service;

import com.coshift.api.entity.Article;
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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {

    private final ArticleRepository articleRepository;
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
     * L'Aspirateur Automatique !
     * Démarre 5 secondes après le lancement de l'application, 
     * puis se relance tout seul toutes les 6 heures.
     */
    @Scheduled(initialDelay = 5_000, fixedRate = 6 * 60 * 60 * 1_000)
    public void fetchAllNews() {
        log.info("📡 Démarrage de l'aspiration automatique des actualités...");
        try {
            String query = "mobilité OR covoiturage OR SNCB OR STIB OR autoroute OR TEC OR \"De Lijn\" OR E411";
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            
            fetchGNews(encodedQuery);
            fetchNewsData(encodedQuery);
            
            log.info("✅ Aspiration terminée et base de données mise à jour !");
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'aspiration : ", e);
        }
    }

    private void fetchGNews(String encodedQuery) {
        try {
            String fromDate = "2026-01-01T00:00:00Z";
            String url = String.format(
                "https://gnews.io/api/v4/search?q=%s&lang=fr&country=be&max=100&sortby=relevance&from=%s&apikey=%s",
                encodedQuery, fromDate, gnewsApiKey
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
                if (!articleRepository.existsByUrl(articleUrl) && !isDuplicate(title)) {
                    saveArticle(
                        category, title, description,
                        node.path("source").path("name").asText("Inconnu"),
                        parseDate(node.path("publishedAt").asText()),
                        node.path("image").asText(""),
                        articleUrl
                    );
                    saved++;
                }
            }
            log.info("GNews : {} nouveaux articles sauvegardés.", saved);
        } catch (Exception e) {
            log.error("Erreur GNews : {}", e.getMessage());
        }
    }

    private void fetchNewsData(String encodedQuery) {
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

                if (!articleRepository.existsByUrl(articleUrl) && !isDuplicate(title)) {
                    String imageUrl = node.path("image_url").asText("");
                    if ("null".equals(imageUrl) || imageUrl.isEmpty()) {
                        imageUrl = "https://images.unsplash.com/photo-1596484552834-6a58f850e0a1?w=400";
                    }

                    saveArticle(
                        category, title, description,
                        node.path("source_id").asText("Inconnu"),
                        parseDate(node.path("pubDate").asText()),
                        imageUrl,
                        articleUrl
                    );
                    saved++;
                }
            }
            log.info("NewsData : {} nouveaux articles sauvegardés.", saved);
        } catch (Exception e) {
            log.error("Erreur NewsData : {}", e.getMessage());
        }
    }

    // --- Logique Anti-Doublon (Levenshtein) ---
    private boolean isDuplicate(String rawTitle) {
        String normalized = TitleNormalizer.normalize(rawTitle);

        // 1. Recherche stricte
        if (articleRepository.existsByNormalizedTitle(normalized)) return true;

        // 2. Recherche floue (Levenshtein) sur les articles existants
        return articleRepository.findAll().stream()
            .anyMatch(a -> TitleNormalizer.areSimilar(a.getNormalizedTitle(), normalized));
    }

    private void saveArticle(String category, String title, String summary,
                              String source, LocalDate date, String imageUrl, String url) {
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
}