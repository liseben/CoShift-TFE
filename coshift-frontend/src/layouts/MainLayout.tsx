import { useState, useEffect, useRef } from "react";
import { Link, Outlet, useNavigate } from "react-router-dom";
import MarqueeText from "../components/marqueeText/MarqueeText";
import Logo from "../components/Logo/Logo";
import ThemeToggle from "../components/ThemeToggle/ThemeToggle";
import { Avatar } from "../components/ui";
import { useAuth } from "../context/AuthContext";
import { FiGlobe } from "react-icons/fi";
import "./MainLayout.css";

export default function MainLayout() {
  const [showLoginMenu, setShowLoginMenu] = useState(false);
  const [showLangMenu, setShowLangMenu] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);                                    
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

  const closeMobileMenu = () => {
    setIsMobileMenuOpen(false);
  };

  return (
    <div className="layout-container">
      {/* HEADER */}
      <header className="main-header">
        <div className="header-left">
          <Link to="/" aria-label="CoShift, accueil">
            <Logo size={34} />
          </Link>
        </div>

        <button
          className="mobile-toggle-btn"
          onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
          aria-label="Menu"
        >
          <svg
            width="32"
            height="32"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            {isMobileMenuOpen ? (
              // Icône Croix (Fermer)
              <>
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </>
            ) : (
              // Icône Hamburger (Ouvrir) - Espacement parfaitement centré
              <>
                <line x1="3" y1="12" x2="21" y2="12"></line>
                <line x1="3" y1="6" x2="21" y2="6"></line>
                <line x1="3" y1="18" x2="21" y2="18"></line>
              </>
            )}
          </svg>
        </button>

        <div
          className={`header-menus-wrapper ${isMobileMenuOpen ? "open" : ""}`}
        >
          <nav className="header-center nav-links">
            <Link
              to="/entreprises"
              className="nav-item"
              onClick={closeMobileMenu}
            >
              Entreprises
            </Link>
            <Link to="/actus" className="nav-item" onClick={closeMobileMenu}>
              Actus Mobilité
            </Link>
            <Link to="/a-propos" className="nav-item" onClick={closeMobileMenu}>
              À propos
            </Link>
            <Link to="/blog" className="nav-item" onClick={closeMobileMenu}>
              Le Blog
            </Link>
          </nav>

          <div className="header-right action-buttons">
            <ThemeToggle />
            <button
              className="btn-download-app"
              onClick={() =>
                alert(
                  "La logique d'installation de la PWA / APK sera ajoutée ici !",
                )
              }
            >
              Téléchargez l'App
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
                <FiGlobe size={28} />
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
                  aria-label="Mon profil"
                >
                  <Avatar
                    src={user.pictureUrl}
                    name={`${user.firstname} ${user.lastname}`}
                    size="sm"
                    verified={user.emailVerified}
                  />

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
                    // Popover quand CONNECTÉ : Une seule colonne bien espacée
                    <div
                      className="popover-section"
                      style={{
                        textAlign: "center",
                        width: "100%",
                        alignItems: "center",
                      }}
                    >
      
                      <h4>Bonjour, {user.firstname} 👋</h4>
                      <p style={{ marginBottom: "1.5rem" }}>{user.email}</p>

                      {/* LA BOÎTE QUI GÈRE L'ESPACE */}
                      <div className="popover-actions">
                        <Link
                          to="/dashboard"
                          className="popover-btn btn-primary"
                          onClick={() => {
                            setShowLoginMenu(false);
                            closeMobileMenu();
                          }}
                        >
                          Dashboard
                        </Link>

                        <button
                          onClick={handleLogout}
                          className="popover-btn btn-logout"
                        >
                          Se déconnecter
                        </button>
                      </div>
                    </div>
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
