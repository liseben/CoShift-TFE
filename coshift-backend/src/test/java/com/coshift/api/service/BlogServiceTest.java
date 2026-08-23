package com.coshift.api.service;

import com.coshift.api.dto.BlogPostRequest;
import com.coshift.api.entity.BlogCategory;
import com.coshift.api.entity.BlogPost;
import com.coshift.api.entity.BlogPostTranslation;
import com.coshift.api.entity.User;
import com.coshift.api.exception.ConflictException;
import com.coshift.api.exception.ResourceNotFoundException;
import com.coshift.api.repository.BlogPostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le blog.
 *
 * <p>Deux choses valent d'etre protegees : le <strong>decoupage en
 * paragraphes</strong>, qui est la seule convention de format du stockage, et
 * le <strong>fragment d'URL fige apres publication</strong>, qui est ce qui
 * empeche de casser silencieusement les liens deja partages.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BlogService — billets du blog")
class BlogServiceTest {

    @Mock private BlogPostRepository repository;
    @Mock private Messages messages;

    @InjectMocks private BlogService service;

    private User auteur;
    private BlogPost publie;
    private BlogPost brouillon;

    @BeforeEach
    void preparer() {
        auteur = User.builder().id(1L).uuid("u-1").email("fanny@coshift.be").firstname("Fanny").build();

        publie = billet("deja-publie", LocalDateTime.now().minusDays(10));
        brouillon = billet("encore-brouillon", null);

        when(messages.get(anyString())).thenReturn("message");
        when(repository.save(any(BlogPost.class))).thenAnswer(i -> i.getArgument(0));
        when(repository.findByUuid("uuid-deja-publie")).thenReturn(Optional.of(publie));
        when(repository.findByUuid("uuid-encore-brouillon")).thenReturn(Optional.of(brouillon));
    }

    private static BlogPost billet(String slug, LocalDateTime publieLe) {
        BlogPost p = BlogPost.builder()
                .id(1L).uuid("uuid-" + slug).slug(slug)
                .category(BlogCategory.CONCEPTION)
                .readingMinutes(3)
                .publishedAt(publieLe)
                .translations(new ArrayList<>())
                .build();
        p.getTranslations().add(BlogPostTranslation.builder()
                .post(p).locale("fr").title("Titre").lead("Chapeau")
                .body("Un.\n\nDeux.").build());
        return p;
    }

    private static BlogPostRequest demande(String slug, boolean publie, String... langues) {
        BlogPostRequest r = new BlogPostRequest();
        r.setSlug(slug);
        r.setCategory(BlogCategory.PRODUIT);
        r.setReadingMinutes(4);
        r.setPublie(publie);
        List<BlogPostRequest.Traduction> ts = new ArrayList<>();
        for (String l : langues) {
            BlogPostRequest.Traduction t = new BlogPostRequest.Traduction();
            t.setLocale(l);
            t.setTitle("  Titre " + l + "  ");
            t.setLead("Chapeau " + l);
            t.setBody("Premier.\n\nDeuxieme.\n\n\n  \n\nTroisieme.  ");
            ts.add(t);
        }
        r.setTraductions(ts);
        return r;
    }

    @Nested
    @DisplayName("Decoupage du corps en paragraphes")
    class Paragraphes {

        @Test
        @DisplayName("une ligne vide separe deux paragraphes")
        void ligneVideSepare() {
            BlogPostTranslation t = BlogPostTranslation.builder()
                    .body("Premier.\n\nDeuxieme.\n\nTroisieme.").build();

            assertThat(t.paragraphes()).containsExactly("Premier.", "Deuxieme.", "Troisieme.");
        }

        @Test
        @DisplayName("les lignes vides en trop et les espaces de fin sont absorbes")
        void toleranceALaFrappe() {
            /* Sans cette tolerance, une frappe de trop dans l'editeur produirait
               un paragraphe vide sur la page publique — visible, et inexplicable
               pour qui n'a pas ecrit le texte. */
            BlogPostTranslation t = BlogPostTranslation.builder()
                    .body("Premier.  \n\n\n   \n\n  Deuxieme.\n").build();

            assertThat(t.paragraphes()).containsExactly("Premier.", "Deuxieme.");
        }

        @Test
        @DisplayName("un corps vide ne donne aucun paragraphe, et ne leve pas")
        void corpsVide() {
            assertThat(BlogPostTranslation.builder().body("").build().paragraphes()).isEmpty();
            assertThat(BlogPostTranslation.builder().body(null).build().paragraphes()).isEmpty();
        }

        @Test
        @DisplayName("un saut de ligne simple ne coupe pas : c'est le meme paragraphe")
        void sautSimpleNeCoupePas() {
            BlogPostTranslation t = BlogPostTranslation.builder()
                    .body("Une phrase\nqui continue.").build();

            assertThat(t.paragraphes()).containsExactly("Une phrase\nqui continue.");
        }
    }

    @Nested
    @DisplayName("Choix de la traduction servie")
    class Traduction {

        @Test
        @DisplayName("sert la langue demandee quand elle existe")
        void langueDemandee() {
            publie.getTranslations().add(BlogPostTranslation.builder()
                    .post(publie).locale("en").title("Title").lead("Lead").body("One.").build());

            assertThat(publie.traduction("en")).isPresent()
                    .get().extracting(BlogPostTranslation::getTitle).isEqualTo("Title");
        }

