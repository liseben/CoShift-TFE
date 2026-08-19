import { useId, type TextareaHTMLAttributes } from "react";
import "../field.css";

type TextareaProps = Omit<TextareaHTMLAttributes<HTMLTextAreaElement>, "id"> & {
  label: string;
  error?: string;
  hint?: string;
  /** Affiche un compteur `n / max`. Requiert `maxLength`. */
  showCount?: boolean;
  id?: string;
};

/** Zone de texte multiligne. Redimensionnable en hauteur uniquement. */
export default function Textarea({
  label,
  error,
  hint,
  showCount,
  id,
  required,
  maxLength,
  value,
  ...rest
}: TextareaProps) {
  const auto = useId();
  const areaId = id ?? auto;
  const errorId = `${areaId}-error`;
  const hintId = `${areaId}-hint`;
  const count = typeof value === "string" ? value.length : 0;

  return (
    <div className={`field ${error ? "field--invalid" : ""}`.trim()}>
      <label className="field__label" htmlFor={areaId}>
        {label}
        {required && (
          <span className="field__required">
            *<span className="sr-only"> obligatoire</span>
          </span>
        )}
      </label>

      <textarea
        id={areaId}
        className="field__control field__control--textarea"
        required={required}
        maxLength={maxLength}
        value={value}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : hint ? hintId : undefined}
        {...rest}
      />

      {error ? (
        <p className="field__error" id={errorId} role="alert">
          {error}
        </p>
      ) : (
        <p className="field__hint" id={hintId}>
          {hint}
          {showCount && maxLength && (
            <span aria-live="polite">
              {hint ? " — " : ""}
              {count} / {maxLength}
            </span>
          )}
        </p>
      )}
    </div>
  );
}
