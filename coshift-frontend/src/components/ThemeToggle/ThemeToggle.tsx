import { FiSun, FiMoon } from "react-icons/fi";
import { useTheme } from "../../context/ThemeContext";
import "./ThemeToggle.css";

/**
 * Bascule clair / sombre.
 *
 * Rendue en `role="switch"` avec `aria-checked` : un lecteur d'écran
 * annonce alors l'état courant, ce qu'un simple bouton ne fait pas. Le
 * curseur glisse d'un côté à l'autre, et les deux icônes restent
 * visibles pour que la destination du clic soit lisible.
 */
export default function ThemeToggle() {
  const { theme, toggle } = useTheme();
  const dark = theme === "dark";

  return (
    <button
      type="button"
      role="switch"
      aria-checked={dark}
      className={`theme-toggle ${dark ? "is-dark" : ""}`}
      onClick={toggle}
      title={dark ? "Passer en mode clair" : "Passer en mode sombre"}
    >
      <span className="sr-only">Mode sombre</span>
      <span className="theme-toggle__track" aria-hidden="true">
        <FiSun className="theme-toggle__icon theme-toggle__icon--sun" />
        <FiMoon className="theme-toggle__icon theme-toggle__icon--moon" />
        <span className="theme-toggle__knob" />
      </span>
    </button>
  );
}
