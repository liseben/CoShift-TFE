import type { ReactNode } from "react";
import "./EmptyState.css";

type Props = {
  /** Icone decorative. Masquee aux lecteurs d'ecran. */
  icon?: ReactNode;
  title: string;
  description?: string;
  /** Action de sortie : un ecran vide doit toujours proposer une suite. */
  action?: ReactNode;
};

/**
 * Etat vide.
 *
 * Une liste vide sans explication ressemble a un bug. On dit ce qui manque
 * et on propose l'action qui remplit l'ecran.
 */
export default function EmptyState({ icon, title, description, action }: Props) {
  return (
    <div className="empty">
      {icon && (
        <span className="empty__icon" aria-hidden="true">
          {icon}
        </span>
      )}
      <p className="empty__title">{title}</p>
      {description && <p className="empty__desc">{description}</p>}
      {action && <div className="empty__action">{action}</div>}
    </div>
  );
}
