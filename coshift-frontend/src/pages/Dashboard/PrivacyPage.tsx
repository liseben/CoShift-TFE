import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import axios from "axios";
import { FiDownload, FiTrash2, FiShield, FiFileText } from "react-icons/fi";
import { Alert, Button, Card, Input, Modal } from "../../components/ui";
import { useAuth } from "../../context/AuthContext";
import { useConsent } from "../../context/ConsentContext";
import { useLang } from "../../context/LangContext";
import { LANGUES } from "../../i18n";
import { API_BASE } from "../../config/api";
import "./PrivacyPage.css";

/**
 * Exercice des droits reconnus par le RGPD, depuis le compte.
 *
 * <h2>Pourquoi ici plutôt que par courriel</h2>
 *
 * <p>L'article 12.2 impose au responsable de traitement de « faciliter
 * l'exercice des droits ». Un formulaire de contact qui ouvre une demande
 * traitée à la main dans le mois respecte la lettre du texte ; un bouton qui
 * agit immédiatement en respecte l'intention. La différence se mesure : le
 * premier suppose qu'on écrive, qu'on attende et qu'on relance, le second
 * coûte deux clics.</p>
 *
 * <h2>La confirmation avant l'effacement</h2>
 *
 * <p>Retaper son adresse n'est pas une friction ajoutée pour dissuader. C'est
 * la seule barrière entre un clic malencontreux et la destruction définitive
 * d'un historique — le serveur exige la même confirmation, et refuse la
 * requête sans elle.</p>
 */
