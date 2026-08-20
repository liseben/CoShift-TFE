import { useEffect } from "react";

/**
 * Métadonnées de référencement propres à une page.
 *
 * <h2>Le problème que ce module résout</h2>
 *
 * L'application est une page unique : `index.html` porte un seul titre et une
 * seule description, servis à l'identique pour l'accueil, un article ou la page
 * À propos. Un moteur qui exécute le JavaScript finit par voir le contenu réel,
 * mais il n'a qu'un titre pour distinguer 129 articles — et un titre identique
 * sur toutes les pages est traité comme du contenu dupliqué.
 *
 * <h2>Pourquoi une écriture impérative plutôt que des balises rendues</h2>
 *
 * React 19 sait remonter une balise `<title>` ou `<meta>` rendue dans un
 * composant vers le `<head>`. Mais il ne remplace pas les balises déjà
 * présentes dans `index.html` : la page se retrouverait avec deux descriptions,
 * et rien ne garantit laquelle un moteur retiendrait. On modifie donc les
 * balises existantes en place, et on ne crée que celles qui manquent.
 *
 * <h2>Ce que ce module ne résout pas</h2>
 *
 * Ces balises sont écrites **par le navigateur**. Les robots des réseaux
 * sociaux — Facebook, LinkedIn, WhatsApp — n'exécutent pas le JavaScript et ne
 * verront donc jamais que les valeurs par défaut d'`index.html`. Seul un
 * pré-rendu à la construction corrigerait cela ; c'est documenté comme la
 * prochaine étape.
 */
export interface Seo {
  /** Sans le suffixe « — CoShift », ajouté ici. */
  titre: string;
  description: string;
  /** Chemin canonique, sans le domaine. Défaut : l'URL courante. */
  chemin?: string;
  /** URL absolue de l'image de partage. Défaut : celle d'`index.html`. */
  image?: string;
  /** `website` pour les pages du site, `article` pour un article. */
  type?: "website" | "article";
  /** Retire la page de l'indexation. Vrai pour tout ce qui est derrière un compte. */
  horsIndex?: boolean;
}

const SUFFIXE = " — CoShift";

function poser(selecteur: string, attribut: string, valeur: string) {
  let balise = document.head.querySelector<HTMLMetaElement | HTMLLinkElement>(selecteur);
  if (!balise) {
    balise = document.createElement(selecteur.startsWith("link") ? "link" : "meta");
    // Le sélecteur a la forme [name="x"] ou [property="x"] ou link[rel="x"].
    const m = selecteur.match(/\[(\w+)="([^"]+)"\]/);
    if (m) balise.setAttribute(m[1], m[2]);
    if (selecteur.startsWith("link")) balise.setAttribute("rel", "canonical");
    document.head.appendChild(balise);
  }
  balise.setAttribute(attribut, valeur);
}

export function useSeo({ titre, description, chemin, image, type = "website", horsIndex }: Seo) {
  useEffect(() => {
    const titreComplet = titre.endsWith("CoShift") ? titre : titre + SUFFIXE;
    const url = window.location.origin + (chemin ?? window.location.pathname);

    document.title = titreComplet;
    poser('meta[name="description"]', "content", description);
    poser('link[rel="canonical"]', "href", url);

    poser('meta[property="og:title"]', "content", titreComplet);
    poser('meta[property="og:description"]', "content", description);
    poser('meta[property="og:url"]', "content", url);
    poser('meta[property="og:type"]', "content", type);
    poser('meta[property="og:locale"]', "content", "fr_BE");
    if (image) poser('meta[property="og:image"]', "content", image);

    poser('meta[name="twitter:card"]', "content", image ? "summary_large_image" : "summary");
    poser('meta[name="twitter:title"]', "content", titreComplet);
    poser('meta[name="twitter:description"]', "content", description);
    if (image) poser('meta[name="twitter:image"]', "content", image);

    /* Les pages derrière un compte n'ont rien à faire dans un index : elles
       renverraient un écran de connexion à qui cliquerait le résultat. */
    if (horsIndex) poser('meta[name="robots"]', "content", "noindex, follow");
    else document.head.querySelector('meta[name="robots"]')?.remove();
  }, [titre, description, chemin, image, type, horsIndex]);
}

/**
 * Insère un bloc de données structurées et le retire au démontage.
 *
 * <p>Sans le retrait, naviguer d'un article à un autre empilerait les blocs et
 * décrirait la page avec les données de celles déjà visitées.</p>
 */
export function useDonneesStructurees(donnees: object | null) {
  useEffect(() => {
    if (!donnees) return;
    const balise = document.createElement("script");
    balise.type = "application/ld+json";
    balise.textContent = JSON.stringify(donnees);
    document.head.appendChild(balise);
    return () => { balise.remove(); };
  }, [donnees]);
}

/**
 * Fabrique un fragment d'URL lisible à partir d'un titre.
 *
 * <p>Une adresse comme `/actus/288241c1-5887-41fd-812f-66256edda9c3` ne dit
 * rien, ni à un lecteur qui la reçoit par message, ni à un moteur. Le fragment
 * lisible précède l'identifiant, qui reste la seule chose que le code lit.</p>
 */
export function slug(texte: string, longueurMax = 60): string {
  return texte
    .normalize("NFD").replace(/\p{Diacritic}/gu, "")   // retire les accents
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, longueurMax)
    .replace(/-+$/, "");
}
