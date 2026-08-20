import {
  createContext, useCallback, useContext, useEffect, useMemo, useState,
  type ReactNode,
} from "react";
import { VERSION_CONFIDENTIALITE } from "../config/legal";

/**
 * Consentement au dépôt de traceurs et au chargement de services tiers.
 *
 * <h2>Ce que la loi vise réellement</h2>
 *
 * L'article 129 de la loi du 13 juin 2005 relative aux communications
 * électroniques ne parle pas de cookies. Il vise « le stockage d'informations
 * ou l'obtention de l'accès à des informations déjà stockées dans l'équipement
 * terminal d'un abonné ou d'un utilisateur ». La formulation est neutre du
 * point de vue technique : un cookie, un jeton dans le stockage local ou une
 * empreinte de navigateur relèvent du même régime.
 *
 * <p>Répondre « nous n'utilisons pas de cookies » parce que l'on stocke dans
 * `localStorage` est donc une réponse à côté de la question.</p>
 *
 * <h2>Ce qui est soumis au consentement, et ce qui ne l'est pas</h2>
 *
 * Le même article exempte le stockage « strictement nécessaire à la fourniture
 * d'un service expressément demandé ». Le jeton d'authentification et le choix
 * de thème rentrent dans cette exemption : sans le premier il faudrait se
 * reconnecter à chaque page, et le second ne fait qu'exécuter une demande
 * explicite. Le fond cartographique et le bouton de connexion Google, eux,
 * transmettent l'adresse IP du visiteur à un tiers établi hors de l'Union sans
 * qu'aucun service demandé ne l'exige : ils sont soumis au consentement.
 *
 * <h2>Ce que ce module garantit</h2>
 *
 * <ul>
 *   <li>Rien n'est chargé chez un tiers avant une réponse explicite. Le
 *       consentement ne se déduit ni du défilement, ni de la navigation.</li>
 *   <li>Refuser coûte exactement un clic, comme accepter — l'article 4.11 du
 *       RGPD exige un consentement libre, et un refus plus coûteux que
 *       l'acceptation ne l'est pas.</li>
 *   <li>Le choix est retirable à tout moment, aussi simplement qu'il a été
 *       donné (article 7.3), par un lien permanent du pied de page.</li>
 *   <li>La preuve du consentement est conservée avec sa date et la version des
 *       documents alors en vigueur (article 7.1).</li>
 *   <li>Le choix expire au bout de six mois : un consentement perpétuel n'est
 *       plus un consentement éclairé.</li>
 * </ul>
 */

/** Services tiers soumis au consentement. */
export type ServiceTiers = "google" | "mapbox";

export interface Consentement {
  /** Connexion par compte Google — script Google Identity Services. */
  google: boolean;
  /** Fond cartographique animé — tuiles Mapbox. */
  mapbox: boolean;
  /** Horodatage ISO du choix, pour en faire la preuve. */
  date: string;
  /** Version des documents en vigueur au moment du choix. */
  version: string;
}

interface Valeur {
  /** `null` tant que la personne n'a pas répondu. */
  choix: Consentement | null;
  /** Faux tant qu'aucune réponse n'a été donnée : le bandeau reste affiché. */
  aRepondu: boolean;
  /** Un service tiers peut-il être chargé ? */
  autorise: (service: ServiceTiers) => boolean;
  accepterTout: () => void;
  refuserTout: () => void;
  enregistrer: (partiel: Pick<Consentement, "google" | "mapbox">) => void;
  /** Efface le choix et réaffiche le bandeau — retrait au sens de l'article 7.3. */
  reinitialiser: () => void;
  /** Panneau de réglage détaillé. */
  panneauOuvert: boolean;
  ouvrirPanneau: () => void;
  fermerPanneau: () => void;
}

const CLE = "coshift_consentement";

/** Six mois en millisecondes. */
const DUREE_MS = 6 * 30 * 24 * 60 * 60 * 1000;

const ConsentContext = createContext<Valeur | undefined>(undefined);

/**
 * Relit le choix enregistré.
 *
 * <p>Un choix périmé, corrompu ou rendu par une version antérieure des
 * documents est traité comme absent : le bandeau réapparaît. Une modification
 * substantielle de la politique de confidentialité invalide donc les
 * consentements antérieurs, ce qui est le comportement correct — on ne peut
 * pas consentir par avance à un texte qu'on n'a pas lu.</p>
 */
function lire(): Consentement | null {
  try {
    const brut = localStorage.getItem(CLE);
    if (!brut) return null;

    const c = JSON.parse(brut) as Partial<Consentement>;
    if (typeof c.google !== "boolean" || typeof c.mapbox !== "boolean") return null;
    if (c.version !== VERSION_CONFIDENTIALITE) return null;

    const age = Date.now() - new Date(c.date ?? 0).getTime();
    if (!Number.isFinite(age) || age > DUREE_MS) return null;

    return c as Consentement;
  } catch {
    /* Stockage indisponible — navigation privée verrouillée, quota atteint.
       Sans trace lisible, on considère qu'aucun choix n'a été fait : le
       bandeau reparaît et aucun tiers n'est chargé. C'est le repli sûr. */
    return null;
  }
}

export function ConsentProvider({ children }: { children: ReactNode }) {
  const [choix, setChoix] = useState<Consentement | null>(() => lire());
  const [panneauOuvert, setPanneauOuvert] = useState(false);

  const enregistrer = useCallback(
    ({ google, mapbox }: Pick<Consentement, "google" | "mapbox">) => {
      const valeur: Consentement = {
        google,
        mapbox,
        date: new Date().toISOString(),
        version: VERSION_CONFIDENTIALITE,
      };
      setChoix(valeur);
      setPanneauOuvert(false);
      try {
        localStorage.setItem(CLE, JSON.stringify(valeur));
      } catch {
        /* Le choix vaut pour la session même s'il ne peut pas être conservé. */
      }
    },
    [],
  );

  const accepterTout = useCallback(
    () => enregistrer({ google: true, mapbox: true }),
    [enregistrer],
  );

  const refuserTout = useCallback(
    () => enregistrer({ google: false, mapbox: false }),
    [enregistrer],
  );

  const reinitialiser = useCallback(() => {
    setChoix(null);
    setPanneauOuvert(false);
    try {
      localStorage.removeItem(CLE);
    } catch {
      /* Sans effet sur l'état en mémoire, déjà remis à zéro. */
    }
  }, []);

  /* Le choix se propage entre onglets : refuser dans l'un doit couper les
     tiers dans les autres, sans quoi le retrait serait partiel. */
  useEffect(() => {
    const surStockage = (e: StorageEvent) => {
      if (e.key === CLE) setChoix(lire());
    };
    window.addEventListener("storage", surStockage);
    return () => window.removeEventListener("storage", surStockage);
  }, []);

  const valeur = useMemo<Valeur>(
    () => ({
      choix,
      aRepondu: choix !== null,
      autorise: (service) => choix?.[service] === true,
      accepterTout,
      refuserTout,
      enregistrer,
      reinitialiser,
      panneauOuvert,
      ouvrirPanneau: () => setPanneauOuvert(true),
      fermerPanneau: () => setPanneauOuvert(false),
    }),
    [choix, accepterTout, refuserTout, enregistrer, reinitialiser, panneauOuvert],
  );

  return <ConsentContext.Provider value={valeur}>{children}</ConsentContext.Provider>;
}

export function useConsent(): Valeur {
  const ctx = useContext(ConsentContext);
  if (!ctx) throw new Error("useConsent doit être utilisé dans un ConsentProvider");
  return ctx;
}
