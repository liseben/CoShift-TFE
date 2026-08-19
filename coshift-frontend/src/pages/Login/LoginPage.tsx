import { GoogleLogin } from "@react-oauth/google";
import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import axios from "axios";
import { useAuth } from "../../context/AuthContext";
import { API_BASE } from "../../config/api";
import { Alert, Button, Input } from "../../components/ui";
import "../Auth/auth.css";

type View = "login" | "forgot";

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [view, setView] = useState<View>("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [forgotEmail, setForgotEmail] = useState("");

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
      const status = axios.isAxiosError(err) ? err.response?.status : undefined;
      setError(
        status === 401 || status === 403
          ? "E-mail ou mot de passe incorrect."
          : "Impossible de joindre le serveur. Veuillez réessayer.",
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
            ? "Cet utilisateur n'existe pas. Veuillez créer un compte."
            : "Échec de la connexion avec Google. Veuillez réessayer."),
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
      await axios.post(`${API_BASE}/api/auth/forgot-password`, { email: forgotEmail });
      setSuccess("Si ce compte existe, un e-mail contenant les instructions vient d'être envoyé.");
      setForgotEmail("");
    } catch {
      setError("Impossible de contacter le serveur. Veuillez réessayer.");
    } finally {
      setLoading(false);
    }
  };

  const switchView = (next: View) => {
    setView(next);
    setError(null);
    setSuccess(null);
  };

  return (
    <div className="auth">
      <div className="auth__card">
        <header className="auth__head">
          <h1 className="auth__title">
            {view === "login" ? "Connexion" : "Mot de passe oublié"}
          </h1>
          <p className="auth__lead">
            {view === "login"
              ? "Connectez-vous pour proposer ou trouver un trajet."
              : "Indiquez votre adresse pour recevoir un lien de réinitialisation."}
          </p>
        </header>

        {error && <Alert tone="danger" onDismiss={() => setError(null)}>{error}</Alert>}
        {success && <Alert tone="success">{success}</Alert>}

        {view === "login" ? (
          <>
            <div className="auth__google">
              <GoogleLogin
                onSuccess={(r) => handleGoogle(r.credential)}
                onError={() => setError("La fenêtre Google s'est fermée ou une erreur est survenue.")}
                shape="pill"
                text="continue_with"
              />
            </div>

            <p className="auth__sep">ou</p>

            <form onSubmit={handleSubmit} className="auth__form">
              <Input
                label="Adresse e-mail"
                type="email"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                onFocus={() => setError(null)}
                placeholder="prenom.nom@entreprise.be"
                required
              />

              <div className="auth__password">
                <Input
                  label="Mot de passe"
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
                  aria-label={showPassword ? "Masquer le mot de passe" : "Afficher le mot de passe"}
                >
                  <EyeIcon off={showPassword} />
                </button>
              </div>

              <div className="auth__options">
                <label className="auth__remember">
                  <input type="checkbox" /> Se souvenir de moi
                </label>
                <button type="button" className="auth__link" onClick={() => switchView("forgot")}>
                  Mot de passe oublié ?
                </button>
              </div>

              <Button type="submit" size="lg" block loading={loading}>
                Se connecter
              </Button>
            </form>
          </>
        ) : (
          <form onSubmit={handleForgot} className="auth__form">
            <Input
              label="Adresse e-mail"
              type="email"
              autoComplete="email"
              value={forgotEmail}
              onChange={(e) => setForgotEmail(e.target.value)}
              onFocus={() => { setError(null); setSuccess(null); }}
              placeholder="prenom.nom@entreprise.be"
              required
            />
            <Button type="submit" size="lg" block loading={loading}>
              Envoyer le lien
            </Button>
            <Button type="button" variant="ghost" onClick={() => switchView("login")}>
              ← Retour à la connexion
            </Button>
          </form>
        )}

        <p className="auth__foot">
          Nouveau sur CoShift ? <Link to="/register">Créer un compte</Link>
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