export default function PrivacyPage() {
  const { user, logout } = useAuth();
  const { choix, reinitialiser } = useConsent();
  const { langue, t } = useLang();
  const navigate = useNavigate();

  const [exportEnCours, setExportEnCours] = useState(false);
  const [suppressionOuverte, setSuppressionOuverte] = useState(false);
  const [confirmation, setConfirmation] = useState("");
  const [suppressionEnCours, setSuppressionEnCours] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);
  const [succes, setSucces] = useState<string | null>(null);

  const entetes = {
    Authorization: `Bearer ${localStorage.getItem("coshift_token") ?? ""}`,
  };

  /**
   * Récupère l'export et le remet au navigateur.
   *
   * <p>Le fichier transite par un objet en mémoire plutôt que par un lien
   * direct vers l'API : l'adresse exige un jeton d'authentification, qu'une
   * simple balise d'ancrage ne transmettrait pas.</p>
   */
  const exporter = async () => {
    setErreur(null);
    setSucces(null);
    setExportEnCours(true);
    try {
      const { data } = await axios.get(`${API_BASE}/api/users/me/export`, { headers: entetes });

      const contenu = new Blob([JSON.stringify(data, null, 2)], {
        type: "application/json",
      });
      const url = URL.createObjectURL(contenu);
      const lien = document.createElement("a");
      lien.href = url;
      lien.download = "coshift-mes-donnees.json";
      lien.click();
      /* Sans révocation, l'objet reste en mémoire jusqu'au rechargement de la
         page — et il contient l'intégralité des données personnelles. */
      URL.revokeObjectURL(url);

      setSucces(t("donnees.exportReussi"));
    } catch {
      setErreur(t("donnees.exportEchoue"));
    } finally {
      setExportEnCours(false);
    }
  };

  const supprimer = async () => {
    setErreur(null);
    setSuppressionEnCours(true);
    try {
      await axios.delete(`${API_BASE}/api/users/me`, {
        headers: entetes,
        data: { confirmationEmail: confirmation },
      });
      logout();
      navigate("/", { replace: true });
    } catch (e) {
      const message = axios.isAxiosError(e)
        ? e.response?.data?.message
        : null;
      setErreur(message ?? t("donnees.suppressionEchouee"));
      setSuppressionEnCours(false);
    }
  };

  const confirmationValide =
    confirmation.trim().toLowerCase() === (user?.email ?? "").toLowerCase();

  return (
    <div className="pv stack-8">
      {erreur && <Alert tone="danger" onDismiss={() => setErreur(null)}>{erreur}</Alert>}
      {succes && <Alert tone="success" onDismiss={() => setSucces(null)}>{succes}</Alert>}

      {/* ── Accès et portabilité ── */}
      <Card padding="lg">
        <div className="pv__tete">
          <span className="pv__icone" aria-hidden="true"><FiDownload /></span>
          <div>
            <h2 className="pv__titre">{t("donnees.recuperer")}</h2>
            <p className="pv__ref">{t("donnees.recupererRef")}</p>
          </div>
        </div>

        <p className="pv__texte">{t("donnees.recupererTexte")}</p>
        <p className="pv__texte pv__texte--sourdine">{t("donnees.recupererNote")}</p>

        <Button onClick={exporter} loading={exportEnCours} icon={<FiDownload />}>
          {t("donnees.exporter")}
        </Button>
      </Card>

      {/* ── Consentement ── */}
      <Card padding="lg">
        <div className="pv__tete">
          <span className="pv__icone" aria-hidden="true"><FiShield /></span>
          <div>
            <h2 className="pv__titre">{t("donnees.tiers")}</h2>
            <p className="pv__ref">{t("donnees.tiersRef")}</p>
          </div>
        </div>

        {choix ? (
          <ul className="pv__liste">
            <li>
              {t("donnees.carteMapbox")}{" "}
              <strong>
                {choix.mapbox ? t("donnees.autorisee") : t("donnees.refusee")}
              </strong>
            </li>
            <li>
              {t("donnees.connexionGoogle")}{" "}
              <strong>
                {choix.google ? t("donnees.autorisee") : t("donnees.refusee")}
              </strong>
            </li>
            <li className="pv__texte--sourdine">
              {t("donnees.choixExprime", {
                date: new Date(choix.date).toLocaleDateString(
                  LANGUES[langue].balise,
                  { day: "numeric", month: "long", year: "numeric" },
                ),
                version: choix.version,
              })}
            </li>
          </ul>
        ) : (
          <p className="pv__texte">{t("donnees.pasEncoreRepondu")}</p>
        )}

        <Button variant="secondary" onClick={reinitialiser}>
          {t("donnees.revoirChoix")}
        </Button>
      </Card>

      {/* ── Documents ── */}
      <Card padding="lg">
        <div className="pv__tete">
          <span className="pv__icone" aria-hidden="true"><FiFileText /></span>
          <div>
            <h2 className="pv__titre">{t("donnees.documents")}</h2>
            <p className="pv__ref">{t("donnees.documentsRef")}</p>
          </div>
        </div>
        <ul className="pv__liste">
          <li>
            <Link to="/confidentialite">{t("pied.confidentialite")}</Link> —{" "}
            {t("donnees.docConfidentialite")}
          </li>
          <li>
            <Link to="/cgu">{t("pied.cgu")}</Link> — {t("donnees.docCgu")}
          </li>
          <li>
            <Link to="/cookies">{t("pied.cookies")}</Link> — {t("donnees.docCookies")}
          </li>
          <li>
            <Link to="/mentions-legales">{t("pied.mentions")}</Link> —{" "}
            {t("donnees.docMentions")}
          </li>
        </ul>
      </Card>

      {/* ── Effacement ── */}
      {/* `tone` pose la bande de couleur prévue par le socle : la zone
          destructrice se distingue avant d'être lue. */}
      <Card padding="lg" tone="danger">
        <div className="pv__tete">
          <span className="pv__icone pv__icone--danger" aria-hidden="true"><FiTrash2 /></span>
          <div>
            <h2 className="pv__titre">{t("donnees.supprimer")}</h2>
            <p className="pv__ref">{t("donnees.supprimerRef")}</p>
          </div>
        </div>

        <p className="pv__texte">
          <strong>{t("donnees.supprimerIrreversible")}</strong>{" "}
          {t("donnees.supprimerP1")}
        </p>
        <p className="pv__texte">{t("donnees.supprimerP2")}</p>
        <p className="pv__texte pv__texte--sourdine">{t("donnees.supprimerP3")}</p>

        <Button variant="danger" onClick={() => setSuppressionOuverte(true)} icon={<FiTrash2 />}>
          {t("donnees.supprimerBouton")}
        </Button>
      </Card>

      <Modal
        open={suppressionOuverte}
        onClose={() => { setSuppressionOuverte(false); setConfirmation(""); }}
        title={t("donnees.confirmerTitre")}
        footer={
          <>
            <Button variant="ghost"
                    onClick={() => { setSuppressionOuverte(false); setConfirmation(""); }}>
              {t("commun.annuler")}
            </Button>
            <Button variant="danger"
                    disabled={!confirmationValide}
                    loading={suppressionEnCours}
                    onClick={supprimer}>
              {t("donnees.supprimerConfirmation")}
            </Button>
          </>
        }
      >
        <Alert tone="warning" title={t("donnees.aucunRetour")}>
          {t("donnees.aucunRetourTexte")}
        </Alert>

        <Input
          label={t("donnees.retapezAdresse")}
          type="email"
          autoComplete="off"
          value={confirmation}
          onChange={(e) => setConfirmation(e.target.value)}
          placeholder={user?.email}
          hint={t("donnees.retapezAide")}
        />
      </Modal>
    </div>
  );
}
