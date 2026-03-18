import { useState, useEffect, useRef } from "react";
import { Link, Outlet, useNavigate } from "react-router-dom";
import MarqueeText from "../components/marqueeText/MarqueeText";
import { useAuth } from "../context/AuthContext";
import "./MainLayout.css";

export default function MainLayout() {
  const [showLoginMenu, setShowLoginMenu] = useState(false);
  const [showLangMenu, setShowLangMenu] = useState(false);
  const [currentLang, setCurrentLang] = useState("FR");
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    setShowLoginMenu(false);
    navigate("/");
  };

  // 1. On crée des "références" pour nos deux menus
  const langMenuRef = useRef<HTMLDivElement>(null);
  const loginMenuRef = useRef<HTMLDivElement>(null);

  // 2. On écoute les clics pour le menu Langue
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      // Si on clique en dehors du wrapper "Langue", on le ferme
      if (
        langMenuRef.current &&
        !langMenuRef.current.contains(event.target as Node)
      ) {
        setShowLangMenu(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // 3. On écoute les clics pour le menu Connexion
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      // Si on clique en dehors du wrapper "Connexion", on le ferme
      if (
        loginMenuRef.current &&
        !loginMenuRef.current.contains(event.target as Node)
      ) {
        setShowLoginMenu(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

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
          <Link to="/actus" className="nav-item">
            Actus Mobilité
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
            onClick={() =>
              alert(
                "La logique d'installation de la PWA / APK sera ajoutée ici !",
              )
            }
          >
            Télécharger l'App
          </button>

          <div className="lang-wrapper" ref={langMenuRef}>
            <button
              /* On ajoute la classe "open" si le menu est ouvert pour tourner la flèche */
              className={`btn-lang-trigger ${showLangMenu ? "open" : ""}`}
              onClick={() => {
                setShowLangMenu(!showLangMenu);
                setShowLoginMenu(false);
              }}
            >
              <span>🌍</span>
              <span>{currentLang}</span>
              {/* Le petit chevron (flèche) en SVG pour un rendu hyper net */}
              <svg
                className="chevron"
                width="14"
                height="14"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <polyline points="6 9 12 15 18 9"></polyline>
              </svg>
            </button>

            {showLangMenu && (
              <div className="lang-dropdown">
                {["FR", "EN", "NL"].map((lang) => (
                  <button
                    key={lang}
                    className={`lang-option ${currentLang === lang ? "active" : ""}`}
                    onClick={() => {
                      setCurrentLang(lang);
                      setShowLangMenu(false);
                    }}
                  >
                    {lang}
                  </button>
                ))}
              </div>
            )}
          </div>

          <div className="login-wrapper" ref={loginMenuRef}>
            {user ? (
              // --- SI CONNECTÉ : Affichage de la photo + Bouton "Mon profil" ---
              <button
                className="btn-profile-trigger"
                onClick={() => setShowLoginMenu(!showLoginMenu)}
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "10px",
                  background: "none",
                  border: "none",
                  cursor: "pointer",
                  color: "white",
                }}
              >
                {/* La photo de profil (ou l'initiale par défaut) */}
                {user.pictureUrl ? (
                  <img
                    src={user.pictureUrl}
                    alt="Profil"
                    style={{
                      width: "35px",
                      height: "35px",
                      borderRadius: "50%",
                      objectFit: "cover",
                      border: "2px solid #ffaa00",
                    }}
                  />
                ) : (
                  <div
                    style={{
                      width: "35px",
                      height: "35px",
                      borderRadius: "50%",
                      backgroundColor: "#ffaa00",
                      display: "flex",
                      justifyContent: "center",
                      alignItems: "center",
                      fontWeight: "bold",
                      color: "#0f172a",
                    }}
                  >
                    {user.firstname.charAt(0)}
                  </div>
                )}

                <span className="nav-item">Mon profil</span>
              </button>
            ) : (
              // --- SI NON CONNECTÉ : Bouton "Connexion" classique ---
              <button
                className="nav-item btn-login-trigger"
                onClick={() => setShowLoginMenu(!showLoginMenu)}
              >
                Connexion
              </button>
            )}

            {/* LE POPOVER DYNAMIQUE */}
            {showLoginMenu && (
              <div className="login-popover">
                {user ? (
                  // Popover quand CONNECTÉ
                  <>
                    <div
                      className="popover-section"
                      style={{ textAlign: "center" }}
                    >
                      <h4>Bonjour, {user.firstname} 👋</h4>
                      <p>{user.email}</p>
                      <Link
                        to="/dashboard"
                        className="popover-btn btn-primary"
                        onClick={() => setShowLoginMenu(false)}
                      >
                        Dashboard
                      </Link>
                    </div>
                    <div className="popover-divider"></div>
                    <div className="popover-section">
                      <button
                        onClick={handleLogout}
                        className="popover-btn btn-outline"
                        style={{ color: "#ff6b6b", borderColor: "#ff6b6b" }}
                      >
                        Se déconnecter
                      </button>
                    </div>
                  </>
                ) : (
                  // Popover quand NON CONNECTÉ (Celui que tu avais déjà)
                  <>
                    <div className="popover-section">
                      <div className="popover-icon">👋</div>
                      <h4>Bon retour</h4>
                      <p>Accédez à votre espace CoShift.</p>
                      <Link
                        to="/login"
                        className="popover-btn btn-primary"
                        onClick={() => setShowLoginMenu(false)}
                      >
                        Se connecter
                      </Link>
                    </div>
                    <div className="popover-divider"></div>
                    <div className="popover-section">
                      <div className="popover-icon">✨</div>
                      <h4>Nouveau ici ?</h4>
                      <p>Rejoignez la mobilité de demain.</p>
                      <Link
                        to="/register"
                        className="popover-btn btn-outline"
                        onClick={() => setShowLoginMenu(false)}
                      >
                        Créer un compte
                      </Link>
                    </div>
                  </>
                )}
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
