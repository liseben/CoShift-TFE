package com.coshift.api.service;

import com.coshift.api.dto.BlogPostRequest;
import com.coshift.api.dto.BlogPostResponse;
import com.coshift.api.entity.BlogPost;
import com.coshift.api.entity.BlogPostTranslation;
import com.coshift.api.entity.User;
import com.coshift.api.exception.ConflictException;
import com.coshift.api.exception.ResourceNotFoundException;
import com.coshift.api.repository.BlogPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Le blog : lecture publique, rédaction réservée.
 *
 * <h2>Pourquoi ce service existe désormais</h2>
 *
 * <p>Les billets vivaient dans le catalogue de traduction du client, et le
 * commentaire de {@code config/blog.ts} annonçait la suite : « le jour où le
 * blog est rédigé par plusieurs personnes et mis à jour sans redéploiement, il
 * faudra une table et un éditeur ». Publier un texte demandait jusqu'ici une
 * modification du code et un déploiement.</p>
 *
 * <h2>Ce que la bascule fait perdre</h2>
 *
 * <p>Dans le catalogue, une clé absente d'une langue était une erreur de
 * compilation. En base, plus rien ne garantit qu'un billet existe dans les deux
 * langues — c'est le prix de la rédaction sans redéploiement, et la lecture s'y
 * adapte en se rabattant sur la langue disponible plutôt qu'en servant une page
 * vide.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlogService {

    private final BlogPostRepository repository;
    private final Messages messages;

    // ──────────────────────────── Lecture publique ───────────────────────────

    /** Billets publiés, du plus récent au plus ancien. */
    public List<BlogPostResponse> billets(String locale) {
        return repository.findByPublishedAtIsNotNullOrderByPublishedAtDesc().stream()
                .map(p -> BlogPostResponse.from(p, locale))
                .toList();
    }

    /**
     * Un billet publié, par son fragment d'URL.
     *
     * <p>Un brouillon répond « introuvable » et non « interdit » : pour le
     * public, un billet non publié n'existe pas, et distinguer les deux
     * apprendrait qu'un texte est en préparation derrière cette adresse.</p>
     */
    public BlogPostResponse billet(String slug, String locale) {
        BlogPost p = repository.findBySlug(slug)
                .filter(BlogPost::estPublie)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("blog.introuvable")));
        return BlogPostResponse.from(p, locale);
    }

    // ────────────────────────────── Rédaction ────────────────────────────────

    /** Tous les billets, brouillons compris. */
    public List<BlogPostResponse> tousLesBillets(String locale) {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(p -> BlogPostResponse.from(p, locale))
                .toList();
    }

    @Transactional
    public BlogPostResponse creer(User auteur, BlogPostRequest demande, String locale) {
        if (repository.existsBySlug(demande.getSlug())) {
            throw new ConflictException(messages.get("blog.slugPris"));
        }

        BlogPost billet = BlogPost.builder()
                .slug(demande.getSlug())
                .category(demande.getCategory())
                .readingMinutes(demande.getReadingMinutes())
                .publishedAt(demande.isPublie() ? LocalDateTime.now() : null)
                .author(auteur)
                .translations(new ArrayList<>())
                .build();

        appliquerTraductions(billet, demande);

        BlogPost enregistre = repository.save(billet);
        log.info("Billet « {} » créé par {} ({})", enregistre.getSlug(), auteur.getEmail(),
                enregistre.estPublie() ? "publié" : "brouillon");
        return BlogPostResponse.from(enregistre, locale);
    }

    /**
     * Met un billet à jour.
     *
     * <h2>Le fragment d'URL ne change plus après publication</h2>
     *
     * <p>Il est indexé par les moteurs et partagé par les lecteurs. Le modifier
     * casserait chaque lien déjà en circulation, sans que personne le remarque
     * avant de recevoir une page d'erreur. Tant que le billet est un brouillon,
     * en revanche, aucun lien n'existe et il se corrige librement.</p>
     *
     * <h2>Publier n'écrase pas une date déjà posée</h2>
     *
     * <p>Republier un billet retiré doit lui rendre sa date d'origine, pas le
     * faire remonter en tête de liste comme s'il était neuf.</p>
     */
    @Transactional
    public BlogPostResponse modifier(String uuid, BlogPostRequest demande, String locale) {
        BlogPost billet = repository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("blog.introuvable")));

        if (!billet.getSlug().equals(demande.getSlug())) {
            if (billet.estPublie()) {
                throw new ConflictException(messages.get("blog.slugFige"));
            }
            if (repository.existsBySlug(demande.getSlug())) {
                throw new ConflictException(messages.get("blog.slugPris"));
            }
            billet.setSlug(demande.getSlug());
        }

        billet.setCategory(demande.getCategory());
        billet.setReadingMinutes(demande.getReadingMinutes());

        if (demande.isPublie() && billet.getPublishedAt() == null) {
            billet.setPublishedAt(LocalDateTime.now());
        } else if (!demande.isPublie()) {
            billet.setPublishedAt(null);
        }

        appliquerTraductions(billet, demande);

        return BlogPostResponse.from(repository.save(billet), locale);
    }

    /**
     * Supprime un billet.
     *
     * <p>La suppression est réelle, contrairement à l'effacement d'un compte
     * qui anonymise : un billet n'engage que son auteur, et rien ne se perd
     * pour un tiers. Retirer un texte du public sans le détruire se fait en le
     * repassant en brouillon.</p>
     */
    @Transactional
    public void supprimer(String uuid) {
        BlogPost billet = repository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("blog.introuvable")));
        repository.delete(billet);
        log.info("Billet « {} » supprimé", billet.getSlug());
    }

    /**
     * Remplace les traductions par celles de la demande.
     *
     * <p>Un remplacement plutôt qu'une fusion : c'est ce qui permet de retirer
     * une traduction devenue fausse. Fusionner conserverait indéfiniment une
     * version anglaise que l'auteur a justement voulu enlever.</p>
     */
    private void appliquerTraductions(BlogPost billet, BlogPostRequest demande) {
        billet.getTranslations().clear();
        for (BlogPostRequest.Traduction t : demande.getTraductions()) {
            billet.getTranslations().add(BlogPostTranslation.builder()
                    .post(billet)
                    .locale(t.getLocale().toLowerCase())
                    .title(t.getTitle().strip())
                    .lead(t.getLead().strip())
                    .body(t.getBody().strip())
                    .build());
        }
    }
}
