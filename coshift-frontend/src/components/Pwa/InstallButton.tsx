import { useState } from "react";
import { FiDownload, FiPlusSquare, FiShare } from "react-icons/fi";
import { Modal } from "../ui";
import { usePwaInstall } from "../../hooks/usePwaInstall";
import { useT } from "../../context/LangContext";
import "./InstallButton.css";

/**
 * Bouton « Installer l'application » de l'en-tête.
 *
 * <h2>Ce qu'il remplace</h2>
 *
 * <p>Un bouton « Téléchargez l'App » qui affichait une alerte : « l'installation
 * de l'application mobile sera proposée ici ». Il promettait quelque chose au
 * futur depuis assez longtemps pour que la promesse compte comme une dette.</p>
 *
 * <h2>Trois comportements, parce qu'il y a trois situations</h2>
 *
 * <ul>
 *   <li>Le navigateur sait installer : un clic, et c'est son invite qui
 *       s'ouvre.</li>
 *   <li>iOS ne connaît pas cette invite : le geste existe mais il est manuel,
 *       et il est introuvable pour qui ne le connaît pas déjà. On l'explique.</li>
 *   <li>Ni l'un ni l'autre — Firefox de bureau, par exemple : le bouton
 *       disparaît. Un bouton qui ne fait rien apprend qu'on ne peut pas se
 *       fier à l'interface ; mieux vaut ne rien proposer.</li>
 * </ul>
 *
 * <p>Une fois l'application installée, il disparaît également : elle est déjà
 * là, et la fenêtre dans laquelle on lit est justement la sienne.</p>
 */
export default function InstallButton({ onDone }: { onDone?: () => void }) {
  const t = useT();
  const { etat, installer } = usePwaInstall();
  const [expliquer, setExpliquer] = useState(false);

  if (etat === "installee" || etat === "indisponible") return null;

  const cliquer = () => {
    if (etat === "possible") {
      void installer().then(() => onDone?.());
      return;
    }
    setExpliquer(true);
  };

  return (
    <>
      <button className="btn-download-app" onClick={cliquer}>
        <FiDownload aria-hidden="true" />
        {t("pwa.installer")}
      </button>

      <Modal
        open={expliquer}
        onClose={() => setExpliquer(false)}
        title={t("pwa.iosTitre")}
        size="sm"
      >
        <p className="pwa-ios__intro">{t("pwa.iosIntro")}</p>

        {/* Une liste ordonnée : ce sont des étapes, et leur ordre compte. */}
        <ol className="pwa-ios__etapes">
          <li>
            <span className="pwa-ios__icone" aria-hidden="true"><FiShare /></span>
            {t("pwa.iosEtape1")}
          </li>
          <li>
            <span className="pwa-ios__icone" aria-hidden="true"><FiPlusSquare /></span>
            {t("pwa.iosEtape2")}
          </li>
          <li>
            <span className="pwa-ios__icone" aria-hidden="true" />
            {t("pwa.iosEtape3")}
          </li>
        </ol>

        <p className="pwa-ios__note">{t("pwa.iosNote")}</p>
      </Modal>
    </>
  );
}
