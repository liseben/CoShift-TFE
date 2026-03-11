import { useState } from "react";
import { Link, Outlet } from "react-router-dom";
import MarqueeText from "../components/marqueeText/MarqueeText";
import "./MainLayout.css";

export default function MainLayout() {
  const [showLoginMenu, setShowLoginMenu] = useState(false);
  return (
    <div className="layout-container">
      {/* HEADER */}
      <header className="main-header">
        <div className="header-left">
          <Link to="/" className="logo">
            CoShift
          </Link>
        </div>

        <nav className="header-center nav-links">
          <Link to="/entreprises" className="nav-item">
            Entreprises
          </Link>
          <Link to="/a-propos" className="nav-item">
            À propos
          </Link>
          <Link to="/blog" className="nav-item">
            Le Blog
          </Link>
        </nav>

        <div className="header-right action-buttons">
          <button 
            className="btn-download-app"
            onClick={() => alert("La logique d'installation de la PWA / APK sera ajoutée ici !")}
          >
            📱 Télécharger l'App
          </button>

          <select className="lang-select">
            <option>FR</option>
            <option>EN</option>
            <option>NL</option>
          </select>

          <div className="login-wrapper">
            <button
              className="nav-item btn-login-trigger"
              onClick={() => setShowLoginMenu(!showLoginMenu)}
            >
              Connexion
            </button>

            {/* LE POPOVER DE CONNEXION */}
            {showLoginMenu && (
              <div className="login-popover">
                <div className="popover-section">
                  <h4>Connexion</h4>
                  <p>J'ai déjà un compte</p>
                  <Link
                    to="/login"
                    className="btn-primary"
                    onClick={() => setShowLoginMenu(false)}
                  >
                    Se connecter
                  </Link>
                </div>

                <div className="popover-divider"></div>

                <div className="popover-section">
                  <h4>Inscription</h4>
                  <p>Je n'ai pas de compte</p>
                  <Link
                    to="/register"
                    className="btn-outline"
                    onClick={() => setShowLoginMenu(false)}
                  >
                    S'inscrire
                  </Link>
                </div>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* CONTENU PRINCIPAL DYNAMIQUE */}
      <main className="main-content">
        <Outlet />
      </main>

      <MarqueeText />

      {/* FOOTER */}
      <footer className="main-footer">
        <div className="footer-content">
          <p>© {new Date().getFullYear()} CoShift</p>
        </div>
      </footer>
    </div>
  );
}
