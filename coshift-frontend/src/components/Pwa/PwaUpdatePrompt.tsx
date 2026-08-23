import { useRegisterSW } from "virtual:pwa-register/react";
import { Button } from "../ui";
import { useT } from "../../context/LangContext";
import "./PwaUpdatePrompt.css";

/**
 * Annonce qu'une nouvelle version de l'application attend d'être installée.
 *
 * <h2>Pourquoi une annonce plutôt qu'une mise à jour silencieuse</h2>
 *
 * <p>Le service worker sait remplacer l'application dès la navigation
 * suivante. Il ne le fait pas ici. Une mise à jour automatique reprendrait la
 * page au milieu d'un formulaire de publication à moitié rempli, et le travail
 * en cours serait perdu sans que personne comprenne pourquoi.</p>
 *
 * <p>C'est aussi la réponse au risque qui avait fait repousser ce chantier à la
 * fin : une application mise en cache sert des versions périmées. Ici, une
 * version périmée se signale — le bandeau reste tant qu'on ne l'a pas
 * rechargée, et il ne réapparaîtra pas une fois la version courante servie.</p>
 *
 * <h2>Ce qui n'est pas annoncé</h2>
 *
 * <p>`onOfflineReady` n'affiche rien. « L'application est prête hors ligne »
 * est un message qui rassure à tort : la coquille est en cache, mais chercher
 * un trajet ou réserver exige toujours le serveur. Annoncer une capacité qu'on
 * n'a pas vaut moins que de se taire.</p>
 */
export default function PwaUpdatePrompt() {
  const t = useT();
  const {
    needRefresh: [besoinDeRecharger, setBesoinDeRecharger],
    updateServiceWorker,
  } = useRegisterSW();

  if (!besoinDeRecharger) return null;

  return (
    /* `status` et non `alert` : rien n'est cassé, et interrompre un lecteur
       d'écran pour annoncer une mise à jour disponible serait disproportionné. */
    <div className="pwa-maj" role="status">
      <p className="pwa-maj__texte">{t("pwa.majDisponible")}</p>
      <div className="pwa-maj__actions">
        <Button size="sm" onClick={() => updateServiceWorker(true)}>
          {t("pwa.majRecharger")}
        </Button>
        <Button size="sm" variant="ghost" onClick={() => setBesoinDeRecharger(false)}>
          {t("pwa.majPlusTard")}
        </Button>
      </div>
    </div>
  );
}
