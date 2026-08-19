import type { ReactNode, HTMLAttributes } from "react";
import { Link } from "react-router-dom";
import "./Card.css";

type CardProps = HTMLAttributes<HTMLDivElement> & {
  /** Titre de la carte. Rendu dans un <h3>. */
  title?: ReactNode;
  /** Contenu aligne a droite du titre : action, badge, prix. */
  action?: ReactNode;
  /** Bande de couleur a gauche, pour signaler un statut. */
  tone?: "brand" | "eco" | "pending" | "danger";
  /** Rend la carte entierement cliquable. */
  to?: string;
  padding?: "sm" | "md" | "lg";
  children: ReactNode;
};

/**
 * Carte.
 *
 * Quand `to` est fourni, toute la carte devient un lien — plus facile a
 * viser qu'un petit « voir plus », surtout au doigt.
 */
export default function Card({
  title,
  action,
  tone,
  to,
  padding = "md",
  children,
  className = "",
  ...rest
}: CardProps) {
  const classes = [
    "card",
    `card--pad-${padding}`,
    tone ? `card--${tone}` : "",
    to ? "card--link" : "",
    className,
  ]
    .filter(Boolean)
    .join(" ");

  const body = (
    <>
      {(title || action) && (
        <div className="card__head">
          {title && <h3 className="card__title">{title}</h3>}
          {action && <div className="card__action">{action}</div>}
        </div>
      )}
      {children}
    </>
  );

  if (to) {
    return (
      <Link to={to} className={classes}>
        {body}
      </Link>
    );
  }

  return (
    <div className={classes} {...rest}>
      {body}
    </div>
  );
}
