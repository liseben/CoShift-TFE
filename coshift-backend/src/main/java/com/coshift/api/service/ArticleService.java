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

import java.time.LocalDateTime;
import java.time.Year;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService implements CommandLineRunner {
  private final ArticleRepository articleRepository;
    private final RestTemplate restTemplate = new RestTemplate(); // Pour faire les requêtes HTTP
    private final ObjectMapper objectMapper = new ObjectMapper(); // Pour lire le JSON

    @Value("${api.gnews.key}")
    private String gnewsApiKey;

    @Value("${api.newsdata.key}")
    private String newsdataApiKey;

    // Cette méthode se lance TOUTE SEULE au démarrage du serveur (Astuce B)
    @Override
    public void run(String... args) {
        log.info("📡 Démarrage de l'aspiration des actualités...");
        try {
            fetchGNews();
            // Tu pourras décommenter fetchNewsData() plus tard si besoin, 
            // mais GNews suffit souvent pour commencer et éviter les conflits !
            // fetchNewsData(); 
            log.info("✅ Aspiration terminée et sauvegardée en base de données !");
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'aspiration des actus : ", e);
        }
    }

    private void fetchGNews() throws Exception {
        String currentYear = Year.now().toString();
        String query = "mobilité OR covoiturage OR écologie OR SNCB OR STIB OR Bruxelles";
        String url = String.format(
            "https://gnews.io/api/v4/search?q=%s&lang=fr&country=be&max=100&sortby=relevance&from=%s-01-01T00:00:00Z&apikey=%s",
            query, currentYear, gnewsApiKey
        );

        log.info("Appel à l'API GNews...");
        String responseJson = restTemplate.getForObject(url, String.class);
        
        if (responseJson != null) {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode articlesNode = root.path("articles");

            for (JsonNode node : articlesNode) {
                String articleUrl = node.path("url").asText();
                
                // On vérifie si on n'a pas DÉJÀ cet article en base de données
                if (!articleRepository.existsByUrl(articleUrl)) {
                    String title = node.path("title").asText();
                    String description = node.path("description").asText("");
                    
                    String category = classifyArticle(title, description);
                    
                    // Si notre algorithme trouve une catégorie pertinente, on sauvegarde
                    if (category != null) {
                        Article article = Article.builder()
                                .id(UUID.randomUUID().toString())
                                .category(category)
                                .title(title)
                                .summary(description.length() > 255 ? description.substring(0, 250) + "..." : description)
                                .source(node.path("source").path("name").asText())
                                .date(node.path("publishedAt").asText().substring(0, 10)) // On garde juste la date YYYY-MM-DD
                                .imageUrl(node.path("image").asText())
                                .url(articleUrl)
                                .createdAt(LocalDateTime.now())
                                .build();

                        articleRepository.save(article);
                    }
                }
            }
        }
    }

    // 🧠 PORTAGE DE NOTRE ALGORITHME REACT EN JAVA
    private String classifyArticle(String title, String description) {
        String fullText = (title + " " + description).toLowerCase();

        // 1. Est-ce pertinent ?
        if (!fullText.matches(".*(vélo|train|stib|sncb|bus|trafic|embouteillage|voiture|covoiturage|mobilité|transport|climat|écologie|carbone|co2|pollution|vert|durable|énergie|solaire|renouvelable|entreprise|employé|télétravail|rh|bureau|salaire|rse|patron|ia|app|application|tech|algorithme|données|logiciel|startup).*")) {
            return null; // On rejette
        }

        // 2. Classification précise
        if (fullText.matches(".*(vélo|train|stib|sncb|bus|trafic|embouteillage|voiture|covoiturage|mobilité|transport).*")) return "mobilite";
        if (fullText.matches(".*(climat|écologie|carbone|co2|pollution|vert|durable|énergie|solaire|renouvelable).*")) return "ecologie";
        if (fullText.matches(".*(entreprise|employé|télétravail|rh|bureau|salaire|rse|patron).*")) return "entreprises";
        if (fullText.matches(".*(ia|app|application|tech|algorithme|données|logiciel|startup).*")) return "technologie";

        return "autre";
    }
}
