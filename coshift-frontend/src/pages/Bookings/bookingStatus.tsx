/**
 * Formatage des dates de trajet, partagé par les écrans de réservation.
 *
 * Le vocabulaire des statuts vivait ici ; il a rejoint le composant
 * StatusBadge, qui est désormais seul à traduire BookingStatus et
 * TripStatus en libellés et en couleurs.
 */
export function formatTripDate(iso: string, balise: string, liaison: string): string {
  const d = new Date(iso);
  return (
    d.toLocaleDateString(balise, { weekday: "short", day: "numeric", month: "long" }) +
    liaison +
    d.toLocaleTimeString(balise, { hour: "2-digit", minute: "2-digit" })
  );
}
