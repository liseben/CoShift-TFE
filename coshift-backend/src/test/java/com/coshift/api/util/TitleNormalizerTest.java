package com.coshift.api.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Détection des doublons d'articles.
 *
 * <p>Deux organes de presse relatent le même événement sous des titres presque
 * identiques : « Le télétravail réduit les embouteillages » et « Le télétravail
 * réduit les embouteillages en Wallonie ». Sans rapprochement, la rubrique
 * affiche deux fois la même information — ce qui était le défaut d'origine du
 * flux.</p>
 *
 * <p>Fonction pure, sans dépendance : elle se teste exhaustivement, ce qui est
 * rare et qu'il serait dommage de ne pas exploiter. Le seuil de 15 % est la
 * valeur que ces tests verrouillent : le déplacer casserait soit le
 * rapprochement de titres proches, soit la distinction de titres différents.</p>
 */
@DisplayName("TitleNormalizer — rapprochement des titres")
class TitleNormalizerTest {

    @Nested
    @DisplayName("Normalisation")
    class Normalisation {

        @Test
        @DisplayName("retire les accents")
        void retireLesAccents() {
            // « TEC » et « E411 » doivent être captés quelle que soit la façon
            // dont l'article les écrit.
            assertThat(TitleNormalizer.normalize("Réduction des émissions à Liège"))
                    .isEqualTo("reduction des emissions a liege");
        }

        @ParameterizedTest(name = "« {0} » devient « {1} »")
        @CsvSource({
                "'Le Télétravail !', 'le teletravail'",
                "'Mobilité : quel avenir ?', 'mobilite quel avenir'",
                "'E411 — travaux', 'e411 travaux'",
                "'  espaces   multiples  ', 'espaces multiples'",
                "'MAJUSCULES', 'majuscules'",
        })
        @DisplayName("met en minuscules, retire la ponctuation et resserre les espaces")
        void normalise(String entree, String attendu) {
            assertThat(TitleNormalizer.normalize(entree)).isEqualTo(attendu);
        }

        @Test
        @DisplayName("rend une chaîne vide sur une entrée absente")
        void toleUneEntreeAbsente() {
            // Un titre nul ne doit pas faire tomber l'aspiration entière.
            assertThat(TitleNormalizer.normalize(null)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "!!!", "??? ..."})
        @DisplayName("rend une chaîne vide quand il ne reste rien d'alphanumérique")
        void videQuandRienNeReste(String entree) {
            assertThat(TitleNormalizer.normalize(entree)).isEmpty();
        }

        @Test
        @DisplayName("conserve les chiffres")
        void conserveLesChiffres() {
            assertThat(TitleNormalizer.normalize("Ring R0 : 3 bandes")).isEqualTo("ring r0 3 bandes");
        }
    }

    @Nested
    @DisplayName("Similarité")
    class Similarite {

        @Test
        @DisplayName("rapproche deux titres identiques")
        void rapprocheDeuxTitresIdentiques() {
            assertThat(TitleNormalizer.areSimilar("le teletravail recule", "le teletravail recule"))
                    .isTrue();
        }

        @Test
        @DisplayName("rapproche deux titres différant d'une faute de frappe")
        void rapprocheUneFauteDeFrappe() {
            // Un caractère sur vingt-trois : bien en deçà du seuil.
            assertThat(TitleNormalizer.areSimilar(
                    "le teletravail reduit les embouteillages",
                    "le teletravail reduit les embouteillage")).isTrue();
        }

        @Test
        @DisplayName("distingue deux titres qui parlent d'autre chose")
        void distingueDeuxSujets() {
            assertThat(TitleNormalizer.areSimilar(
                    "le teletravail reduit les embouteillages",
                    "la sncb annonce une hausse des tarifs")).isFalse();
        }

        @Test
        @DisplayName("distingue un titre allongé d'un quart")
        void distingueUnTitreAllonge() {
            // Au-delà de 15 % d'écart, ce n'est plus une variante : c'est une
            // information supplémentaire, donc un article différent.
            assertThat(TitleNormalizer.areSimilar(
                    "le teletravail reduit les embouteillages",
                    "le teletravail reduit les embouteillages en wallonie selon une etude"))
                    .isFalse();
        }

        @Test
        @DisplayName("rapproche deux chaînes vides")
        void rapprocheDeuxChainesVides() {
            // Division par zéro évitée : deux titres illisibles se valent.
            assertThat(TitleNormalizer.areSimilar("", "")).isTrue();
        }

        @Test
        @DisplayName("distingue une chaîne vide d'un titre réel")
        void distingueLeVideDuPlein() {
            assertThat(TitleNormalizer.areSimilar("", "un titre bien reel")).isFalse();
        }

        @Test
        @DisplayName("est symétrique")
        void estSymetrique() {
            // La distance d'édition l'est ; le rapprochement doit l'être aussi,
            // sinon l'ordre d'arrivée des articles changerait le résultat.
            String a = "mobilite douce a namur";
            String b = "mobilite douce a namurr";

            assertThat(TitleNormalizer.areSimilar(a, b))
                    .isEqualTo(TitleNormalizer.areSimilar(b, a));
        }

        @Test
        @DisplayName("s'applique après normalisation, accents et casse compris")
        void sAppliqueApresNormalisation() {
            // C'est l'usage réel : on normalise, puis on compare.
            String a = TitleNormalizer.normalize("Mobilité douce à Namur !");
            String b = TitleNormalizer.normalize("MOBILITE DOUCE A NAMUR");

            assertThat(TitleNormalizer.areSimilar(a, b)).isTrue();
        }
    }
}
