import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { FiArrowRight, FiSearch } from "react-icons/fi";
import { FaLeaf, FaUsers, FaShieldAlt } from "react-icons/fa";
import axios from "axios";
import MapBackground from "../../components/MapBackground";
import TripSearchForm, { type TripCriteria } from "../../components/TripSearchForm/TripSearchForm";
import TripCard, { type TripSummary } from "../../components/TripCard/TripCard";
import { Button, EmptyState, Spinner } from "../../components/ui";
import { useSeo, useDonneesStructurees } from "../../hooks/useSeo";
import { useAuth } from "../../context/AuthContext";
import { useT } from "../../context/LangContext";
import { PHOTOS } from "../../components/image_site";
import { API_BASE } from "../../config/api";
import "./HomePage.css";

const PAR_PAGE = 6;

/* Icone et clef de traduction : le texte lui-meme vit dans le catalogue. Un
   tableau de constantes de module est evalue au chargement du fichier, avant
   que le contexte de langue existe. */
const ATOUTS = [
  { icon: <FaUsers />,      cle: "entreCollegues" },
  { icon: <FaLeaf />,       cle: "voitureDeMoins" },
  { icon: <FaShieldAlt />,  cle: "adresseVerifiee" },
];

/**
 * Description de l'organisation au format schema.org.
 *
 * Ce bloc ne change pas l'affichage : il donne à un moteur de quoi rattacher le
 * site à une entité nommée plutôt qu'à une simple suite de pages. C'est ce qui
 * permet, à terme, l'affichage d'un panneau de connaissance.
 */
const ORGANISATION = {
  "@context": "https://schema.org",
  "@type": "Organization",
  name: "CoShift",
  description:
    "Plateforme de covoiturage pour les entreprises, les hautes écoles et les organisateurs d'événements.",
  url: "https://coshift.be",
  areaServed: { "@type": "Country", name: "Belgique" },
  knowsLanguage: "fr-BE",
};

export default function HomePage() {
  /* Déclaré avant `useSeo` : les métadonnées de la page se traduisent aussi,
     et une constante de bloc ne s'utilise pas avant sa déclaration. */
  const t = useT();

  useSeo({
    titre: t("accueil.titre"),
    description: t("accueil.description"),
    chemin: "/",
  });
  useDonneesStructurees(ORGANISATION);

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
          <span className="hero-pill">{t("accueil.heroPastille")}</span>

          <h1 className="home__title">
            {t("accueil.heroTitre")}{" "}
            <span className="home__accent">CoShift</span>
          </h1>

          <p className="home__lead">{t("accueil.accroche")}</p>

          <TripSearchForm layout="hero" onSubmit={search} />
        </div>
      </section>

      {/* ── Trajets disponibles ── */}
      <section className="container container--wide home__section">
        <header className="home__section-head">
          <div>
            <h2>{t("accueil.trajetsDisponibles")}</h2>
            <p className="home__section-lead">
              {user ? t("accueil.prochainsDeparts") : t("accueil.connectezVous")}
            </p>
          </div>
          {user && total > 0 && (
            <Button variant="secondary" to="/trips/search" icon={<FiSearch />}>
              {t("accueil.rechercheDetaillee")}
            </Button>
          )}
        </header>

        {!user ? (
          <div className="home__locked">
            <p className="home__locked-text">{t("accueil.invite")}</p>
            <div className="home__locked-actions">
              <Button to="/register" size="lg">{t("commun.creerCompte")}</Button>
              <Button to="/login" variant="secondary" size="lg">{t("commun.seConnecter")}</Button>
            </div>
          </div>
        ) : loading ? (
          <Spinner size="lg" center showLabel label={t("accueil.chargementTrajets")} />
        ) : total === 0 ? (
          <EmptyState
            icon={<FiSearch />}
            title={t("accueil.aucunTrajet")}
            description={t("accueil.aucunTrajetTexte")}
            action={<Button to="/trips/create">{t("pied.proposerTrajet")}</Button>}
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
              <nav className="home__pagination" aria-label={t("accueil.paginationLabel")}>
                <Button variant="secondary" size="sm" disabled={page === 1}
                        onClick={() => setPage((p) => p - 1)}>
                  {t("commun.precedent")}
                </Button>
                <p aria-live="polite">
                  {t("accueil.pagination", { page, total: totalPages })}
                  <span className="home__count">
                    {t("accueil.paginationTrajets", { n: total })}
                  </span>
                </p>
                <Button variant="secondary" size="sm" disabled={page === totalPages}
                        onClick={() => setPage((p) => p + 1)}>
                  {t("commun.suivant")}
                </Button>
              </nav>
            )}
          </>
        )}
      </section>

      {/* ── Bande illustrée ── */}
      <section className="home__showcase">
        <div className="container container--wide home__showcase-grid">
          <figure className="home__showcase-figure">
            <img
              src={PHOTOS.depart.src}
              alt={PHOTOS.depart.alt}
              loading="lazy"
              decoding="async"
            />
          </figure>
          <div className="home__showcase-text">
            <h2>{t("accueil.argumentTitre")}</h2>
            <p>{t("accueil.argumentP1")}</p>
            <p>{t("accueil.argumentP2")}</p>
            <Button to="/trips/search" icon={<FiArrowRight />}>
              {t("accueil.voirTrajets")}
            </Button>
          </div>
        </div>
      </section>

      {/* ── Pourquoi CoShift ── */}
      <section className="home__why">
        <div className="container container--wide">
          <h2 className="home__why-title">{t("atouts.titre")}</h2>
          <div className="home__atouts">
            {ATOUTS.map((a) => (
              <article key={a.cle} className="home__atout">
                <span className="home__atout-icon" aria-hidden="true">{a.icon}</span>
                <h3>{t(`accueil.${a.cle}`)}</h3>
                <p>{t(`accueil.${a.cle}Texte`)}</p>
              </article>
            ))}
          </div>
          <p className="home__why-cta">
            <Button to="/a-propos" variant="ghost" icon={<FiArrowRight />}>
              {t("accueil.enSavoirPlus")}
            </Button>
          </p>
        </div>
      </section>
    </div>
  );
}
