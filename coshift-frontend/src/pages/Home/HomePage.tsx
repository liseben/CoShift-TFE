import { useNavigate } from "react-router-dom";
import MapBackground from "../../components/MapBackground";
import TripSearchForm, { type TripCriteria } from "../../components/TripSearchForm/TripSearchForm";
import "./HomePage.css";

export default function HomePage() {
  const navigate = useNavigate();

  /* Le formulaire ne cherche pas lui-même : il transmet ses critères à la
     page de recherche par l'URL. Celle-ci reste ainsi partageable et
     réutilisable en favori. */
  const search = (c: TripCriteria) => {
    const params = new URLSearchParams(
      Object.entries(c).filter(([, v]) => v !== ""),
    );
    navigate(`/trips/search?${params.toString()}`);
  };

  return (
    <div className="home">
      <section className="home__hero">
        <MapBackground />

        <div className="home__panel">
          <span className="hero-pill">B2B &amp; Campus</span>

          <h1 className="home__title">
            Partagez vos trajets quotidiens avec{" "}
            <span className="home__accent">CoShift</span>
          </h1>

          <p className="home__lead">
            La solution de covoiturage pensée pour les entreprises, les
            universités et les festivals. Réduisez votre empreinte carbone et
            vos frais de transport.
          </p>

          <TripSearchForm layout="hero" onSubmit={search} />
        </div>
      </section>
    </div>
  );
}
