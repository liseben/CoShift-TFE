import type { ReactNode } from "react";
import "./Alert.css";

type Props = {
  tone?: "info" | "success" | "warning" | "danger";
  title?: string;
  children: ReactNode;
  /** Affiche une croix de fermeture. */
  onDismiss?: () => void;
};

/** Libelle textuel du ton : la couleur seule ne porte jamais l'information. */
const LABEL = {
  info: "Information",
  success: "Succès",
  warning: "Attention",
  danger: "Erreur",
} as const;

/**
 * Message contextuel.
 *
 * Les tons `danger` et `warning` prennent `role="alert"` : ils
 * interrompent le lecteur d'ecran, ce qui se justifie pour une erreur.
 * `info` et `success` restent en `status`, annonces sans interruption.
 */
export default function Alert({ tone = "info", title, children, onDismiss }: Props) {
  const assertive = tone === "danger" || tone === "warning";

  return (
    <div className={`alert alert--${tone}`} role={assertive ? "alert" : "status"}>
      <div className="alert__body">
        <p className="alert__title">
          <span className="sr-only">{LABEL[tone]} : </span>
          {title ?? LABEL[tone]}
        </p>
        <div className="alert__content">{children}</div>
      </div>

      {onDismiss && (
        <button
          type="button"
          className="alert__close is-inline"
          onClick={onDismiss}
          aria-label="Fermer ce message"
        >
          ×
        </button>
      )}
    </div>
  );
}
