import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import axios from "axios";
import "./RegisterPage.css";

const API_BASE = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

const RegisterPage: React.FC = () => {
  const [firstname, setFirstname] = useState<string>("");
  const [lastname, setLastname] = useState<string>("");
  const [email, setEmail] = useState<string>("");
  const [password, setPassword] = useState<string>("");
  const [confirmPassword, setConfirmPassword] = useState<string>("");
  const [showPassword, setShowPassword] = useState<boolean>(false);
  const [showConfirmPassword, setShowConfirmPassword] =
    useState<boolean>(false);

  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    // 1. Vérification côté Front-End
    if (password !== confirmPassword) {
      setError("Les mots de passe ne correspondent pas.");
      return;
    }
    if (password.length < 6) {
      setError("Le mot de passe doit contenir au moins 6 caractères.");
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      // 2. Appel API vers ton backend Spring Boot
      await axios.post(`${API_BASE}/api/auth/register`, {
        firstname,
        lastname,
        email,
        password,
      });

      // 3. L'inscription ne renvoie plus de token — l'utilisateur doit vérifier son email
      navigate(`/verify-email?email=${encodeURIComponent(email)}`);
    } catch (err: any) {
      console.error("Erreur d'inscription :", err);
      // Si le backend renvoie une erreur (ex: email déjà pris)
      if (err.response && err.response.data && err.response.data.message) {
        setError(err.response.data.message);
      } else if (err.response?.status === 403 || err.response?.status === 401) {
        setError("Un compte existe peut-être déjà avec cet email.");
      } else {
        setError("Impossible de joindre le serveur. Veuillez réessayer.");
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="register-container">
      <div className="register-card">
        <div className="register-header">
          <h2 className="register-title">Rejoindre CoShift</h2>
          <p>Créez votre compte pour commencer à covoiturer.</p>
        </div>

        {error && <div className="auth-error-message">⚠️ {error}</div>}

        <form onSubmit={handleSubmit} className="register-form">
          {/* Grille pour mettre Prénom et Nom côte à côte */}
          <div className="name-grid">
            <div className="input-group">
              <label className="input-label">Prénom</label>
              <input
                type="text"
                className="register-input"
                value={firstname}
                onChange={(e) => setFirstname(e.target.value)}
                required
                placeholder="Jean"
              />
            </div>
            <div className="input-group">
              <label className="input-label">Nom</label>
              <input
                type="text"
                className="register-input"
                value={lastname}
                onChange={(e) => setLastname(e.target.value)}
                required
                placeholder="Dupont"
              />
            </div>
          </div>

          <div className="input-group">
            <label className="input-label">Email professionnel</label>
            <input
              type="email"
              className="register-input"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              placeholder="jean.dupont@entreprise.be"
            />
          </div>

          <div className="input-group">
            <label className="input-label">Mot de passe</label>
            <div className="password-wrapper">
              <input
                type={showPassword ? "text" : "password"}
                className="register-input"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                placeholder="Minimum 6 caractères"
              />
              <button
                type="button"
                className="eye-button"
                onClick={() => setShowPassword(!showPassword)}
                aria-label={
                  showPassword
                    ? "Masquer le mot de passe"
                    : "Afficher le mot de passe"
                }
              >
                {/* Icônes SVG (Identiques au login) */}
                {showPassword ? (
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    width="20"
                    height="20"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                    <line x1="1" y1="1" x2="23" y2="23"></line>
                  </svg>
                ) : (
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    width="20"
                    height="20"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                    <circle cx="12" cy="12" r="3"></circle>
                  </svg>
                )}
              </button>
            </div>
          </div>

          <div className="input-group">
            <label className="input-label">Confirmer le mot de passe</label>
            <div className="password-wrapper">
              <input
                type={showConfirmPassword ? "text" : "password"}
                className="register-input"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
                placeholder="Retapez votre mot de passe"
              />
              <button
                type="button"
                className="eye-button"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                aria-label={
                  showConfirmPassword
                    ? "Masquer le mot de passe"
                    : "Afficher le mot de passe"
                }
              >
                {showConfirmPassword ? (
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    width="20"
                    height="20"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                    <line x1="1" y1="1" x2="23" y2="23"></line>
                  </svg>
                ) : (
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    width="20"
                    height="20"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                    <circle cx="12" cy="12" r="3"></circle>
                  </svg>
                )}
              </button>
            </div>
          </div>

          <button
            type="submit"
            className={`register-button ${isLoading ? "loading" : ""}`}
            disabled={isLoading}
          >
            {isLoading ? "Création en cours..." : "Créer mon compte"}
          </button>
        </form>

        <div className="auth-footer">
          <p>
            Vous avez déjà un compte ? <Link to="/login">Se connecter</Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default RegisterPage;
