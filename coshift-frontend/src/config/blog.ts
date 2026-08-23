/**
 * Index des billets du blog.
 *
 * <h2>Pourquoi ici et pas en base</h2>
 *
 * <p>La rubrique Actus est alimentée par un flux extérieur, stocké en base
 * parce qu'il change tous les jours et qu'aucun humain ne l'écrit. Le blog est
 * l'inverse : quelques textes, rédigés une fois, qui expliquent des choix du
 * projet. Leur donner une table, un écran d'administration et un éditeur
 * demanderait plus de code que les textes eux-mêmes.</p>
 *
 * <p>Ce module ne porte donc que les métadonnées. Les textes vivent dans le
 * catalogue de traduction, comme le reste de l'interface — ce qui les rend
 * traduisibles et rend impossible d'en publier un qui n'existerait que dans une
 * langue : le type du catalogue français impose sa forme à l'anglais.</p>
 *
 * <h2>Ce que devient ce fichier plus tard</h2>
 *
 * <p>Le jour où le blog est rédigé par plusieurs personnes et mis à jour sans
 * redéploiement, il faudra une table et un éditeur. Le composant qui affiche un
 * billet n'aura pas à changer : il reçoit déjà un titre, un chapeau et une
 * suite de paragraphes.</p>
 */

export interface Billet {
  /** Fragment d'URL, stable : il est indexé et partagé. */
  slug: string;
  /** Date de publication, ISO. Sert au tri et à la balise `datetime`. */
  date: string;
  /** Durée de lecture annoncée, en minutes. */
  lecture: number;
  /** Clé de la rubrique, traduite à l'affichage. */
  rubrique: "produit" | "confidentialite" | "ouverture" | "conception";
  /**
   * Nombre de paragraphes du corps.
   *
   * <p>Le composant lit `blog.<slug>.p1` … `p<paragraphes>`. Ajouter un
   * paragraphe au catalogue français sans l'ajouter à l'anglais devient une
   * erreur de compilation, pas un trou découvert en production.</p>
   */
  paragraphes: number;
}

/** Du plus récent au plus ancien : c'est l'ordre d'affichage. */
export const BILLETS: readonly Billet[] = [
  {
    slug: "confirmer-un-trajet",
    date: "2026-08-23",
    lecture: 3,
    rubrique: "conception",
    paragraphes: 6,
  },
  {
    slug: "vos-donnees-en-clair",
    date: "2026-08-19",
    lecture: 4,
    rubrique: "confidentialite",
    paragraphes: 7,
  },
  {
    slug: "donnees-ouvertes",
    date: "2026-08-14",
    lecture: 3,
    rubrique: "ouverture",
    paragraphes: 6,
  },
  {
    slug: "domicile-travail",
    date: "2026-08-08",
    lecture: 4,
    rubrique: "produit",
    paragraphes: 6,
  },
] as const;

export function billetParSlug(slug: string | undefined): Billet | undefined {
  return BILLETS.find((b) => b.slug === slug);
}
