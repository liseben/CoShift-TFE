/**
 * Point d'entrée unique vers l'API CoShift.
 *
 * L'URL était auparavant recopiée à l'identique dans huit fichiers, ce qui
 * imposait huit modifications au moindre changement de port. Elle est
 * désormais définie ici et surchargeable via `VITE_API_URL` dans le `.env`.
 */
import axios from "axios";
import { LANGUES, type Langue } from "../i18n";

export const API_BASE: string =
  import.meta.env.VITE_API_URL ?? "http://localhost:8080";

/**
 * Annonce au serveur la langue choisie dans l'interface.
 *
 * <h2>Pourquoi ne pas laisser faire le navigateur</h2>
 *
 * <p>Le navigateur pose déjà un en-tête `Accept-Language`, mais il y met les
 * langues configurées dans ses préférences système. Quelqu'un dont l'ordinateur
 * est en néerlandais et qui a demandé l'anglais dans CoShift recevrait donc des
 * messages d'erreur en néerlandais — ou, faute de catalogue, en français.</p>
 *
 * <p>L'en-tête est donc écrasé avec le choix explicite de la personne. Il porte
 * une seule langue, sans facteur de qualité : il n'y a rien à négocier, la
 * réponse est connue.</p>
 *
 * <h2>Ce que cela couvre</h2>
 *
 * <p>Tous les messages composés par le serveur : erreurs de validation,
 * conflits, refus d'autorisation, et le contenu des courriels de vérification
 * et de réinitialisation, qui reprennent la langue de la requête ayant
 * déclenché l'envoi.</p>
 */
export function annoncerLangue(langue: Langue): void {
  axios.defaults.headers.common["Accept-Language"] = LANGUES[langue].balise;
}
