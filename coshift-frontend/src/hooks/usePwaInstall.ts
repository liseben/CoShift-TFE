import { useCallback, useEffect, useState } from "react";

/**
 * Événement propre à Chromium, absent de la bibliothèque standard du DOM.
 *
 * <p>Il n'est pas normalisé : Safari et Firefox ne l'émettent pas, et rien ne
 * garantit qu'ils le feront. C'est la raison d'être du repli manuel plus
 * bas.</p>
 */
interface InviteInstallation extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: "accepted" | "dismissed" }>;
}

/** Vrai si la page tourne déjà dans une fenêtre installée. */
function dejaInstallee(): boolean {
  if (window.matchMedia("(display-mode: standalone)").matches) return true;
  /* Safari sur iOS ne renseigne pas `display-mode` et expose à la place ce
     drapeau, hors norme et absent des types du DOM. */
  return (window.navigator as Navigator & { standalone?: boolean }).standalone === true;
}

/**
 * Reconnaît iOS, y compris un iPad qui se déclare « Macintosh ».
 *
 * <p>Depuis iPadOS 13, Safari annonce un agent utilisateur de bureau. Le seul
 * indice fiable est la présence d'un écran tactile sur un « Macintosh ».</p>
 */
function estIos(): boolean {
  const ua = navigator.userAgent;
  if (/iPad|iPhone|iPod/.test(ua)) return true;
  return ua.includes("Macintosh") && navigator.maxTouchPoints > 1;
}

export type EtatInstallation =
  /** Déjà sur l'écran d'accueil : il n'y a plus rien à proposer. */
  | "installee"
  /** Le navigateur propose l'installation en un geste. */
  | "possible"
  /** iOS n'a pas d'invite : il faut expliquer le geste. */
  | "manuelle"
  /** Ni invite ni recette connue — on ne propose rien plutôt que d'échouer. */
  | "indisponible";

/**
 * État d'installation de l'application sur l'écran d'accueil.
 *
 * <h2>Pourquoi un état à quatre valeurs plutôt qu'un booléen</h2>
 *
 * <p>Le bouton « Téléchargez l'App » affichait jusqu'ici une alerte annonçant
 * que l'installation « sera proposée ici ». Le remplacer par un bouton qui
 * appelle {@code prompt()} ne suffirait pas : sur iOS l'invite n'existe pas et
 * l'appel n'aurait aucun effet, sur Firefox non plus. Un bouton qui ne fait
 * rien est pire que pas de bouton — il apprend qu'on ne peut pas se fier à
 * l'interface.</p>
 *
 * <p>Les quatre états permettent au menu de proposer l'installation là où elle
 * marche, d'expliquer le geste là où il faut le faire à la main, et de se
 * taire ailleurs.</p>
 */
export function usePwaInstall() {
  const [invite, setInvite] = useState<InviteInstallation | null>(null);
  const [installee, setInstallee] = useState(dejaInstallee);

  useEffect(() => {
    const capturer = (e: Event) => {
      /* Sans cela, Chrome affiche sa propre bannière au bas de l'écran. On la
         retient pour la déclencher depuis le bouton du menu, à un moment
         choisi par la personne plutôt que par le navigateur. */
      e.preventDefault();
      setInvite(e as InviteInstallation);
    };
    const installe = () => {
      setInstallee(true);
      setInvite(null);
    };

    window.addEventListener("beforeinstallprompt", capturer);
    window.addEventListener("appinstalled", installe);
    return () => {
      window.removeEventListener("beforeinstallprompt", capturer);
      window.removeEventListener("appinstalled", installe);
    };
  }, []);

  const etat: EtatInstallation = installee
    ? "installee"
    : invite
      ? "possible"
      : estIos()
        ? "manuelle"
        : "indisponible";

  /**
   * Déclenche l'invite du navigateur.
   *
   * <p>L'événement retenu n'est utilisable qu'une fois : après une réponse, il
   * est jeté. Un second appel ne donnerait rien, et le bouton doit disparaître
   * plutôt que rester à ne rien faire.</p>
   */
  const installer = useCallback(async () => {
    if (!invite) return;
    await invite.prompt();
    await invite.userChoice;
    setInvite(null);
  }, [invite]);

  return { etat, installer };
}
