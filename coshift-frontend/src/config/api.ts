/**
 * Point d'entrée unique vers l'API CoShift.
 *
 * L'URL était auparavant recopiée à l'identique dans huit fichiers, ce qui
 * imposait huit modifications au moindre changement de port. Elle est
 * désormais définie ici et surchargeable via `VITE_API_URL` dans le `.env`.
 */
export const API_BASE: string =
  import.meta.env.VITE_API_URL ?? "http://localhost:8080";
