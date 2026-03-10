import Background3D from "../../components/Background3D";
import "./HomePage.css";

export default function HomePage() {
  return (
    <div className="home-container">
      {/* Notre nouveau composant 3D en arrière-plan */}
      <Background3D />

      {/* HERO SECTION - On s'assure qu'elle est au-dessus avec le z-index dans le CSS */}
      <section className="hero-section">
        <div className="hero-content">
          <span className="badge">B2B & Campus</span>
          <h1 className="hero-title">
            Partagez vos trajets quotidiens avec{" "}
            <span className="highlight">CoShift</span>
          </h1>
          <p className="hero-subtitle">
            La solution de covoiturage pensée pour les entreprises, les
            universités et les festivals. Réduisez votre empreinte carbone et
            vos frais de transport.
          </p>

          <div className="quick-search-card">
            <div className="input-group">
              <label>Départ</label>
              <input type="text" placeholder="Domicile, Ville..." />
            </div>
            <div className="input-group">
              <label>Arrivée</label>
              <input type="text" placeholder="Lieu de travail, Campus..." />
            </div>
            <button className="btn-search">Rechercher</button>
          </div>
        </div>
      </section>
    </div>
  );
}
