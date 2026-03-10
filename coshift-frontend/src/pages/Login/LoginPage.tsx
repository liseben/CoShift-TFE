import React, { useState } from "react";
import "./LoginPage.css";

const LoginPage: React.FC = () => {
  const [email, setEmail] = useState<string>("");
  const [password, setPassword] = useState<string>("");
  const [showPassword, setShowPassword] = useState<boolean>(false);

  // On type l'événement "e" pour savoir que c'est un formulaire HTML
  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    console.log("Tentative de connexion avec :", email, password);
    // TODO: Appel API
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <h2 className="login-title">Connexion CoShift</h2>

        <form onSubmit={handleSubmit} className="login-form">
          <div className="input-group">
            <label className="input-label">Email</label>
            <input
              type="email"
              className="login-input"
              value={email}
              // TypeScript sait que e.target.value est un string
              onChange={(e) => setEmail(e.target.value)}
              required
              placeholder="exemple@email.com"
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
                // Bonne pratique accessibilité
                aria-label={
                  showPassword
                    ? "Masquer le mot de passe"
                    : "Afficher le mot de passe"
                }
              >
                {showPassword ? (
                  // Icône Oeil Barré
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
                  // Icône Oeil Ouvert
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

          <button type="submit" className="login-button">
            Se connecter
          </button>
        </form>
      </div>
    </div>
  );
};

export default LoginPage;
