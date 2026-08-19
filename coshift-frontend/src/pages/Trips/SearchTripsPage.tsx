import { useState, useEffect, useCallback, type ReactElement } from "react";
import { useSearchParams } from "react-router-dom";
import { FaStar, FaBolt, FaLeaf, FaGasPump, FaSuitcase, FaDog, FaMusic } from "react-icons/fa";
import { FiArrowRight, FiSearch } from "react-icons/fi";
import axios from "axios";
import { API_BASE } from "../../config/api";
import TripSearchForm, {
  EMPTY_CRITERIA, type TripCriteria,
} from "../../components/TripSearchForm/TripSearchForm";
import { Alert, Avatar, Card, EmptyState, Spinner } from "../../components/ui";
import "./TripsPage.css";

interface Trip {
  uuid: string;
  departureCity: string;
  departureAddress?: string;
  arrivalCity: string;
  arrivalAddress?: string;
  departureTime: string;
  availableSeats: number;
  pricePerSeat: number;
  description?: string;
  acceptsLuggage: boolean;
  acceptsPets: boolean;
  musicAllowed: boolean;
  talkingAllowed: boolean;
  driver: {
    uuid: string;
    firstname: string;
    lastname: string;
    pictureUrl?: string;
    averageRating: number;
    tripsCount: number;
  };
  vehicule: { brand: string; model: string; seats: number; energy: string };
}

const ENERGY: Record<string, { icon: ReactElement; label: string }> = {
  ELECTRIC: { icon: <FaBolt />, label: "Électrique" },
  HYBRID:   { icon: <FaLeaf />, label: "Hybride" },
  GASOLINE: { icon: <FaGasPump />, label: "Essence" },
  DIESEL:   { icon: <FaGasPump />, label: "Diesel" },
  LPG:      { icon: <FaGasPump />, label: "GPL" },
};

const formatDate = (iso: string) => {
  const d = new Date(iso);
  return (
    d.toLocaleDateString("fr-FR", { weekday: "long", day: "numeric", month: "long" }) +
    " à " +
    d.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" })
  );
};

export default function SearchTripsPage() {
  const [params, setParams] = useSearchParams();
  const [trips, setTrips] = useState<Trip[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /* Les critères vivent dans l'URL : la recherche est partageable, et le
     retour arrière du navigateur redonne les résultats précédents. */
  const criteria: TripCriteria = {
    ...EMPTY_CRITERIA,
    ...Object.fromEntries(params.entries()),
  };

  const run = useCallback(async (c: TripCriteria) => {
    setLoading(true);
    setError(null);
    try {
      const query: Record<string, string> = {};
      if (c.departure) query.departure = c.departure;
      if (c.arrival) query.arrival = c.arrival;
      if (c.date) query.date = c.date;
      if (c.seats) query.seats = c.seats;

      const res = await axios.get<Trip[]>(`${API_BASE}/api/trips/search`, {
        params: query,
        headers: { Authorization: `Bearer ${localStorage.getItem("coshift_token") ?? ""}` },
      });

      /* L'API ne filtre que par jour. L'heure choisie sert de borne basse,
         appliquée ici pour que le champ ne soit pas décoratif. */
      const filtered = c.time
        ? res.data.filter((t) => {
            const d = new Date(t.departureTime);
            const mins = d.getHours() * 60 + d.getMinutes();
            const [h, m] = c.time.split(":").map(Number);
            return mins >= h * 60 + m;
          })
        : res.data;

      setTrips(filtered);
    } catch (err) {
      setError(
        (axios.isAxiosError(err) && err.response?.data?.message) ||
          "Une erreur est survenue lors de la recherche.",
      );
    } finally {
      setLoading(false);
    }
  }, []);

  // Une recherche arrivant depuis l'accueil se lance d'elle-même.
  useEffect(() => {
    if (params.toString()) run(criteria);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const submit = (c: TripCriteria) => {
    setParams(new URLSearchParams(Object.entries(c).filter(([, v]) => v !== "")));
    run(c);
  };

  return (
    <div className="container page stack-8">
      <header>
        <h1>Trouver un trajet</h1>
        <p className="trips__lead">
          Recherchez parmi les trajets disponibles et réservez votre place.
        </p>
      </header>

      <TripSearchForm initial={criteria} onSubmit={submit} loading={loading} />

      {error && <Alert tone="danger" onDismiss={() => setError(null)}>{error}</Alert>}

      {loading ? (
        <Spinner size="lg" center showLabel label="Recherche des trajets" />
      ) : trips === null ? (
        <EmptyState
          icon={<FiSearch />}
          title="Lancez une recherche"
          description="Indiquez au moins une ville de départ ou d'arrivée pour voir les trajets disponibles."
        />
      ) : trips.length === 0 ? (
        <EmptyState
          icon={<FiSearch />}
          title="Aucun trajet pour ces critères"
          description="Élargissez la date ou l'heure de départ, ou retirez le filtre sur le nombre de places."
        />
      ) : (
        <>
          <p className="trips__count">
            {trips.length} trajet{trips.length > 1 ? "s" : ""} disponible
            {trips.length > 1 ? "s" : ""}
          </p>

          <div className="grid-auto">
            {trips.map((t) => {
              const energy = ENERGY[t.vehicule.energy];
              const name = `${t.driver.firstname} ${t.driver.lastname}`;
              return (
                <Card
                  key={t.uuid}
                  to={`/trips/${t.uuid}`}
                  title={
                    <span className="trips__route">
                      {t.departureCity}
                      <FiArrowRight aria-hidden="true" />
                      {t.arrivalCity}
                    </span>
                  }
                  action={<span className="trips__price">{t.pricePerSeat.toFixed(2)} €</span>}
                >
                  <p className="trips__when">{formatDate(t.departureTime)}</p>

                  <div className="trips__driver">
                    <Avatar src={t.driver.pictureUrl} name={name} size="sm" />
                    <div>
                      <p className="trips__driver-name">{name}</p>
                      <p className="trips__meta">
                        {t.driver.averageRating > 0 ? (
                          <>
                            <FaStar aria-hidden="true" className="trips__star" />
                            {t.driver.averageRating.toFixed(1)}
                          </>
                        ) : (
                          "Nouveau conducteur"
                        )}
                        {" · "}
                        {t.vehicule.brand} {t.vehicule.model}
                      </p>
                    </div>
                  </div>

                  <ul className="trips__tags">
                    <li className="trips__tag trips__tag--seats">
                      {t.availableSeats} place{t.availableSeats > 1 ? "s" : ""}
                    </li>
                    {energy && (
                      <li className="trips__tag">
                        <span aria-hidden="true">{energy.icon}</span> {energy.label}
                      </li>
                    )}
                    {t.acceptsLuggage && (
                      <li className="trips__tag"><FaSuitcase aria-hidden="true" /> Bagages</li>
                    )}
                    {t.acceptsPets && (
                      <li className="trips__tag"><FaDog aria-hidden="true" /> Animaux</li>
                    )}
                    {t.musicAllowed && (
                      <li className="trips__tag"><FaMusic aria-hidden="true" /> Musique</li>
                    )}
                  </ul>

                  {/* Pas de bouton ici : la carte entière est déjà le lien.
                      Un <button> dans un <a> est invalide et double la
                      tabulation. */}
                  <p className="trips__cta" aria-hidden="true">
                    Voir le trajet <FiArrowRight />
                  </p>
                </Card>
              );
            })}
          </div>
        </>
      )}
    </div>
  );
}
