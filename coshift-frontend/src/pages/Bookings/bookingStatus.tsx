/**
 * Formatage des dates de trajet, partagé par les écrans de réservation.
 *
 * Le vocabulaire des statuts vivait ici ; il a rejoint le composant
 * StatusBadge, qui est désormais seul à traduire BookingStatus et
 * TripStatus en libellés et en couleurs.
 */
export function formatTripDate(iso: string): string {
  const d = new Date(iso);
  return (
    d.toLocaleDateString("fr-FR", { weekday: "short", day: "numeric", month: "long" }) +
    " à " +
    d.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" })
  );
}
