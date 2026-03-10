import { Link, Outlet } from "react-router-dom";
import "./MainLayout.css";

export default function MainLayout() {
  return (
    <div className="layout-container">
      {/* HEADER */}
      <header className="main-header">
        <div className="header-content">
          <Link to="/" className="logo">
            
            <h1>CoShift</h1>
          </Link>

          <nav className="main-nav">
            <Link to="/trajets" className="nav-link">
              Trajets
            </Link>
            <Link to="/organisation" className="nav-link">
              Mon Campus/Entreprise
            </Link>
          </nav>

          <div className="header-actions">
            <Link to="/login" className="btn-login">
              Connexion
            </Link>
            <Link to="/register" className="btn-register">
              S'inscrire
            </Link>
          </div>
        </div>
      </header>

      {/* CONTENU PRINCIPAL DYNAMIQUE */}
      <main className="main-content">
        <Outlet />
      </main>

      {/* FOOTER */}
      <footer className="main-footer">
        <div className="footer-content">
          <p>
            © {new Date().getFullYear()} CoShift
          </p>
        </div>
      </footer>
    </div>
  );
}
