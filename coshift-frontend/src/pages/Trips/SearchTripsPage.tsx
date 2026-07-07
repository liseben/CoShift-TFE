import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { FaMapMarkerAlt, FaCalendarAlt, FaUsers, FaStar, FaCar, FaBolt, FaLeaf, FaGasPump } from "react-icons/fa";
import { FiSearch, FiArrowRight } from "react-icons/fi";
import axios from "axios";
import "./TripsPage.css";

const API_BASE = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

interface DriverSummary {
  uuid: string;
  firstname: string;
  lastname: string;
  pictureUrl?: string;
  averageRating: number;
  tripsCount: number;
}

interface VehicleSummary {
  brand: string;
  model: string;
  seats: number;
  energy: string;
  photoUrl?: string;
}

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
  driver: DriverSummary;
  vehicule: VehicleSummary;
}

const ENERGY_ICONS: Record<string, JSX.Element> = {
  ELECTRIC: <FaBolt style={{ color: "#34d399" }} />,
  HYBRID:   <FaLeaf style={{ color: "#60a5fa" }} />,
  GASOLINE: <FaGasPump style={{ color: "#fbbf24" }} />,
  DIESEL:   <FaGasPump style={{ color: "#f87171" }} />,
  LPG:      <FaGasPump style={{ color: "#a78bfa" }} />,
};

const SearchTripsPage: React.FC = () => {
  const navigate = useNavigate();
  const [form, setForm] = useState({ departure: "", arrival: "", date: "", seats: "" });
  const [trips, setTrips]         = useState<Trip[] | null>(null);
  const [loading, setLoading]     = useState(false);
  const [error, setError]         = useState<string | null>(null);

  const token = () => localStorage.getItem("coshift_token") ?? "";

  const handleSearch = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setLoading(true); setError(null);
    try {
      const params: Record<string, string> = {};
      if (form.departure) params.departure = form.departure;
      if (form.arrival)   params.arrival   = form.arrival;
      if (form.date)      params.date      = form.date;
      if (form.seats)     params.seats     = form.seats;

      const res = await axios.get(`${API_BASE}/api/trips/search`, {
        params,
        headers: { Authorization: `Bearer ${token()}` },
      });
      setTrips(res.data);
    } catch (err: any) {
      setError(err.response?.data?.message ?? "Une erreur est survenue lors de la recherche.");
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (iso: string) => {
    const d = new Date(iso);
    return d.toLocaleDateString("fr-FR", { weekday: "long", day: "numeric", month: "long" })
      + " à " + d.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" });
  };

  return (
    <div className="trips-container">
      <div className="trips-inner">

        {/* ── En-tête ── */}
        <div className="search-hero">
          <h1 className="trips-page-title">Trouver un trajet</h1>
          <p className="trips-page-subtitle">
            Recherchez parmi les trajets disponibles et réservez votre place.
          </p>

          {/* ── Formulaire de recherche ── */}
          <form onSubmit={handleSearch} className="search-bar-form">
            <div className="search-field">
              <FaMapMarkerAlt className="sf-icon" />
              <input className="sf-input" placeholder="Départ (ville)"
                value={form.departure} onChange={(e) => setForm({ ...form, departure: e.target.value })} />
            </div>
            <div className="search-divider">→</div>
            <div className="search-field">
              <FaMapMarkerAlt className="sf-icon" style={{ color: "#60a5fa" }} />
              <input className="sf-input" placeholder="Arrivée (ville)"
                value={form.arrival} onChange={(e) => setForm({ ...form, arrival: e.target.value })} />
            </div>
            <div className="search-field">
              <FaCalendarAlt className="sf-icon" />
              <input className="sf-input" type="date"
                value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} />
            </div>
            <div className="search-field narrow">
              <FaUsers className="sf-icon" />
              <input className="sf-input" type="number" min={1} max={8} placeholder="Places"
                value={form.seats} onChange={(e) => setForm({ ...form, seats: e.target.value })} />
            </div>
            <button type="submit" className="btn-search" disabled={loading}>
              <FiSearch size={16} />
              {loading ? "Recherche..." : "Rechercher"}
            </button>
          </form>
        </div>

        {error && <div className="trips-alert">{error}</div>}

        {/* ── Résultats ── */}
        {trips !== null && (
          <div className="search-results">
            <p className="results-count">
              {trips.length === 0
                ? "Aucun trajet disponible pour ces critères."
                : `${trips.length} trajet${trips.length > 1 ? "s" : ""} disponible${trips.length > 1 ? "s" : ""}`}
            </p>

            <div className="trip-cards">
              {trips.map((t) => (
                <div key={t.uuid} className="trip-card" onClick={() => navigate(`/trips/${t.uuid}`)}>

                  <div className="tc-route">
                    <div className="tc-city">
                      <span className="tc-dot dep" />
                      <div>
                        <p className="tc-city-name">{t.departureCity}</p>
                        {t.departureAddress && <p className="tc-address">{t.departureAddress}</p>}
                      </div>
                    </div>
                    <div className="tc-line" />
                    <div className="tc-city">
                      <span className="tc-dot arr" />
                      <div>
                        <p className="tc-city-name">{t.arrivalCity}</p>
                        {t.arrivalAddress && <p className="tc-address">{t.arrivalAddress}</p>}
                      </div>
                    </div>
                  </div>

                  <div className="tc-meta">
                    <span className="tc-date">📅 {formatDate(t.departureTime)}</span>
                  </div>

                  <div className="tc-footer">
                    <div className="tc-driver">
                      {t.driver.pictureUrl ? (
                        <img src={t.driver.pictureUrl} className="tc-avatar" alt="" />
                      ) : (
                        <div className="tc-avatar-initial">
                          {t.driver.firstname.charAt(0)}
                        </div>
                      )}
                      <div>
                        <p className="tc-driver-name">{t.driver.firstname} {t.driver.lastname}</p>
                        <p className="tc-driver-rating">
                          {t.driver.averageRating > 0
                            ? <><FaStar size={10} style={{ color: "#fbbf24" }} /> {t.driver.averageRating.toFixed(1)}</>
                            : "Nouveau conducteur"}
                        </p>
                      </div>
                    </div>

                    <div className="tc-right">
                      <div className="tc-vehicle">
                        {ENERGY_ICONS[t.vehicule.energy] ?? <FaCar />}
                        <span>{t.vehicule.brand} {t.vehicule.model}</span>
                      </div>
                      <div className="tc-seats">
                        <FaUsers size={11} /> {t.availableSeats} place{t.availableSeats > 1 ? "s" : ""}
                      </div>
                    </div>

                    <div className="tc-price-block">
                      <span className="tc-price">{t.pricePerSeat.toFixed(2)} €</span>
                      <span className="tc-price-label">/ place</span>
                    </div>

                    <button className="tc-cta" onClick={(e) => { e.stopPropagation(); navigate(`/trips/${t.uuid}`); }}>
                      Voir <FiArrowRight size={13} />
                    </button>
                  </div>

                  {/* Préférences */}
                  <div className="tc-prefs">
                    {t.acceptsLuggage   && <span className="pref-chip">🧳 Bagages</span>}
                    {t.acceptsPets      && <span className="pref-chip">🐾 Animaux</span>}
                    {t.musicAllowed     && <span className="pref-chip">🎵 Musique</span>}
                    {t.talkingAllowed   && <span className="pref-chip">💬 Discussion</span>}
                  </div>

                </div>
              ))}
            </div>
          </div>
        )}

      </div>
    </div>
  );
};

export default SearchTripsPage;
