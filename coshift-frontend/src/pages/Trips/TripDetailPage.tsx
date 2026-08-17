import React, { useState, useEffect, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  FaStar, FaUsers, FaCar, FaBolt, FaLeaf, FaGasPump, FaEuroSign,
} from "react-icons/fa";
import { FiArrowLeft, FiCheck, FiClock, FiMapPin } from "react-icons/fi";
import axios from "axios";
import { API_BASE } from "../../config/api";
import { useAuth } from "../../context/AuthContext";
import "./TripDetailPage.css";

const ENERGY_LABELS: Record<string, { label: string; icon: React.ReactElement }> = {
  ELECTRIC: { label: "Électrique", icon: <FaBolt /> },
  HYBRID:   { label: "Hybride",    icon: <FaLeaf /> },
  GASOLINE: { label: "Essence",    icon: <FaGasPump /> },
  DIESEL:   { label: "Diesel",     icon: <FaGasPump /> },
  LPG:      { label: "GPL",        icon: <FaGasPump /> },
};

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
  status: string;
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
  vehicule: { brand: string; model: string; seats: number; energy: string; photoUrl?: string };
}

/** F26 — Détail d'un trajet, et point d'entrée de la réservation (F27). */
const TripDetailPage: React.FC = () => {
  const { uuid } = useParams<{ uuid: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [trip, setTrip]       = useState<Trip | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState<string | null>(null);
  const [seats, setSeats]     = useState(1);
  const [booking, setBooking] = useState(false);
  const [success, setSuccess] = useState(false);

  const headers = () => ({ Authorization: `Bearer ${localStorage.getItem("coshift_token") ?? ""}` });

  const loadTrip = useCallback(async () => {
    try {
      const res = await axios.get(`${API_BASE}/api/trips/${uuid}`, { headers: headers() });
      setTrip(res.data);
    } catch (err: any) {
      setError(err.response?.data?.message ?? "Ce trajet est introuvable.");
    } finally {
      setLoading(false);
    }
  }, [uuid]);

  useEffect(() => { loadTrip(); }, [loadTrip]);

  const handleBooking = async () => {
    setBooking(true);
    setError(null);
    try {
      await axios.post(`${API_BASE}/api/bookings`,
        { tripUuid: uuid, seatsBooked: seats },
        { headers: headers() });
      setSuccess(true);
      // Le conducteur doit encore accepter : on rafraîchit pour refléter l'état réel.
      await loadTrip();
      setTimeout(() => navigate("/bookings"), 2200);
    } catch (err: any) {
      setError(err.response?.data?.message ?? "La réservation n'a pas pu être enregistrée.");
    } finally {
      setBooking(false);
    }
  };

  const formatDate = (iso: string) =>
    new Date(iso).toLocaleDateString("fr-FR", {
      weekday: "long", day: "numeric", month: "long", year: "numeric",
    });

  const formatTime = (iso: string) =>
    new Date(iso).toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" });

  if (loading) {
    return <div className="td-container"><div className="td-loading"><div className="spinner" /></div></div>;
  }

  if (!trip) {
    return (
      <div className="td-container">
        <div className="td-inner">
          <div className="td-alert">{error ?? "Ce trajet est introuvable."}</div>
          <button className="btn-back" onClick={() => navigate("/trips/search")}>
            <FiArrowLeft size={16} /> Retour à la recherche
          </button>
        </div>
      </div>
    );
  }

  const isOwnTrip  = user?.email !== undefined && trip.driver.firstname === user?.firstname
                     && trip.driver.lastname === user?.lastname;
  const isBookable = trip.status === "PLANNED" && trip.availableSeats > 0 && !isOwnTrip;
  const energy     = ENERGY_LABELS[trip.vehicule.energy] ?? { label: trip.vehicule.energy, icon: <FaCar /> };
  const total      = (trip.pricePerSeat * seats).toFixed(2);

  return (
    <div className="td-container">
      <div className="td-inner">

        <button className="btn-back" onClick={() => navigate(-1)}>
          <FiArrowLeft size={16} /> Retour
        </button>

        {success && (
          <div className="td-success">
            <FiCheck size={20} />
            Demande envoyée ! Le conducteur doit maintenant l'accepter. Redirection vers vos réservations...
          </div>
        )}
        {error && !success && <div className="td-alert">{error}</div>}

        <div className="td-grid">

          {/* ── Colonne principale ── */}
          <div className="td-main">

            <div className="td-card td-route-card">
              <p className="td-date">
                <FiClock size={13} /> {formatDate(trip.departureTime)}
              </p>

              <div className="td-route">
                <div className="td-stop">
                  <span className="td-time">{formatTime(trip.departureTime)}</span>
                  <span className="td-dot dep" />
                  <div className="td-place">
                    <p className="td-city">{trip.departureCity}</p>
                    {trip.departureAddress && <p className="td-address"><FiMapPin size={11} /> {trip.departureAddress}</p>}
                  </div>
                </div>

                <div className="td-line" />

                <div className="td-stop">
                  <span className="td-time" />
                  <span className="td-dot arr" />
                  <div className="td-place">
                    <p className="td-city">{trip.arrivalCity}</p>
                    {trip.arrivalAddress && <p className="td-address"><FiMapPin size={11} /> {trip.arrivalAddress}</p>}
                  </div>
                </div>
              </div>

              {trip.description && (
                <div className="td-description">
                  <p className="td-section-label">Précisions du conducteur</p>
                  <p>{trip.description}</p>
                </div>
              )}

              <div className="td-prefs">
                <span className={`td-chip ${trip.acceptsLuggage ? "on" : "off"}`}>🧳 Bagages {trip.acceptsLuggage ? "acceptés" : "refusés"}</span>
                <span className={`td-chip ${trip.acceptsPets ? "on" : "off"}`}>🐾 Animaux {trip.acceptsPets ? "acceptés" : "refusés"}</span>
                <span className={`td-chip ${trip.musicAllowed ? "on" : "off"}`}>🎵 Musique</span>
                <span className={`td-chip ${trip.talkingAllowed ? "on" : "off"}`}>💬 Discussion</span>
              </div>
            </div>

            <div className="td-card">
              <p className="td-section-label">Conducteur</p>
              <div className="td-driver">
                {trip.driver.pictureUrl
                  ? <img src={trip.driver.pictureUrl} alt="" className="td-avatar" />
                  : <div className="td-avatar-initial">{trip.driver.firstname.charAt(0)}</div>}
                <div>
                  <p className="td-driver-name">{trip.driver.firstname} {trip.driver.lastname}</p>
                  <p className="td-driver-meta">
                    {trip.driver.averageRating > 0
                      ? <><FaStar size={11} style={{ color: "#fbbf24" }} /> {trip.driver.averageRating.toFixed(1)} / 5 · </>
                      : <>Nouveau conducteur · </>}
                    {trip.driver.tripsCount} trajet{trip.driver.tripsCount !== 1 ? "s" : ""}
                  </p>
                </div>
              </div>
            </div>

            <div className="td-card">
              <p className="td-section-label">Véhicule</p>
              <div className="td-vehicle">
                {trip.vehicule.photoUrl
                  ? <img src={trip.vehicule.photoUrl} alt="" className="td-vehicle-photo" />
                  : <div className="td-vehicle-placeholder"><FaCar size={26} /></div>}
                <div>
                  <p className="td-vehicle-name">{trip.vehicule.brand} {trip.vehicule.model}</p>
                  <p className="td-vehicle-meta">
                    {energy.icon} {energy.label} · <FaUsers size={11} /> {trip.vehicule.seats} places
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* ── Panneau de réservation ── */}
          <aside className="td-aside">
            <div className="td-card td-booking-card">
              <div className="td-price-row">
                <span className="td-price">{trip.pricePerSeat.toFixed(2)} €</span>
                <span className="td-price-unit">/ place</span>
              </div>

              <p className="td-seats-left">
                {trip.availableSeats > 0
                  ? <><FaUsers size={12} /> {trip.availableSeats} place{trip.availableSeats > 1 ? "s" : ""} restante{trip.availableSeats > 1 ? "s" : ""}</>
                  : "Complet"}
              </p>

              {isBookable ? (
                <>
                  <label className="td-label">Nombre de places</label>
                  <div className="td-seat-picker">
                    <button type="button" onClick={() => setSeats((s) => Math.max(1, s - 1))} disabled={seats <= 1}>−</button>
                    <span>{seats}</span>
                    <button type="button" onClick={() => setSeats((s) => Math.min(trip.availableSeats, s + 1))} disabled={seats >= trip.availableSeats}>+</button>
                  </div>

                  <div className="td-total">
                    <span>Total</span>
                    <strong><FaEuroSign size={12} /> {total}</strong>
                  </div>

                  <button className="btn-book" onClick={handleBooking} disabled={booking || success}>
                    {booking ? "Envoi en cours..." : success ? "Demande envoyée" : "Réserver"}
                  </button>
                  <p className="td-hint">
                    Votre demande est envoyée au conducteur. Elle n'est confirmée qu'après son accord.
                  </p>
                </>
              ) : (
                <p className="td-unavailable">
                  {isOwnTrip
                    ? "Vous êtes le conducteur de ce trajet."
                    : trip.status !== "PLANNED"
                      ? "Ce trajet n'accepte plus de réservation."
                      : "Il ne reste plus de place disponible."}
                </p>
              )}
            </div>
          </aside>
        </div>
      </div>
    </div>
  );
};

export default TripDetailPage;
