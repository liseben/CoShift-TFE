import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { FiArrowRight, FiSearch } from "react-icons/fi";
import { FaLeaf, FaUsers, FaShieldAlt } from "react-icons/fa";
import axios from "axios";
import MapBackground from "../../components/MapBackground";
import TripSearchForm, { type TripCriteria } from "../../components/TripSearchForm/TripSearchForm";
import TripCard, { type TripSummary } from "../../components/TripCard/TripCard";
import { Button, EmptyState, Spinner } from "../../components/ui";
import { useAuth } from "../../context/AuthContext";
import { API_BASE } from "../../config/api";
import "./HomePage.css";

const PAR_PAGE = 6;

const ATOUTS = [
  {
    icon: <FaUsers />,
    title: "Entre collègues",
    text: "Vous partagez la route avec des personnes de votre organisation, pas avec des inconnus croisés sur Internet.",
  },
  {
    icon: <FaLeaf />,
    title: "Une voiture de moins",
    text: "Chaque place partagée retire un véhicule des embouteillages du matin et divise les frais d'autant.",
  },
  {
    icon: <FaShieldAlt />,
    title: "Adresse vérifiée",
    text: "L'inscription passe par votre e-mail professionnel : c'est lui qui vous rattache à votre organisation.",
  },
];

export default function HomePage() {
  const navigate = useNavigate();
  const { user } = useAuth();

  const [trips, setTrips] = useState<TripSummary[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);

  /* Les trajets ne sont visibles qu'une fois connecté : ils portent le nom
     du conducteur et son horaire quotidien. Publier cela ouvertement
     reviendrait à exposer les déplacements de personnes identifiables. */
  useEffect(() => {
    if (!user) return;
    setLoading(true);
    axios
      .get<TripSummary[]>(`${API_BASE}/api/trips/search`, {
        headers: { Authorization: `Bearer ${localStorage.getItem("coshift_token") ?? ""}` },
      })
      .then((r) => setTrips(r.data))
      .catch(() => setTrips([]))
      .finally(() => setLoading(false));
  }, [user]);

  const search = (c: TripCriteria) => {
    const params = new URLSearchParams(Object.entries(c).filter(([, v]) => v !== ""));
    navigate(`/trips/search?${params.toString()}`);
  };

  const total = trips?.length ?? 0;
  const totalPages = Math.max(1, Math.ceil(total / PAR_PAGE));
  const visible = trips?.slice((page - 1) * PAR_PAGE, page * PAR_PAGE) ?? [];

  return (
    <div className="home">
      {/* ── Hero ── */}
      <section className="home__hero">
        <MapBackground />

        <div className="home__panel">
          <span className="hero-pill">B2B &amp; Campus</span>

          <h1 className="home__title">
            Partagez vos trajets quotidiens avec{" "}
            <span className="home__accent">CoShift</span>
          </h1>

          <p className="home__lead">
            Le covoiturage pensé pour les entreprises, les universités et les
            événements. Moins de voitures sur la route, moins de frais, et des
            trajets faits avec des collègues.
          </p>

          <TripSearchForm layout="hero" onSubmit={search} />
        </div>
      </section>

      {/* ── Trajets disponibles ── */}
      <section className="container container--wide home__section">
        <header className="home__section-head">
          <div>
            <h2>Trajets disponibles</h2>
            <p className="home__section-lead">
              {user
                ? "Les prochains départs proposés par les membres."
                : "Connectez-vous pour voir les trajets proposés près de chez vous."}
            </p>
          </div>
          {user && total > 0 && (
            <Button variant="secondary" to="/trips/search" icon={<FiSearch />}>
              Recherche détaillée
            </Button>
          )}
        </header>

        {!user ? (
          <div className="home__locked">
            <p className="home__locked-text">
              Les trajets affichent le nom du conducteur et son horaire. Ils ne
              sont visibles qu'entre membres connectés.
            </p>
            <div className="home__locked-actions">
              <Button to="/register" size="lg">Créer un compte</Button>
              <Button to="/login" variant="secondary" size="lg">Se connecter</Button>
            </div>
          </div>
        ) : loading ? (
          <Spinner size="lg" center showLabel label="Chargement des trajets" />
        ) : total === 0 ? (
          <EmptyState
            icon={<FiSearch />}
            title="Aucun trajet publié pour le moment"
            description="Soyez le premier à proposer le vôtre — vos collègues le verront ici."
            action={<Button to="/trips/create">Proposer un trajet</Button>}
          />
        ) : (
          <>
            <div className="grid-auto">
              {visible.map((t) => (
                <TripCard key={t.uuid} trip={t} />
              ))}
            </div>

            {/* La pagination n'apparaît qu'au-delà d'une page. */}
            {totalPages > 1 && (
              <nav className="home__pagination" aria-label="Pages de trajets">
                <Button variant="secondary" size="sm" disabled={page === 1}
                        onClick={() => setPage((p) => p - 1)}>
                  Précédent
                </Button>
                <p aria-live="polite">
                  Page {page} sur {totalPages}
                  <span className="home__count"> · {total} trajets</span>
                </p>
                <Button variant="secondary" size="sm" disabled={page === totalPages}
                        onClick={() => setPage((p) => p + 1)}>
                  Suivant
                </Button>
              </nav>
            )}
          </>
        )}
      </section>

      {/* ── Pourquoi CoShift ── */}
      <section className="home__why">
        <div className="container container--wide">
          <h2 className="home__why-title">Pourquoi passer par CoShift</h2>
          <div className="home__atouts">
            {ATOUTS.map((a) => (
              <article key={a.title} className="home__atout">
                <span className="home__atout-icon" aria-hidden="true">{a.icon}</span>
                <h3>{a.title}</h3>
                <p>{a.text}</p>
              </article>
            ))}
          </div>
          <p className="home__why-cta">
            <Button to="/a-propos" variant="ghost" icon={<FiArrowRight />}>
              En savoir plus sur CoShift
            </Button>
          </p>
        </div>
      </section>
    </div>
  );
}
