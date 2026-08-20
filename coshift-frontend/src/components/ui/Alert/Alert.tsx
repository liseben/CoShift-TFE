import type { ReactNode } from "react";
import { useT } from "../../../context/LangContext";
import "./Alert.css";

type Props = {
  tone?: "info" | "success" | "warning" | "danger";
  title?: string;
  children: ReactNode;
  /** Affiche une croix de fermeture. */
  onDismiss?: () => void;
};

/* Le libellé textuel du ton : la couleur seule ne porte jamais l'information.
   La table ne garde que la clé — un libellé écrit ici serait figé au
   chargement du module, hors de portée du contexte de langue. */
const LABEL = {
  info: "message.info",
  success: "message.success",
  warning: "message.warning",
  danger: "message.danger",
} as const;

/**
 * Message contextuel.
 *
 * Les tons `danger` et `warning` prennent `role="alert"` : ils
 * interrompent le lecteur d'ecran, ce qui se justifie pour une erreur.
 * `info` et `success` restent en `status`, annonces sans interruption.
 */
export default function Alert({ tone = "info", title, children, onDismiss }: Props) {
  const t = useT();
  const assertive = tone === "danger" || tone === "warning";

  return (
    <div className={`alert alert--${tone}`} role={assertive ? "alert" : "status"}>
      <div className="alert__body">
        <p className="alert__title">
          <span className="sr-only">{t(LABEL[tone])} : </span>
          {title ?? t(LABEL[tone])}
        </p>
        <div className="alert__content">{children}</div>
      </div>

      {onDismiss && (
        <button
          type="button"
          className="alert__close is-inline"
          onClick={onDismiss}
          aria-label={t("message.fermer")}
        >
          ×
        </button>
      )}
    </div>
  );
}
