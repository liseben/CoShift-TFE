import { createContext, useContext, useEffect, useState, type ReactNode } from "react";

export type Theme = "light" | "dark";

const STORAGE_KEY = "coshift_theme";

type ThemeContextValue = {
  theme: Theme;
  toggle: () => void;
  /** Vrai tant que l'utilisateur n'a pas choisi : on suit alors le système. */
  followsSystem: boolean;
};

const ThemeContext = createContext<ThemeContextValue | null>(null);

function readStored(): Theme | null {
  const v = localStorage.getItem(STORAGE_KEY);
  return v === "light" || v === "dark" ? v : null;
}

function systemTheme(): Theme {
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

/**
 * Thème clair ou sombre.
 *
 * Le thème est écrit dans `data-theme` sur <html>. Un script placé en tête
 * d'index.html fait déjà ce travail avant le premier rendu ; ce contexte
 * reprend simplement la valeur en place, ce qui évite que la page
 * apparaisse en clair une fraction de seconde avant de basculer.
 *
 * Tant que l'utilisateur n'a rien choisi, on suit sa préférence système et
 * on réagit à ses changements — quelqu'un dont le téléphone bascule en
 * sombre le soir n'a pas à venir le redire ici.
 */
/* Fonds de page des deux thèmes, repris de tokens.css. Deux valeurs écrites
   ici plutôt que lues dans les jetons : `getComputedStyle` sur `<html>` ne
   renverrait rien tant que la feuille n'est pas chargée, et la barre système
   ne supporte pas d'être mise à jour une frame trop tard. */
const BARRE_CLAIRE = "#F7F9FC";
const BARRE_SOMBRE = "#0F1620";

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useState<Theme>(
    () => (document.documentElement.dataset.theme as Theme) ?? "light",
  );
  const [followsSystem, setFollowsSystem] = useState(() => readStored() === null);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;

    /* Couleur de la barre système, en mode installé sur l'écran d'accueil.
       index.html en pose deux, choisies par requête média : elles suivent la
       préférence du système, ce qui suffit avant le premier rendu mais devient
       faux dès que quelqu'un choisit explicitement l'autre thème. On écrit
       donc une balise sans requête média, qui l'emporte sur les deux autres.

       Sans cela, une personne qui passe CoShift en sombre sur un téléphone
       réglé en clair garde une barre blanche au-dessus d'une application
       noire. */
    let balise = document.head.querySelector<HTMLMetaElement>(
      'meta[name="theme-color"]:not([media])',
    );
    if (!balise) {
      balise = document.createElement("meta");
      balise.name = "theme-color";
      document.head.appendChild(balise);
    }
    balise.content = theme === "dark" ? BARRE_SOMBRE : BARRE_CLAIRE;
  }, [theme]);

  useEffect(() => {
    if (!followsSystem) return;
    const mq = window.matchMedia("(prefers-color-scheme: dark)");
    const onChange = () => setTheme(systemTheme());
    mq.addEventListener("change", onChange);
    return () => mq.removeEventListener("change", onChange);
  }, [followsSystem]);

  const toggle = () => {
    const next: Theme = theme === "dark" ? "light" : "dark";
    setTheme(next);
    setFollowsSystem(false);
    localStorage.setItem(STORAGE_KEY, next);
  };

  return (
    <ThemeContext.Provider value={{ theme, toggle, followsSystem }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error("useTheme doit être utilisé dans un ThemeProvider.");
  return ctx;
}
