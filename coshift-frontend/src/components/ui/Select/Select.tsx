import { useId, type SelectHTMLAttributes, type ReactNode } from "react";
import "../field.css";

type Option = { value: string; label: string };

type SelectProps = Omit<SelectHTMLAttributes<HTMLSelectElement>, "id"> & {
  label: string;
  /** Liste d'options. Ignoree si `children` est fourni. */
  options?: Option[];
  /** Entree neutre en tete de liste. */
  placeholder?: string;
  error?: string;
  hint?: string;
  id?: string;
  children?: ReactNode;
};

/** Liste deroulante, meme habillage et memes regles que les autres champs. */
export default function Select({
  label,
  options,
  placeholder,
  error,
  hint,
  id,
  required,
  children,
  ...rest
}: SelectProps) {
  const auto = useId();
  const selectId = id ?? auto;
  const errorId = `${selectId}-error`;
  const hintId = `${selectId}-hint`;

  return (
    <div className={`field ${error ? "field--invalid" : ""}`.trim()}>
      <label className="field__label" htmlFor={selectId}>
        {label}
        {required && (
          <span className="field__required">
            *<span className="sr-only"> obligatoire</span>
          </span>
        )}
      </label>

      <select
        id={selectId}
        className="field__control field__control--select"
        required={required}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : hint ? hintId : undefined}
        {...rest}
      >
        {placeholder && (
          <option value="" disabled>
            {placeholder}
          </option>
        )}
        {children ??
          options?.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
      </select>

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
