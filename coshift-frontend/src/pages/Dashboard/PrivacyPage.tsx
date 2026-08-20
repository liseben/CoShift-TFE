import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import axios from "axios";
import { FiDownload, FiTrash2, FiShield, FiFileText } from "react-icons/fi";
import { Alert, Button, Card, Input, Modal } from "../../components/ui";
import { useAuth } from "../../context/AuthContext";
import { useConsent } from "../../context/ConsentContext";
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

      setSucces("Votre export a été téléchargé au format JSON.");
    } catch {
      setErreur("L'export n'a pas pu être produit. Réessayez dans un instant.");
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
      setErreur(message ?? "La suppression n'a pas abouti. Réessayez dans un instant.");
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
            <h2 className="pv__titre">Récupérer mes données</h2>
            <p className="pv__ref">Articles 15 et 20 du RGPD</p>
          </div>
        </div>

        <p className="pv__texte">
          Téléchargez tout ce que CoShift détient sur vous : votre compte, vos
          organisations, vos véhicules, les trajets que vous avez proposés et
          les réservations que vous avez demandées. Le fichier est au format
          JSON, lisible par une machine comme par un humain.
        </p>
        <p className="pv__texte pv__texte--sourdine">
          Les données des autres membres en sont exclues. Un trajet réservé chez
          quelqu'un apparaît avec son itinéraire et son horaire, jamais avec son
          téléphone : ce sont ses données, pas les vôtres.
        </p>

        <Button onClick={exporter} loading={exportEnCours} icon={<FiDownload />}>
          Exporter mes données
        </Button>
      </Card>

      {/* ── Consentement ── */}
      <Card padding="lg">
        <div className="pv__tete">
          <span className="pv__icone" aria-hidden="true"><FiShield /></span>
          <div>
            <h2 className="pv__titre">Mes services tiers</h2>
            <p className="pv__ref">Article 7.3 du RGPD</p>
          </div>
        </div>

        {choix ? (
          <ul className="pv__liste">
            <li>
              Carte animée — Mapbox :{" "}
              <strong>{choix.mapbox ? "autorisée" : "refusée"}</strong>
            </li>
            <li>
              Connexion Google :{" "}
              <strong>{choix.google ? "autorisée" : "refusée"}</strong>
            </li>
            <li className="pv__texte--sourdine">
              Choix exprimé le{" "}
              {new Date(choix.date).toLocaleDateString("fr-BE", {
                day: "numeric", month: "long", year: "numeric",
              })}
              , sur la version {choix.version} de la politique de confidentialité.
            </li>
          </ul>
        ) : (
          <p className="pv__texte">
            Vous n'avez pas encore répondu au bandeau. Aucun service tiers n'est
            chargé.
          </p>
        )}

        <Button variant="secondary" onClick={reinitialiser}>
          Revoir mon choix
        </Button>
      </Card>

      {/* ── Documents ── */}
      <Card padding="lg">
        <div className="pv__tete">
          <span className="pv__icone" aria-hidden="true"><FiFileText /></span>
          <div>
            <h2 className="pv__titre">Ce à quoi vous avez souscrit</h2>
            <p className="pv__ref">Article 13 du RGPD</p>
          </div>
        </div>
        <ul className="pv__liste">
          <li><Link to="/confidentialite">Politique de confidentialité</Link> — ce qui est collecté, pourquoi, et pour combien de temps</li>
          <li><Link to="/cgu">Conditions générales</Link> — les engagements de chacun</li>
          <li><Link to="/cookies">Cookies et traceurs</Link> — ce qui est stocké dans votre navigateur</li>
          <li><Link to="/mentions-legales">Mentions légales</Link> — qui édite le service</li>
        </ul>
      </Card>

      {/* ── Effacement ── */}
      {/* `tone` pose la bande de couleur prévue par le socle : la zone
          destructrice se distingue avant d'être lue. */}
      <Card padding="lg" tone="danger">
        <div className="pv__tete">
          <span className="pv__icone pv__icone--danger" aria-hidden="true"><FiTrash2 /></span>
          <div>
            <h2 className="pv__titre">Supprimer mon compte</h2>
            <p className="pv__ref">Article 17 du RGPD</p>
          </div>
        </div>

        <p className="pv__texte">
          <strong>Cette action est irréversible.</strong> Votre nom, votre
          adresse, votre téléphone, votre photographie et vos plaques
          d'immatriculation sont effacés immédiatement, sans copie de
          sauvegarde.
        </p>
        <p className="pv__texte">
          Vos trajets et réservations passés sont anonymisés plutôt que
          supprimés : ils engagent d'autres membres, dont l'historique ne peut
          pas être détruit par votre demande. Une fois détachés de vous, ils ne
          désignent plus personne.
        </p>
        <p className="pv__texte pv__texte--sourdine">
          Vos trajets à venir et vos réservations en cours sont annulés, avec un
          motif explicite, pour que personne ne se présente à un rendez-vous qui
          n'aura pas lieu.
        </p>

        <Button variant="danger" onClick={() => setSuppressionOuverte(true)} icon={<FiTrash2 />}>
          Supprimer définitivement mon compte
        </Button>
      </Card>

      <Modal
        open={suppressionOuverte}
        onClose={() => { setSuppressionOuverte(false); setConfirmation(""); }}
        title="Confirmer la suppression"
        footer={
          <>
            <Button variant="ghost"
                    onClick={() => { setSuppressionOuverte(false); setConfirmation(""); }}>
              Annuler
            </Button>
            <Button variant="danger"
                    disabled={!confirmationValide}
                    loading={suppressionEnCours}
                    onClick={supprimer}>
              Supprimer mon compte
            </Button>
          </>
        }
      >
        <Alert tone="warning" title="Aucun retour en arrière">
          Une fois l'opération lancée, ni vous ni CoShift ne pourrez récupérer
          votre compte. Pensez à exporter vos données auparavant si vous
          souhaitez les conserver.
        </Alert>

        <Input
          label="Retapez votre adresse électronique pour confirmer"
          type="email"
          autoComplete="off"
          value={confirmation}
          onChange={(e) => setConfirmation(e.target.value)}
          placeholder={user?.email}
          hint="Le serveur exige la même confirmation : sans elle, la requête est refusée."
        />
      </Modal>
    </div>
  );
}
