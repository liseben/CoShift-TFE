import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import axios from "axios";
import { API_BASE } from "../../config/api";
import { Alert, Button, Input } from "../../components/ui";
import "../Auth/auth.css";
import "./RegisterPage.css";

export default function RegisterPage() {
  const navigate = useNavigate();

  const [firstname, setFirstname] = useState("");
  const [lastname, setLastname] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [show, setShow] = useState(false);

  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  /* Validation par champ : l'erreur s'affiche sous le champ fautif plutot
     qu'en un bloc unique en haut du formulaire. */
  const passwordError =
    password.length > 0 && password.length < 6
      ? "Au moins 6 caractères."
      : undefined;
  const confirmError =
    confirm.length > 0 && confirm !== password
      ? "Les deux mots de passe diffèrent."
      : undefined;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (passwordError || confirmError || password !== confirm) {
      setError("Corrigez les champs signalés avant de continuer.");
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
      });
      // L'inscription ne renvoie pas de jeton : l'e-mail doit d'abord etre verifie.
      navigate(`/verify-email?email=${encodeURIComponent(email)}`);
    } catch (err) {
      const res = axios.isAxiosError(err) ? err.response : undefined;
      setError(
        res?.data?.message ??
          (res?.status === 403 || res?.status === 401
            ? "Un compte existe peut-être déjà avec cette adresse."
            : "Impossible de joindre le serveur. Veuillez réessayer."),
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth">
      <div className="auth__card">
        <header className="auth__head">
          <h1 className="auth__title">Rejoindre CoShift</h1>
          <p className="auth__lead">Créez votre compte pour commencer à covoiturer.</p>
        </header>

        {error && <Alert tone="danger" onDismiss={() => setError(null)}>{error}</Alert>}

        <form onSubmit={handleSubmit} className="auth__form">
          <div className="register__names">
            <Input
              label="Prénom"
              autoComplete="given-name"
              value={firstname}
              onChange={(e) => setFirstname(e.target.value)}
              placeholder="Jean"
              required
            />
            <Input
              label="Nom"
              autoComplete="family-name"
              value={lastname}
              onChange={(e) => setLastname(e.target.value)}
              placeholder="Dupont"
              required
            />
          </div>

          <Input
            label="E-mail professionnel"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="jean.dupont@entreprise.be"
            hint="C'est cette adresse qui vous rattache à votre organisation."
            required
          />

          <div className="auth__password">
            <Input
              label="Mot de passe"
              type={show ? "text" : "password"}
              autoComplete="new-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              error={passwordError}
              hint={passwordError ? undefined : "6 caractères minimum."}
              required
            />
            <button
              type="button"
              className="auth__eye"
              onClick={() => setShow((v) => !v)}
              aria-label={show ? "Masquer le mot de passe" : "Afficher le mot de passe"}
            >
              {show ? "◎" : "○"}
            </button>
          </div>

          <Input
            label="Confirmer le mot de passe"
            type={show ? "text" : "password"}
            autoComplete="new-password"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            error={confirmError}
            required
          />

          <Button type="submit" size="lg" block loading={loading}>
            Créer mon compte
          </Button>
        </form>

        <p className="auth__foot">
          Déjà inscrit ? <Link to="/login">Se connecter</Link>
        </p>
      </div>
    </div>
  );
}
