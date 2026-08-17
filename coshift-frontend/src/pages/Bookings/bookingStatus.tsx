/**
 * Vocabulaire partagé des statuts de réservation.
 *
 * Les libellés sont volontairement écrits du point de vue de l'utilisateur —
 * « En attente du conducteur » plutôt que « PENDING » — et centralisés ici pour
 * que les deux écrans (passager et conducteur) racontent la même histoire.
 */
export type BookingStatus =
  | "PENDING"
  | "CONFIRMED"
  | "CANCELLED"
  | "REJECTED"
  | "COMPLETED";

export const STATUS_LABELS: Record<BookingStatus, { label: string; tone: string }> = {
  PENDING:   { label: "En attente du conducteur", tone: "wait" },
  CONFIRMED: { label: "Confirmée",                tone: "ok" },
  CANCELLED: { label: "Annulée",                  tone: "off" },
  REJECTED:  { label: "Refusée",                  tone: "bad" },
  COMPLETED: { label: "Terminée",                 tone: "done" },
};

export function statusOf(status: string) {
  return STATUS_LABELS[status as BookingStatus] ?? { label: status, tone: "off" };
}

export function formatTripDate(iso: string): string {
  const d = new Date(iso);
  return (
    d.toLocaleDateString("fr-FR", { weekday: "short", day: "numeric", month: "long" }) +
    " à " +
    d.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" })
  );
}
