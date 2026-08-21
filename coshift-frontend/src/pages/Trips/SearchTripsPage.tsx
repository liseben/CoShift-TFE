import { useState, useEffect, useCallback } from "react";
import { useSearchParams } from "react-router-dom";
import { FiSearch } from "react-icons/fi";
import axios from "axios";
import { API_BASE } from "../../config/api";
import TripSearchForm, {
  EMPTY_CRITERIA, type TripCriteria,
} from "../../components/TripSearchForm/TripSearchForm";
import { Alert, EmptyState, Spinner } from "../../components/ui";
import TripCard from "../../components/TripCard/TripCard";
import { useT } from "../../context/LangContext";
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

export default function SearchTripsPage() {
  const t = useT();
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
          t("trajets.erreurRecherche"),
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
        <h1>{t("trajets.trouverTitre")}</h1>
        <p className="trips__lead">{t("trajets.trouverAccroche")}</p>
      </header>

      <TripSearchForm initial={criteria} onSubmit={submit} loading={loading} />

      {error && <Alert tone="danger" onDismiss={() => setError(null)}>{error}</Alert>}

      {loading ? (
        <Spinner size="lg" center showLabel label={t("trajets.chargementRecherche")} />
      ) : trips === null ? (
        <EmptyState
          icon={<FiSearch />}
          title={t("trajets.lancezRecherche")}
          description={t("trajets.lancezRechercheTexte")}
        />
      ) : trips.length === 0 ? (
        <EmptyState
          icon={<FiSearch />}
          title={t("trajets.aucunResultat")}
          description={t("trajets.aucunResultatTexte")}
        />
      ) : (
        <>
          <p className="trips__count">
            {trips.length > 1
              ? t("trajets.disponible_plusieurs", { n: trips.length })
              : t("trajets.disponible_un", { n: trips.length })}
          </p>

          <div className="grid-auto">
            {trips.map((t) => (
              <TripCard key={t.uuid} trip={t} />
            ))}
          </div>
        </>
      )}
    </div>
  );
}
