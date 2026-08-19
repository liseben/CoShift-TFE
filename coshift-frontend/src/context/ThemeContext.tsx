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
export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useState<Theme>(
    () => (document.documentElement.dataset.theme as Theme) ?? "light",
  );
  const [followsSystem, setFollowsSystem] = useState(() => readStored() === null);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
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