        @Test
        @DisplayName("se rabat sur une langue disponible plutot que de ne rien servir")
        void repliSurLaLangueDisponible() {
            /* Le repli n'est pas « le francais » mais « la premiere
               disponible » : le jour ou un billet est redige d'abord en
               neerlandais, imposer le francais produirait une page vide alors
               qu'un texte existe. */
            assertThat(publie.traduction("en")).isPresent()
                    .get().extracting(BlogPostTranslation::getLocale).isEqualTo("fr");
        }

        @Test
        @DisplayName("un billet sans traduction ne leve pas")
        void aucuneTraduction() {
            publie.getTranslations().clear();
            assertThat(publie.traduction("fr")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Lecture publique")
    class Lecture {

        @Test
        @DisplayName("un brouillon repond introuvable, pas interdit")
        void brouillonIntrouvable() {
            /* Pour le public, un billet non publie n'existe pas. Distinguer les
               deux apprendrait qu'un texte est en preparation derriere cette
               adresse. */
            when(repository.findBySlug("encore-brouillon")).thenReturn(Optional.of(brouillon));

            assertThatThrownBy(() -> service.billet("encore-brouillon", "fr"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("un billet publie est servi")
        void billetPublie() {
            when(repository.findBySlug("deja-publie")).thenReturn(Optional.of(publie));

            assertThat(service.billet("deja-publie", "fr").title()).isEqualTo("Titre");
        }
    }

    @Nested
    @DisplayName("Redaction")
    class Redaction {

        @Test
        @DisplayName("refuse un fragment d'URL deja pris")
        void slugUnique() {
            when(repository.existsBySlug("deja-publie")).thenReturn(true);

            assertThatThrownBy(() -> service.creer(auteur, demande("deja-publie", true, "fr"), "fr"))
                    .isInstanceOf(ConflictException.class);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("cree un brouillon sans date de publication")
        void brouillonSansDate() {
            var reponse = service.creer(auteur, demande("neuf", false, "fr"), "fr");

            assertThat(reponse.publishedAt()).isNull();
            assertThat(reponse.auteur()).isEqualTo("Fanny");
        }

        @Test
        @DisplayName("accepte un billet redige dans une seule langue")
        void uneSeuleLangue() {
            /* Attendre la traduction pour accepter un texte ferait du francais
               une condition technique plutot qu'un choix editorial. */
            var reponse = service.creer(auteur, demande("neuf", true, "en"), "fr");

            assertThat(reponse.languesDisponibles()).containsExactly("en");
            assertThat(reponse.locale()).isEqualTo("en");
        }

        @Test
        @DisplayName("nettoie les espaces de bord a l'enregistrement")
        void nettoieLesBords() {
            var reponse = service.creer(auteur, demande("neuf", true, "fr"), "fr");

            assertThat(reponse.title()).isEqualTo("Titre fr");
            assertThat(reponse.paragraphes()).containsExactly("Premier.", "Deuxieme.", "Troisieme.");
        }
    }

    @Nested
    @DisplayName("Modification")
    class Modification {

        @Test
        @DisplayName("refuse de changer le fragment d'URL d'un billet publie")
        void slugFigeApresPublication() {
            /* Il est indexe par les moteurs et partage par les lecteurs : le
               modifier casserait chaque lien en circulation, sans que personne
               le remarque avant de recevoir une page d'erreur. */
            assertThatThrownBy(() ->
                    service.modifier("uuid-deja-publie", demande("nouvelle-adresse", true, "fr"), "fr"))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("laisse corriger le fragment d'URL d'un brouillon")
        void slugLibreSurUnBrouillon() {
            when(repository.existsBySlug("adresse-corrigee")).thenReturn(false);

            var reponse = service.modifier("uuid-encore-brouillon",
                    demande("adresse-corrigee", false, "fr"), "fr");

            assertThat(reponse.slug()).isEqualTo("adresse-corrigee");
        }

        @Test
        @DisplayName("republier ne remonte pas le billet en tete de liste")
        void republierGardeLaDateDorigine() {
            /* Republier un billet retire doit lui rendre sa date d'origine, pas
               le faire passer pour neuf. */
            LocalDateTime origine = publie.getPublishedAt();

            service.modifier("uuid-deja-publie", demande("deja-publie", true, "fr"), "fr");

            assertThat(publie.getPublishedAt()).isEqualTo(origine);
        }

        @Test
        @DisplayName("depublier retire du site sans detruire")
        void depublier() {
            service.modifier("uuid-deja-publie", demande("deja-publie", false, "fr"), "fr");

            assertThat(publie.getPublishedAt()).isNull();
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("les traductions sont remplacees, pas fusionnees")
        void remplacementDesTraductions() {
            /* C'est ce qui permet de retirer une version devenue fausse.
               Fusionner conserverait indefiniment une traduction que l'auteur a
               justement voulu enlever. */
            publie.getTranslations().add(BlogPostTranslation.builder()
                    .post(publie).locale("en").title("Title").lead("Lead").body("One.").build());

            service.modifier("uuid-deja-publie", demande("deja-publie", true, "fr"), "fr");

            assertThat(publie.getTranslations()).hasSize(1);
            assertThat(publie.getTranslations().get(0).getLocale()).isEqualTo("fr");
        }
    }
}
