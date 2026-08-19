import type { ButtonHTMLAttributes, ReactNode } from "react";
import { Link } from "react-router-dom";
import "./Button.css";

type Variant = "primary" | "secondary" | "ghost" | "danger" | "eco";
type Size = "sm" | "md" | "lg";

type BaseProps = {
  variant?: Variant;
  size?: Size;
  /** Occupe toute la largeur disponible. */
  block?: boolean;
  /** Affiche un indicateur et neutralise le bouton. */
  loading?: boolean;
  /** Icone placee avant le libelle. Purement decorative. */
  icon?: ReactNode;
  children: ReactNode;
};

type ButtonProps = BaseProps &
  Omit<ButtonHTMLAttributes<HTMLButtonElement>, "children"> & { to?: never };

type LinkProps = BaseProps & {
  /** Rend un lien de navigation avec l'apparence d'un bouton. */
  to: string;
  disabled?: never;
};

/**
 * Bouton.
 *
 * `to` produit un lien, sans quoi un vrai <button>. La distinction compte :
 * un lien navigue et s'ouvre dans un nouvel onglet, un bouton agit.
 *
 * En chargement, le bouton reste dans le flux et conserve sa largeur — un
 * bouton qui retrecit pendant l'attente deplace tout ce qui l'entoure.
 */
export default function Button(props: ButtonProps | LinkProps) {
  const {
    variant = "primary",
    size = "md",
    block = false,
    loading = false,
    icon,
    children,
    className = "",
    ...rest
  } = props as BaseProps & { className?: string; [k: string]: unknown };

  const classes = [
    "btn",
    `btn--${variant}`,
    `btn--${size}`,
    block ? "btn--block" : "",
    loading ? "is-loading" : "",
    className,
  ]
    .filter(Boolean)
    .join(" ");

  const inner = (
    <>
      {loading && <span className="btn__spinner" aria-hidden="true" />}
      {!loading && icon && (
        <span className="btn__icon" aria-hidden="true">
          {icon}
        </span>
      )}
      <span className="btn__label">{children}</span>
    </>
  );

  if ("to" in props && props.to) {
    const { to, ...linkRest } = rest as { to: string };
    return (
      <Link to={to} className={classes} {...linkRest}>
        {inner}
      </Link>
    );
  }

  const buttonRest = rest as ButtonHTMLAttributes<HTMLButtonElement>;
  return (
    <button
      type={buttonRest.type ?? "button"}
      className={classes}
      disabled={buttonRest.disabled || loading}
      aria-busy={loading || undefined}
      {...buttonRest}
    >
      {inner}
    </button>
  );
}
