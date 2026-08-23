package com.coshift.api.dto;

import com.coshift.api.entity.BlogCategory;
import com.coshift.api.entity.BlogPost;
import com.coshift.api.entity.BlogPostTranslation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Un billet, dans une langue, tel que le site le lit.
 *
 * <p>Le corps est renvoyé <strong>découpé en paragraphes</strong> plutôt qu'en
 * un bloc à découper côté client. Le format de stockage — une ligne vide entre
 * deux paragraphes — est une affaire de serveur ; le rendre à l'interface
 * l'obligerait à connaître une convention qui ne la regarde pas, et à la
 * réimplémenter dans chaque client.</p>
 */
@Builder
@Schema(name = "Billet", description = "Un billet du blog CoShift.")
public record BlogPostResponse(

        @Schema(example = "0e5-3001-0000-4000-8000-000000003001") String uuid,
        @Schema(description = "Fragment d'URL, stable.", example = "confirmer-un-trajet") String slug,
        @Schema(example = "CONCEPTION") BlogCategory category,

        @Schema(description = "Langue effectivement servie, qui peut différer de celle demandée si le billet n'est pas traduit.",
                example = "fr")
        String locale,

        @Schema(description = "Langues dans lesquelles ce billet existe.", example = "[\"fr\",\"en\"]")
        List<String> languesDisponibles,

        @Schema(example = "Pourquoi c'est au passager de confirmer le trajet") String title,
        @Schema(description = "Chapeau affiché dans la liste.") String lead,

        @Schema(description = "Corps découpé en paragraphes, dans l'ordre.")
        List<String> paragraphes,

        @Schema(description = "Durée de lecture annoncée, en minutes.", example = "3") int readingMinutes,

        @Schema(description = "Instant de publication. Nul pour un brouillon, qui n'est servi qu'à l'administration.")
        LocalDateTime publishedAt,

        @Schema(description = "Prénom de l'auteur, ou null pour un billet sans signature.")
        String auteur
) {

    /**
     * Compose la réponse dans la langue demandée.
     *
     * <p>Seul le prénom de l'auteur est exposé. Un blog d'entreprise se signe,
     * mais le nom complet et l'adresse d'un membre du personnel n'ont rien à
     * faire sur une page publique indexée.</p>
     */
    public static BlogPostResponse from(BlogPost p, String locale) {
        BlogPostTranslation t = p.traduction(locale).orElse(null);
        return BlogPostResponse.builder()
                .uuid(p.getUuid())
                .slug(p.getSlug())
                .category(p.getCategory())
                .locale(t == null ? null : t.getLocale())
                .languesDisponibles(p.getTranslations().stream()
                        .map(BlogPostTranslation::getLocale).sorted().toList())
                .title(t == null ? null : t.getTitle())
                .lead(t == null ? null : t.getLead())
                .paragraphes(t == null ? List.of() : t.paragraphes())
                .readingMinutes(p.getReadingMinutes())
                .publishedAt(p.getPublishedAt())
                .auteur(p.getAuthor() == null ? null : p.getAuthor().getFirstname())
                .build();
    }
}
