package com.coshift.api.controller.blog;

import com.coshift.api.dto.BlogPostRequest;
import com.coshift.api.dto.BlogPostResponse;
import com.coshift.api.exception.ResourceNotFoundException;
import com.coshift.api.repository.UserRepository;
import com.coshift.api.service.BlogService;
import com.coshift.api.service.Messages;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Le blog : lecture ouverte, rédaction réservée.
 *
 * <h2>Deux régimes dans un seul contrôleur</h2>
 *
 * <p>Les deux points d'entrée de lecture sont publics — un blog qui exige un
 * compte pour être lu n'est pas un blog. Les quatre autres portent
 * {@code @PreAuthorize} et sont réservés au {@code SUPER_ADMIN}.</p>
 *
 * <h2>Pourquoi SUPER_ADMIN et non ADMIN</h2>
 *
 * <p>Le blog est la voix éditoriale de CoShift, pas celle d'une organisation
 * cliente. Un administrateur d'entreprise supervise son cercle ; publier sur le
 * site public au nom de la plateforme est autre chose, et c'est le rôle qui
 * répond de la plateforme qui en décide.</p>
 */
@RestController
@RequestMapping("/api/blog")
@RequiredArgsConstructor
@Tag(name = "Blog", description = "Les textes de CoShift. Lecture publique, rédaction réservée.")
public class BlogController {

    private final BlogService blogService;
    private final UserRepository userRepository;
    private final Messages messages;

    // ──────────────────────────── Lecture publique ───────────────────────────

    @Operation(
            summary = "Lister les billets publiés",
            description = """
                    Du plus récent au plus ancien. Les brouillons n'y figurent pas.

                    Le texte est servi dans la langue de l'en-tête `Accept-Language`. Si le
                    billet n'existe pas dans cette langue, il est servi dans une langue
                    disponible plutôt que vide, et le champ `locale` dit laquelle — un billet
                    lisible en français vaut mieux qu'un billet retenu jusqu'à sa traduction.""")
    @ApiResponse(responseCode = "200", description = "Billets publiés.")
    @GetMapping
    public ResponseEntity<List<BlogPostResponse>> billets() {
        return ResponseEntity.ok(blogService.billets(langue()));
    }

    @Operation(
            summary = "Lire un billet",
            description = """
                    Par son fragment d'URL.

                    Un brouillon répond **404** et non 403 : pour le public, un billet non
                    publié n'existe pas, et distinguer les deux apprendrait qu'un texte est
                    en préparation derrière cette adresse.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Le billet."),
            @ApiResponse(responseCode = "404", description = "Aucun billet publié pour ce fragment.", content = @Content())
    })
    @GetMapping("/{slug}")
    public ResponseEntity<BlogPostResponse> billet(
            @Parameter(description = "Fragment d'URL du billet.", example = "confirmer-un-trajet")
            @PathVariable String slug) {
        return ResponseEntity.ok(blogService.billet(slug, langue()));
    }

    // ────────────────────────────── Rédaction ────────────────────────────────

    @Operation(
            summary = "Lister tous les billets, brouillons compris",
            description = """
                    Triés sur la date de création et non de publication : un brouillon n'en a
                    pas, et il finirait en queue de liste alors que c'est lui qui attend une
                    action.""")
    @ApiResponse(responseCode = "200", description = "Tous les billets.")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/administration")
    public ResponseEntity<List<BlogPostResponse>> tous() {
        return ResponseEntity.ok(blogService.tousLesBillets(langue()));
    }

    @Operation(
            summary = "Écrire un billet",
            description = """
                    Au moins une traduction est exigée, aucune en particulier : un billet
                    rédigé d'abord en anglais est un billet valable.

                    Le corps est du **texte brut**, paragraphes séparés par une ligne vide.
                    Le HTML n'est pas interprété : l'accepter ouvrirait une injection de
                    script sur une page publique, et la première personne à en profiter
                    serait celle qui compromettrait un compte d'administration.

                    `publie` à faux garde le billet en brouillon, invisible du public.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Billet créé."),
            @ApiResponse(responseCode = "400", description = "Champ manquant, ou fragment d'URL mal formé.", content = @Content()),
            @ApiResponse(responseCode = "409", description = "Ce fragment d'URL est déjà pris.", content = @Content())
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<BlogPostResponse> creer(@Valid @RequestBody BlogPostRequest demande,
                                                  Authentication auth) {
        var auteur = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException(messages.get("auth.utilisateurIntrouvable")));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(blogService.creer(auteur, demande, langue()));
    }

    @Operation(
            summary = "Modifier un billet",
            description = """
                    **Le fragment d'URL est figé dès la publication.** Il est indexé par les
                    moteurs et partagé par les lecteurs : le modifier casserait chaque lien en
                    circulation, sans que personne le remarque avant de recevoir une erreur.
                    Sur un brouillon, aucun lien n'existe et il se corrige librement.

                    Les traductions sont **remplacées**, pas fusionnées : c'est ce qui permet
                    de retirer une version devenue fausse.

                    Repasser `publie` à faux retire le billet du public sans le détruire, et
                    republier lui rend sa date d'origine plutôt que de le faire remonter en
                    tête comme s'il était neuf.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Billet modifié."),
            @ApiResponse(responseCode = "409", description = "Fragment d'URL figé ou déjà pris.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Aucun billet pour cet identifiant.", content = @Content())
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/{uuid}")
    public ResponseEntity<BlogPostResponse> modifier(@PathVariable String uuid,
                                                     @Valid @RequestBody BlogPostRequest demande) {
        return ResponseEntity.ok(blogService.modifier(uuid, demande, langue()));
    }

    @Operation(
            summary = "Supprimer un billet",
            description = """
                    Suppression réelle, contrairement à l'effacement d'un compte qui anonymise :
                    un billet n'engage que son auteur, et rien ne se perd pour un tiers.

                    Pour retirer un texte du public sans le détruire, le repasser en brouillon.""")
    @ApiResponse(responseCode = "204", description = "Billet supprimé.")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> supprimer(@PathVariable String uuid) {
        blogService.supprimer(uuid);
        return ResponseEntity.noContent().build();
    }

    /** Langue de la requête, telle que le catalogue la nomme. */
    private String langue() {
        return messages.langueCourante().getLanguage();
    }
}
