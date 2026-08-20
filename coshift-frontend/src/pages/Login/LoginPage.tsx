import { GoogleLogin } from "@react-oauth/google";
import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import axios from "axios";
import { useAuth } from "../../context/AuthContext";
import { useT } from "../../context/LangContext";
import { API_BASE } from "../../config/api";
import { Alert, Button, Input } from "../../components/ui";
import GoogleGate from "../../components/Consent/GoogleGate";
import { useSeo } from "../../hooks/useSeo";
import "../Auth/auth.css";

type View = "login" | "forgot" | "reset";

/* Le titre annonçait « un lien de réinitialisation » alors que le serveur
   envoie un code à six chiffres, comme pour la vérification d'adresse.

   Les libellés sont désormais des couples de clés plutôt que du texte : une
   constante de module est évaluée au chargement du fichier, avant que la
   langue soit connue. */
const HEAD: Record<View, { titre: string; accroche: string }> = {
  login:  { titre: "connexion.titre",       accroche: "connexion.accroche" },
  forgot: { titre: "connexion.oublieTitre", accroche: "connexion.oublieAccroche" },
  reset:  { titre: "connexion.resetTitre",  accroche: "connexion.resetAccroche" },
};

export default function LoginPage() {
  /* Page volontairement retirée de l'index : un écran de connexion en résultat
     de recherche n'apporte rien à qui cherche du covoiturage. Elle reste
     autorisée au crawl dans robots.txt — une page interdite d'exploration ne
     serait jamais visitée, et cette consigne jamais lue. */
  const { login } = useAuth();
  const t = useT();
  const navigate = useNavigate();

  useSeo({
    titre: t("connexion.titre"),
    description: t("connexion.accroche"),
    chemin: "/login",
    horsIndex: true,
  });

  const [view, setView] = useState<View>("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [forgotEmail, setForgotEmail] = useState("");
  const [resetCode, setResetCode] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const enter = (token: string) => {
    login(token);
    navigate("/");
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const { data } = await axios.post(`${API_BASE}/api/auth/login`, { email, password });
      if (data.token) enter(data.token);
    } catch (err) {
      const res = axios.isAxiosError(err) ? err.response : undefined;
      /* Le serveur rédige des messages précis — compte non activé (403), trop
         de tentatives (429) — que « E-mail ou mot de passe incorrect » écrasait
         jusqu'ici. On ne retombe sur un texte générique que sans réponse. */
      setError(
        res?.data?.message ??
          (res?.status === 401
            ? t("connexion.identifiantsRefuses")
            : t("commun.erreurReseau")),
      );
      setLoading(false);
    }
  };

  const handleGoogle = async (credential: string | undefined) => {
    setLoading(true);
    setError(null);
    try {
      const { data } = await axios.post(`${API_BASE}/api/auth/google`, { token: credential });
      if (data.token) enter(data.token);
    } catch (err) {
      const res = axios.isAxiosError(err) ? err.response : undefined;
      setError(
        res?.data?.message ??
          (res?.status === 401 || res?.status === 403
            ? t("connexion.googleInconnu")
            : t("connexion.googleEchec")),
      );
      setLoading(false);
    }
  };

  const handleForgot = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      const { data } = await axios.post(`${API_BASE}/api/auth/forgot-password`, {
        email: forgotEmail,
      });
      /* L'adresse est conservée : l'étape suivante en a besoin, et la
         redemander alors qu'elle vient d'être saisie n'apporte rien. Le
         serveur répond la même chose que le compte existe ou non, pour ne pas
         révéler qui est inscrit. */
      setView("reset");
      setSuccess(data?.message ?? t("connexion.codeEnvoye"));
    } catch (err) {
      setError(
        (axios.isAxiosError(err) && err.response?.data?.message) ||
          t("commun.erreurReseau"),
      );
    } finally {
      setLoading(false);
    }
  };

  const handleReset = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    // Contrôle côté client uniquement : la règle qui fait foi est celle du
    // backend, qui refuse tout mot de passe de moins de six caractères.
    if (newPassword !== confirmPassword) {
      setError(t("connexion.motsDePasseDifferents"));
      return;
    }

    setLoading(true);
    try {
      await axios.post(`${API_BASE}/api/auth/reset-password`, {
        email: forgotEmail,
        code: resetCode,
        newPassword,
      });
      setView("login");
      setEmail(forgotEmail);
      setSuccess(t("connexion.motDePasseModifie"));
      setForgotEmail("");
      setResetCode("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (err) {
      setError(
        (axios.isAxiosError(err) && err.response?.data?.message) ||
          t("commun.erreurReseau"),
      );
    } finally {
      setLoading(false);
    }
  };

  const switchView = (next: View) => {
    setView(next);
    setError(null);
    setSuccess(null);
    if (next === "login") {
      setResetCode("");
      setNewPassword("");
      setConfirmPassword("");
    }
  };

  return (
    <div className="auth">
      <div className="auth__card">
        <header className="auth__head">
          <h1 className="auth__title">{t(HEAD[view].titre)}</h1>
          <p className="auth__lead">{t(HEAD[view].accroche)}</p>
        </header>

        {error && <Alert tone="danger" onDismiss={() => setError(null)}>{error}</Alert>}
        {success && <Alert tone="success">{success}</Alert>}

        {view === "login" ? (
          <>
            <div className="auth__google">
              {/* Le fournisseur Google n'est monté que si le service a été
                  autorisé : sans barrière, son script partirait dès l'affichage
                  de l'écran, avant tout clic. */}
              <GoogleGate>
                <GoogleLogin
                  onSuccess={(r) => handleGoogle(r.credential)}
                  onError={() => setError(t("connexion.erreurGoogle"))}
                  shape="pill"
                  text="continue_with"
                />
              </GoogleGate>
            </div>

            <p className="auth__sep">{t("connexion.ou")}</p>

            <form onSubmit={handleSubmit} className="auth__form">
              <Input
                label={t("connexion.email")}
                type="email"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                onFocus={() => setError(null)}
                placeholder={t("connexion.emailExemple")}
                required
              />

              <div className="auth__password">
                <Input
                  label={t("connexion.motDePasse")}
                  type={showPassword ? "text" : "password"}
                  autoComplete="current-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  onFocus={() => setError(null)}
                  required
                />
                <button
                  type="button"
                  className="auth__eye"
                  onClick={() => setShowPassword((v) => !v)}
                  aria-label={showPassword ? t("connexion.masquerMotDePasse") : t("connexion.afficherMotDePasse")}
                >
                  <EyeIcon off={showPassword} />
                </button>
              </div>

              <div className="auth__options">
                <label className="auth__remember">
                  <input type="checkbox" /> {t("connexion.seSouvenir")}
                </label>
                <button type="button" className="auth__link" onClick={() => switchView("forgot")}>
                  {t("connexion.oublie")}
                </button>
              </div>

              <Button type="submit" size="lg" block loading={loading}>
                {t("commun.seConnecter")}
              </Button>
            </form>
          </>
        ) : view === "forgot" ? (
          <form onSubmit={handleForgot} className="auth__form">
            <Input
              label={t("connexion.email")}
              type="email"
              autoComplete="email"
              value={forgotEmail}
              onChange={(e) => setForgotEmail(e.target.value)}
              onFocus={() => { setError(null); setSuccess(null); }}
              placeholder={t("connexion.emailExemple")}
              required
            />
            <Button type="submit" size="lg" block loading={loading}>
              {t("connexion.envoyerCode")}
            </Button>
            <Button type="button" variant="ghost" onClick={() => switchView("login")}>
              {t("connexion.retourConnexion")}
            </Button>
          </form>
        ) : (
          <form onSubmit={handleReset} className="auth__form">
            <Input
              label={t("connexion.codeRecu")}
              inputMode="numeric"
              autoComplete="one-time-code"
              maxLength={6}
              value={resetCode}
              onChange={(e) => setResetCode(e.target.value.replace(/\D/g, ""))}
              onFocus={() => { setError(null); setSuccess(null); }}
              hint={t("connexion.codeAide", { email: forgotEmail })}
              placeholder="000000"
              required
            />

            <Input
              label={t("connexion.nouveauMotDePasse")}
              type={showPassword ? "text" : "password"}
              autoComplete="new-password"
              minLength={6}
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              onFocus={() => setError(null)}
              hint={t("connexion.nouveauMotDePasseAide")}
              required
            />

            <Input
              label={t("connexion.confirmerMotDePasse")}
              type={showPassword ? "text" : "password"}
              autoComplete="new-password"
              minLength={6}
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              onFocus={() => setError(null)}
              required
            />

            <label className="auth__remember">
              <input
                type="checkbox"
                checked={showPassword}
                onChange={() => setShowPassword((v) => !v)}
              />{" "}
              {t("connexion.afficherLesMotsDePasse")}
            </label>

            <Button type="submit" size="lg" block loading={loading}>
              {t("connexion.changerMotDePasse")}
            </Button>
            <Button type="button" variant="ghost" onClick={() => switchView("forgot")}>
              {t("connexion.demanderNouveauCode")}
            </Button>
          </form>
        )}

        <p className="auth__foot">
          {t("connexion.nouveauSurCoShift")}{" "}
          <Link to="/register">{t("commun.creerCompte")}</Link>
        </p>
      </div>
    </div>
  );
}

function EyeIcon({ off }: { off: boolean }) {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
         strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {off ? (
        <>
          <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
          <line x1="1" y1="1" x2="23" y2="23" />
        </>
      ) : (
        <>
          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
          <circle cx="12" cy="12" r="3" />
        </>
      )}
    </svg>
  );
}
