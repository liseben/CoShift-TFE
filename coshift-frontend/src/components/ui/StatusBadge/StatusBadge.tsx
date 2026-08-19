import type { ReactNode } from "react";
import "./StatusBadge.css";

/** Statuts issus de BookingStatus et TripStatus cote backend. */
export type Status =
  | "PENDING"
  | "ACCEPTED"
  | "REJECTED"
  | "CANCELLED"
  | "COMPLETED"
  | "PLANNED"
  | "FULL";

type Tone = "brand" | "eco" | "pending" | "danger" | "neutral";

/**
 * Chaque statut porte un libelle ET une couleur porteuse de sens.
 * La couleur ne suffit jamais seule : le mot est toujours affiche, sans
 * quoi l'information disparait pour un daltonien (WCAG 1.4.1).
 */
const MAP: Record<Status, { label: string; tone: Tone }> = {
  PENDING:   { label: "En attente", tone: "pending" },
  ACCEPTED:  { label: "Acceptée",   tone: "eco" },
  REJECTED:  { label: "Refusée",    tone: "danger" },
  CANCELLED: { label: "Annulée",    tone: "danger" },
  COMPLETED: { label: "Terminé",    tone: "neutral" },
  PLANNED:   { label: "À venir",    tone: "brand" },
  FULL:      { label: "Complet",    tone: "danger" },
};

type Props = {
  status: Status;
  /** Remplace le libelle par defaut. */
  children?: ReactNode;
  size?: "sm" | "md";
};

export default function StatusBadge({ status, children, size = "md" }: Props) {
  const entry = MAP[status] ?? { label: status, tone: "neutral" as Tone };

  return (
    <span className={`badge badge--${entry.tone} badge--${size}`}>
      <span className="badge__dot" aria-hidden="true" />
      {children ?? entry.label}
    </span>
  );
}
