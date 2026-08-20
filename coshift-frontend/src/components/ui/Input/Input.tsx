import { useId, type InputHTMLAttributes } from "react";
import { useT } from "../../../context/LangContext";
import "../field.css";

type InputProps = Omit<InputHTMLAttributes<HTMLInputElement>, "id"> & {
  label: string;
  /** Message d'erreur. Sa presence marque le champ comme invalide. */
  error?: string;
  /** Texte d'aide affiche sous le champ tant qu'il n'y a pas d'erreur. */
  hint?: string;
  id?: string;
};

/**
 * Champ de saisie.
 *
 * Le libelle est obligatoire et toujours visible : un placeholder seul
 * disparait des que l'utilisateur commence a taper, et n'est pas fiable
 * pour les lecteurs d'ecran.
 */
export default function Input({ label, error, hint, id, required, ...rest }: InputProps) {
  const t = useT();
  const auto = useId();
  const inputId = id ?? auto;
  const errorId = `${inputId}-error`;
  const hintId = `${inputId}-hint`;

  return (
    <div className={`field ${error ? "field--invalid" : ""}`.trim()}>
      <label className="field__label" htmlFor={inputId}>
        {label}
        {required && (
          <span className="field__required">
            *<span className="sr-only"> {t("champ.obligatoire")}</span>
          </span>
        )}
      </label>

      <input
        id={inputId}
        className="field__control"
        required={required}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : hint ? hintId : undefined}
        {...rest}
      />

      {error ? (
        <p className="field__error" id={errorId} role="alert">
          {error}
        </p>
      ) : hint ? (
        <p className="field__hint" id={hintId}>
          {hint}
        </p>
      ) : null}
    </div>
  );
}
