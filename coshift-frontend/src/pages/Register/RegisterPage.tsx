import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import axios from "axios";
import { API_BASE } from "../../config/api";
import { useT } from "../../context/LangContext";
import { Alert, Button, Input } from "../../components/ui";
import "../Auth/auth.css";
import "./RegisterPage.css";

export default function RegisterPage() {
  const t = useT();
  const navigate = useNavigate();

  const [firstname, setFirstname] = useState("");
  const [lastname, setLastname] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [show, setShow] = useState(false);
  /* Faux au départ : une case pré-cochée ne vaut pas acceptation. */
  const [acceptedTerms, setAcceptedTerms] = useState(false);

  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  /* Validation par champ : l'erreur s'affiche sous le champ fautif plutot
     qu'en un bloc unique en haut du formulaire. */
  const passwordError =
    password.length > 0 && password.length < 6
      ? t("inscription.motDePasseCourt")
      : undefined;
  const confirmError =
    confirm.length > 0 && confirm !== password
      ? t("inscription.confirmerDifferent")
      : undefined;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (passwordError || confirmError || password !== confirm) {
      setError(t("inscription.corrigerChamps"));
      return;
    }
    if (!acceptedTerms) {
      setError(t("inscription.accepterObligatoire"));
      return;
    }

    setLoading(true);
    setError(null);
    try {
      await axios.post(`${API_BASE}/api/auth/register`, {
        firstname,
        lastname,
        email,
        password,
        /* Le serveur refuse la requête sans ce champ à vrai : la barrière
           n'est pas seulement dans l'interface. */
        acceptedTerms,
      });
      // L'inscription ne renvoie pas de jeton : l'e-mail doit d'abord etre verifie.
      navigate(`/verify-email?email=${encodeURIComponent(email)}`);
    } catch (err) {
      const res = axios.isAxiosError(err) ? err.response : undefined;
      setError(
        res?.data?.message ??
          (res?.status === 403 || res?.status === 401
            ? t("inscription.compteExiste")
            : t("commun.erreurReseau")),
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth">
      <div className="auth__card">
        <header className="auth__head">
          <h1 className="auth__title">{t("inscription.titre")}</h1>
          <p className="auth__lead">{t("inscription.accroche")}</p>
        </header>

        {error && <Alert tone="danger" onDismiss={() => setError(null)}>{error}</Alert>}

        <form onSubmit={handleSubmit} className="auth__form">
          <div className="register__names">
            <Input
              label={t("inscription.prenom")}
              autoComplete="given-name"
              value={firstname}
              onChange={(e) => setFirstname(e.target.value)}
              placeholder={t("inscription.prenomExemple")}
              required
            />
            <Input
              label={t("inscription.nom")}
              autoComplete="family-name"
              value={lastname}
              onChange={(e) => setLastname(e.target.value)}
              placeholder={t("inscription.nomExemple")}
              required
            />
          </div>

          <Input
            label={t("inscription.emailPro")}
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder={t("inscription.emailProExemple")}
            hint={t("inscription.emailProAide")}
            required
          />

          <div className="auth__password">
            <Input
              label={t("inscription.motDePasse")}
              type={show ? "text" : "password"}
              autoComplete="new-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              error={passwordError}
              hint={passwordError ? undefined : t("inscription.motDePasseAide")}
              required
            />
            <button
              type="button"
              className="auth__eye"
              onClick={() => setShow((v) => !v)}
              aria-label={show ? t("connexion.masquerMotDePasse") : t("connexion.afficherMotDePasse")}
            >
              {show ? "◎" : "○"}
            </button>
          </div>

          <Input
            label={t("inscription.confirmer")}
            type={show ? "text" : "password"}
            autoComplete="new-password"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            error={confirmError}
            required
          />

          {/* Case distincte, jamais pré-cochée. L'article VI.83, 21° du Code
              de droit économique répute abusive la clause qui constate de
              manière irréfragable l'adhésion à des conditions dont on n'a pas
              eu connaissance : une acceptation déduite du seul fait de
              s'inscrire ne vaudrait rien. Les liens s'ouvrent dans un onglet
              séparé pour ne pas perdre la saisie en cours. */}
          <label className="auth__cgu">
            <input
              type="checkbox"
              checked={acceptedTerms}
              onChange={(e) => setAcceptedTerms(e.target.checked)}
              required
            />
            <span>
              {t("inscription.accepterCgu")}{" "}
              <Link to="/cgu" target="_blank" rel="noopener noreferrer">
                {t("inscription.accepterCguLien")}
              </Link>{" "}
              {t("inscription.accepterEt")}{" "}
              <Link to="/confidentialite" target="_blank" rel="noopener noreferrer">
                {t("inscription.accepterConfidentialiteLien")}
              </Link>
              .
            </span>
          </label>

          <Button type="submit" size="lg" block loading={loading} disabled={!acceptedTerms}>
            {t("inscription.creerMonCompte")}
          </Button>
        </form>

        <p className="auth__foot">
          {t("inscription.dejaInscrit")}{" "}
          <Link to="/login">{t("commun.seConnecter")}</Link>
        </p>
      </div>
    </div>
  );
}
