import axios from "axios";
import { API_BASE } from "./api";

/**
 * Accès aux billets du blog.
 *
 * <h2>Ce qui a changé</h2>
 *
 * <p>Les billets vivaient dans `config/blog.ts` et le catalogue de traduction.
 * Le commentaire de ce fichier annonçait la suite : « le jour où le blog est
 * rédigé par plusieurs personnes et mis à jour sans redéploiement, il faudra
 * une table et un éditeur. Le composant qui affiche un billet n'aura pas à
 * changer : il reçoit déjà un titre, un chapeau et une suite de paragraphes. »
 * C'est exactement ce que renvoie l'API, et c'est pour cela que les deux pages
 * du blog n'ont presque pas bougé.</p>
 *
 * <h2>La langue n'est pas un paramètre</h2>
 *
 * <p>Elle voyage dans l'en-tête `Accept-Language`, posé une fois pour toutes
 * par `annoncerLangue`. L'ajouter ici en second endroit ouvrirait la
 * possibilité que les deux divergent.</p>
 */
export interface Billet {
  uuid: string;
  slug: string;
  category: "PRODUIT" | "CONFIDENTIALITE" | "OUVERTURE" | "CONCEPTION";
  /** Langue effectivement servie, qui peut différer de celle demandée. */
  locale: string | null;
  languesDisponibles: string[];
  title: string | null;
  lead: string | null;
  paragraphes: string[];
  readingMinutes: number;
  /** Nul pour un brouillon, que seule l'administration reçoit. */
  publishedAt: string | null;
  auteur: string | null;
}

/** Clé de traduction de la rubrique, telle que le catalogue la nomme. */
export function cleRubrique(c: Billet["category"]): string {
  return c.toLowerCase();
}

export function chargerBillets() {
  return axios.get<Billet[]>(`${API_BASE}/api/blog`).then((r) => r.data);
}

export function chargerBillet(slug: string) {
  return axios.get<Billet>(`${API_BASE}/api/blog/${slug}`).then((r) => r.data);
}
