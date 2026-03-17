package com.coshift.api.service;

import com.coshift.api.entity.Article;
import com.coshift.api.repository.ArticleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService implements CommandLineRunner {

    private final ArticleRepository articleRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${api.gnews.key}")
    private String gnewsApiKey;

    @Value("${api.newsdata.key}")
    private String newsdataApiKey;

    private static final List<String> MOBILITE = Arrays.asList("vélo", "train", "stib", "sncb", "trafic", "bus", "transport", "voiture", "covoiturage", "mobilité", "péage", "villo", "autoroute", "route", "tec", "de lijn", "e411", "ring");
    private static final List<String> ECOLOGIE = Arrays.asList("climat", "co2", "pollution", "durable", "écologie", "carbone", "énergie", "planète", "transition");
    private static final List<String> ENTREPRISES = Arrays.asList("rh", "rse", "télétravail", "salaire", "bureau", "management", "emploi", "recrutement", "entreprises");
    private static final List<String> TECHNOLOGIE = Arrays.asList("ia", "app", "startup", "algorithme", "digital", "innovation", "tech", "logiciel");

    @Override
    public void run(String... args) {
        log.info("📡 Démarrage de l'aspiration des actualités (GNews + NewsData)...");
        try {
            String query = "mobilité OR covoiturage SNCB OR STIB OR autoroute OR TEC OR \"De Lijn\" OR E411";
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

            fetchGNews(encodedQuery);
            fetchNewsData(encodedQuery);
            
            log.info("✅ Aspiration terminée et sauvegardée en base de données !");
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'aspiration des actus : ", e);
        }
    }

    private void fetchGNews(String encodedQuery) {
        try {
            String fromDate = "2026-01-01T00:00:00Z";
            //String currentYear = Year.now().toString();
            // CORRECTION : Remise du &country=be EXACTEMENT comme dans ton React
            String url = String.format(
                "https://gnews.io/api/v4/search?q=%s&lang=fr&country=be&max=100&sortby=relevance&from=%s-01-01T00:00:00Z&apikey=%s",
                encodedQuery, fromDate, gnewsApiKey
            );

            log.info("Appel GNews Historique : " + url);

            String responseJson = restTemplate.getForObject(url, String.class);
            if (responseJson != null) {
                JsonNode articlesNode = objectMapper.readTree(responseJson).path("articles");

                for (JsonNode node : articlesNode) {
                    String articleUrl = node.path("url").asText();
                    String title = node.path("title").asText().trim();
                    
                    if (!articleRepository.existsByUrl(articleUrl) && !articleRepository.existsByTitleIgnoreCase(title)) {
                        
                        String description = node.path("description").asText("");
                        if (description.isEmpty() || description.equals("null")) {
                            description = "Aucune description.";
                        }
                        
                        String category = classifyArticle(title, description);
                        if (category != null) {
                            saveArticle(category, title, description, node.path("source").path("name").asText(),
                                    node.path("publishedAt").asText().substring(0, 10), node.path("image").asText(), articleUrl);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Erreur avec GNews : " + e.getMessage());
        }
    }

    private void fetchNewsData(String encodedQuery) {
        try {
            // CORRECTION : Remise du &country=be EXACTEMENT comme dans ton React
            String url = String.format(
                "https://newsdata.io/api/1/news?apikey=%s&q=%s&language=fr&country=be",
                newsdataApiKey, encodedQuery
            );

            String responseJson = restTemplate.getForObject(url, String.class);
            if (responseJson != null) {
                JsonNode resultsNode = objectMapper.readTree(responseJson).path("results");

                for (JsonNode node : resultsNode) {
                    String articleUrl = node.path("link").asText();
                    String title = node.path("title").asText().trim();

                    if (!articleRepository.existsByUrl(articleUrl) && !articleRepository.existsByTitleIgnoreCase(title)) {
                        
                        String description = node.path("description").asText("");
                        if (description.isEmpty() || description.equals("null")) {
                            description = "Information complémentaire sur la source.";
                        }
                        
                        String category = classifyArticle(title, description);
                        if (category != null) {
                            String imageUrl = node.path("image_url").asText();
                            if (imageUrl.equals("null") || imageUrl.isEmpty()) {
                                imageUrl = "https://images.unsplash.com/photo-1596484552834-6a58f850e0a1?w=400";
                            }

                            saveArticle(category, title, description, node.path("source_id").asText(),
                                    node.path("pubDate").asText().substring(0, 10), imageUrl, articleUrl);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Erreur avec NewsData : " + e.getMessage());
        }
    }

    private void saveArticle(String category, String title, String summary, String source, String date, String imageUrl, String url) {
        Article article = Article.builder()
                .id(UUID.randomUUID().toString())
                .category(category)
                .title(title)
                .summary(summary.length() > 500 ? summary.substring(0, 497) + "..." : summary)
                .source(source)
                .date(date)
                .imageUrl(imageUrl)
                .url(url)
                .createdAt(LocalDateTime.now())
                .build();
        articleRepository.save(article);
    }

    private String classifyArticle(String title, String description) {
        String fullText = (title + " " + description).toLowerCase();

        boolean isRelevant = false;
        List<List<String>> allKeywords = Arrays.asList(MOBILITE, ECOLOGIE, ENTREPRISES, TECHNOLOGIE);
        for (List<String> list : allKeywords) {
            for (String word : list) {
                if (fullText.contains(word)) {
                    isRelevant = true;
                    break;
                }
            }
        }

        if (!isRelevant) return null;

        if (MOBILITE.stream().anyMatch(fullText::contains)) return "mobilite";
        if (ECOLOGIE.stream().anyMatch(fullText::contains)) return "ecologie";
        if (ENTREPRISES.stream().anyMatch(fullText::contains)) return "entreprises";
        if (TECHNOLOGIE.stream().anyMatch(fullText::contains)) return "technologie";

        return "autre";   
    }
}