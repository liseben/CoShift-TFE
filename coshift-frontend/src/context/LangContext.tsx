import {
  createContext, useCallback, useContext, useEffect, useMemo, useState,
  type ReactNode,
} from "react";
import {
  LANGUES, estUneLangue, langueDuNavigateur, traduire, type Langue,
} from "../i18n";

/**
 * Langue de l'interface.
 *
 * <h2>Comment la langue est choisie</h2>
 *
 * <ol>
 *   <li>Le choix explicite conservé dans le navigateur, s'il existe. Il prime
 *       sur tout : une personne qui a demandé l'anglais ne doit pas retrouver
 *       du français parce que son système est configuré autrement.</li>
 *   <li>À défaut, la première des langues déclarées par le navigateur que
 *       CoShift sert.</li>
 *   <li>À défaut, le français.</li>
 * </ol>
 *
 * <h2>Ce que le choix change en dehors du texte</h2>
 *
 * <p>L'attribut `lang` de la racine du document est mis à jour. Ce n'est pas
 * cosmétique : il détermine la langue annoncée par les lecteurs d'écran, les
 * règles de césure appliquées par le navigateur, et la langue déclarée aux
 * moteurs de recherche. Une page anglaise servie sous `lang="fr"` est lue avec
 * l'accent français par une synthèse vocale.</p>
 *
 * <h2>Sur le stockage</h2>
 *
 * <p>La préférence de langue relève du stockage strictement nécessaire au sens
 * de l'article 129 de la loi du 13 juin 2005 : elle exécute une demande
 * explicite de la personne, exactement comme le choix de thème. Elle n'est donc
 * pas soumise au consentement — et elle est inscrite à l'inventaire de la
 * politique de cookies, parce qu'être exempté ne dispense pas de déclarer.</p>
 */

const CLE = "coshift_langue";

interface Valeur {
  langue: Langue;
  definir: (l: Langue) => void;
  /** Traduit une clé pointée, avec interpolation optionnelle. */
  t: (chemin: string, valeurs?: Record<string, string | number>) => string;
}

const LangContext = createContext<Valeur | undefined>(undefined);

function langueInitiale(): Langue {
  try {
    const conserve = localStorage.getItem(CLE);
    if (estUneLangue(conserve)) return conserve;
  } catch {
    /* Stockage indisponible — navigation privée verrouillée. On se rabat sur
       le navigateur, ce qui reste un choix raisonnable. */
  }
  return langueDuNavigateur();
}

export function LangProvider({ children }: { children: ReactNode }) {
  const [langue, setLangue] = useState<Langue>(langueInitiale);

  const definir = useCallback((l: Langue) => {
    setLangue(l);
    try {
      localStorage.setItem(CLE, l);
    } catch {
      /* Le choix vaut pour la session même s'il ne peut pas être conservé. */
    }
  }, []);

  useEffect(() => {
    document.documentElement.lang = LANGUES[langue].balise;
  }, [langue]);

  /* Le choix se propage entre onglets : changer de langue dans l'un ne doit
     pas laisser les autres dans l'ancienne. */
  useEffect(() => {
    const surStockage = (e: StorageEvent) => {
      if (e.key === CLE && estUneLangue(e.newValue)) setLangue(e.newValue);
    };
    window.addEventListener("storage", surStockage);
    return () => window.removeEventListener("storage", surStockage);
  }, []);

  const valeur = useMemo<Valeur>(
    () => ({
      langue,
      definir,
      t: (chemin, valeurs) => traduire(langue, chemin, valeurs),
    }),
    [langue, definir],
  );

  return <LangContext.Provider value={valeur}>{children}</LangContext.Provider>;
}

export function useLang(): Valeur {
  const ctx = useContext(LangContext);
  if (!ctx) throw new Error("useLang doit être utilisé dans un LangProvider");
  return ctx;
}

/** Raccourci pour les composants qui n'ont besoin que de traduire. */
export function useT() {
  return useLang().t;
}
