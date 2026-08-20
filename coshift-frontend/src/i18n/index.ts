import { fr } from "./fr";
import { en } from "./en";

/**
 * Traduction de l'interface.
 *
 * <h2>Pourquoi pas de bibliothèque</h2>
 *
 * Le réflexe serait d'ajouter `react-i18next`. Trois raisons de s'en passer
 * ici :
 *
 * <ul>
 *   <li>Le besoin est étroit — trois langues, aucun chargement différé à cette
 *       taille, aucune règle de pluriel au-delà du singulier et du pluriel
 *       simples.</li>
 *   <li>La sûreté de typage obtenue ci-dessous est meilleure que celle d'une
 *       bibliothèque configurée par défaut : une clé absente d'une traduction
 *       est une <strong>erreur de compilation</strong>, pas un texte manquant
 *       découvert en production.</li>
 *   <li>Le nombre de dépendances de l'interface est une donnée publiée dans le
 *       livrable juridique, avec l'inventaire de leurs licences. L'augmenter
 *       pour cent lignes de code demanderait une meilleure raison.</li>
 * </ul>
 *
 * <h2>Le français fait foi</h2>
 *
 * Le dictionnaire français définit la forme du catalogue ; les deux autres
 * doivent s'y conformer. C'est ce que dit `Traductions` ci-dessous, et c'est
 * ce qui rend impossible d'oublier une clé en ajoutant un écran.
 */
export type Traductions = typeof fr;

/**
 * Langues effectivement servies.
 *
 * <p>Le néerlandais est prévu et n'y figure pas encore : proposer « NL » dans
 * le sélecteur pour afficher du français serait pire que de ne pas le
 * proposer. Il s'ajoute ici le jour où `nl.ts` existe, et le sélecteur le
 * reprend automatiquement — il lit cette table plutôt qu'une liste écrite en
 * dur, ce qui était le défaut de la version précédente.</p>
 */
export const LANGUES = {
  fr: { code: "fr", etiquette: "FR", nom: "Français", balise: "fr-BE" },
  en: { code: "en", etiquette: "EN", nom: "English", balise: "en" },
} as const;

export type Langue = keyof typeof LANGUES;

/**
 * Les catalogues.
 *
 * <p>`en` est annoté `Traductions` : le compilateur refuse une clé manquante ou
 * surnuméraire. Une chaîne restée en français y passe en revanche — le typage
 * ne sait pas lire.</p>
 */
export const CATALOGUES: Record<Langue, Traductions> = { fr, en };

/** Ordre de repli. Une clé absente d'une traduction retombe sur le français. */
const REPLI: Langue = "fr";

/**
 * Lit une clé pointée dans un catalogue.
 *
 * <p>`accueil.titre` plutôt qu'un objet plat : le regroupement par écran fait
 * qu'on retrouve les chaînes d'une page côte à côte au moment de la relire,
 * ce qui est exactement le moment où l'on juge de leur cohérence.</p>
 */
function lire(catalogue: unknown, chemin: string): string | undefined {
  const valeur = chemin.split(".").reduce<unknown>(
    (noeud, cle) =>
      noeud && typeof noeud === "object" ? (noeud as Record<string, unknown>)[cle] : undefined,
    catalogue,
  );
  return typeof valeur === "string" ? valeur : undefined;
}

/**
 * Remplace les marques `{nom}` par les valeurs fournies.
 *
 * <p>Volontairement primitif : pas de format de nombre, pas de sélection de
 * pluriel. Les rares cas de pluriel sont traités par deux clés distinctes, ce
 * qui laisse le traducteur maître de sa langue — le néerlandais et l'anglais
 * n'accordent pas comme le français.</p>
 */
function interpoler(texte: string, valeurs?: Record<string, string | number>): string {
  if (!valeurs) return texte;
  return texte.replace(/\{(\w+)\}/g, (entier, cle) =>
    cle in valeurs ? String(valeurs[cle]) : entier,
  );
}

export function traduire(
  langue: Langue,
  chemin: string,
  valeurs?: Record<string, string | number>,
): string {
  const texte = lire(CATALOGUES[langue], chemin) ?? lire(CATALOGUES[REPLI], chemin);
  if (texte === undefined) {
    /* Afficher la clé serait plus explicite pour un développeur et illisible
       pour un utilisateur. L'avertissement va à la console, le lecteur voit
       une chaîne vide plutôt qu'un identifiant technique. */
    if (import.meta.env.DEV) console.warn(`[i18n] clé absente : ${chemin}`);
    return "";
  }
  return interpoler(texte, valeurs);
}

/**
 * Déduit la langue du navigateur.
 *
 * <p>`navigator.languages` est ordonné par préférence déclarée. On retient la
 * première entrée dont le préfixe correspond à une langue servie — `en-GB` et
 * `en-US` valent `en`.</p>
 */
export function langueDuNavigateur(): Langue {
  const preferees = navigator.languages?.length
    ? navigator.languages
    : [navigator.language ?? ""];

  for (const brute of preferees) {
    const prefixe = brute.toLowerCase().split("-")[0];
    if (prefixe in LANGUES) return prefixe as Langue;
  }
  return REPLI;
}

export function estUneLangue(valeur: unknown): valeur is Langue {
  return typeof valeur === "string" && valeur in LANGUES;
}
