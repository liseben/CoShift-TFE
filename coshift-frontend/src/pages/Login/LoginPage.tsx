import { GoogleLogin } from "@react-oauth/google";
import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import axios from "axios";
import "./LoginPage.css";

const API_BASE = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

const LoginPage: React.FC = () => {
  const [email, setEmail] = useState<string>("");
  const [password, setPassword] = useState<string>("");
  const [showPassword, setShowPassword] = useState<boolean>(false);

  // États pour la communication avec le backend
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setIsLoading(true);
    setError(null);

    try {
      // Appel API vers ton backend Spring Boot
      const response = await axios.post(`${API_BASE}/api/auth/login`, {
        email,
        password,
      });

      const token = response.data.token;
      if (token) {
        localStorage.setItem("coshift_token", token);
        navigate("/dashboard"); // Redirection après succès
      }
    } catch (err: any) {
      console.error("Erreur de connexion :", err);
      if (err.response?.status === 401 || err.response?.status === 403) {
        setError("Email ou mot de passe incorrect.");
      } else {
        setError("Impossible de joindre le serveur. Veuillez réessayer.");
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleGoogleSuccess = async (credentialResponse: any) => {
    setIsLoading(true);
    setError(null);
    try {
      // credentialResponse.credential contient le token sécurisé de Google
      const googleToken = credentialResponse.credential;

      // On enverra ce token à notre Spring Boot plus tard
      const response = await axios.post(`${API_BASE}/api/auth/google`, {
        token: googleToken,
      });

      const token = response.data.token;
      if (token) {
        localStorage.setItem("coshift_token", token);
        navigate("/dashboard");
      }
    } catch (err) {
      console.error("Erreur Google :", err);
      setError("Échec de la connexion avec Google. Veuillez réessayer.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-header">
          <h2 className="login-title">Connexion CoShift</h2>
          <p>Connectez-vous pour proposer ou trouver un trajet.</p>
        </div>

        {error && <div className="auth-error-message">⚠️ {error}</div>}

        {/* --- LE NOUVEAU BOUTON GOOGLE --- */}
        <div
          style={{
            display: "flex",
            justifyContent: "center",
            marginBottom: "20px",
          }}
        >
          <GoogleLogin
            onSuccess={handleGoogleSuccess}
            onError={() => {
              setError(
                "La fenêtre Google a été fermée ou une erreur est survenue.",
              );
            }}
            theme="filled_black" // S'intègre bien avec ton design sombre
            shape="pill"
            text="continue_with"
          />
        </div>

        <div
          style={{
            textAlign: "center",
            color: "rgba(255,255,255,0.5)",
            marginBottom: "20px",
            fontSize: "0.9rem",
          }}
        >
          — OU —
        </div>
        {/* --- FIN DU BOUTON GOOGLE --- */}

        <form onSubmit={handleSubmit} className="login-form">
          <div className="input-group">
            <label className="input-label">Email</label>
            <input
              type="email"
              className="login-input"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              placeholder="elisabeth.benga@entreprise.be"
            />
          </div>

          <div className="input-group">
            <label className="input-label">Mot de passe</label>
            <div className="password-wrapper">
              <input
                type={showPassword ? "text" : "password"}
                className="login-input"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                placeholder="Votre mot de passe"
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

          <div className="auth-options">
            <label className="remember-me">
              <input type="checkbox" /> Se souvenir de moi
            </label>
            <a href="#" className="forgot-password">
              Mot de passe oublié ?
            </a>
          </div>

          <button
            type="submit"
            className={`login-button ${isLoading ? "loading" : ""}`}
            disabled={isLoading}
          >
            {isLoading ? "Connexion en cours..." : "Se connecter"}
          </button>
        </form>

        <div className="auth-footer">
          <p>
            Nouveau sur CoShift ? <Link to="/register">Créer un compte</Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
