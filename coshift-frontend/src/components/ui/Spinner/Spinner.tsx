import { useT } from "../../../context/LangContext";
import "./Spinner.css";

type Props = {
  size?: "sm" | "md" | "lg";
  /** Texte annonce aux lecteurs d'ecran, et affiche si `showLabel`. */
  label?: string;
  showLabel?: boolean;
  /** Centre le bloc dans l'espace disponible. */
  center?: boolean;
};

/**
 * Indicateur de chargement.
 *
 * `role="status"` annonce l'attente sans voler le focus. Sans lui, un
 * lecteur d'ecran ne signale rien et l'utilisateur croit l'interface figee.
 */
export default function Spinner({
  size = "md",
  /* Le défaut n'est plus une chaîne mais une absence : un libellé écrit dans
     la signature serait figé en français. Il est résolu dans le corps, où le
     contexte de langue est accessible. */
  label,
  showLabel = false,
  center = false,
}: Props) {
  const t = useT();
  const libelle = label ?? t("commun.chargementEnCours");

  return (
    <div className={`spinner-block ${center ? "spinner-block--center" : ""}`.trim()} role="status">
      <span className={`spinner spinner--${size}`} aria-hidden="true" />
      <span className={showLabel ? "spinner-block__label" : "sr-only"}>{libelle}</span>
    </div>
  );
}
