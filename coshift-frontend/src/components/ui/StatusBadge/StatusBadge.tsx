import type { ReactNode } from "react";
import { useT } from "../../../context/LangContext";
import "./StatusBadge.css";

/**
 * Statuts recopies a l'identique des enums backend :
 * BookingStatus (PENDING, CONFIRMED, CANCELLED, REJECTED, COMPLETED)
 * et TripStatus (PLANNED, FULL, COMPLETED, CANCELLED).
 */
export type Status =
  | "PENDING"
  | "CONFIRMED"
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
/* Le libelle a quitte cette table : il vient du catalogue de traduction, la
   couleur reste ici. Un statut porte une information de sens (le ton) et une
   information de langue (le mot) ; les melanger obligeait a traduire une
   constante de module, hors de portee d'un hook. */
const TON: Record<Status, Tone> = {
  PENDING:   "pending",
  CONFIRMED: "eco",
  REJECTED:  "danger",
  CANCELLED: "danger",
  COMPLETED: "neutral",
  PLANNED:   "brand",
  FULL:      "danger",
};

type Props = {
  status: Status;
  /** Remplace le libelle par defaut. */
  children?: ReactNode;
  size?: "sm" | "md";
};

export default function StatusBadge({ status, children, size = "md" }: Props) {
  const t = useT();
  const entry = {
    label: TON[status] ? t(`statuts.${status}`) : status,
    tone: TON[status] ?? ("neutral" as Tone),
  };

  return (
    <span className={`badge badge--${entry.tone} badge--${size}`}>
      <span className="badge__dot" aria-hidden="true" />
      {children ?? entry.label}
    </span>
  );
}
