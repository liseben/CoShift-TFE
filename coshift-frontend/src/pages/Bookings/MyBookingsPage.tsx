import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { FaStar, FaTicketAlt, FaPhoneAlt, FaCar } from "react-icons/fa";
import { FiArrowRight, FiSearch } from "react-icons/fi";
import axios from "axios";
import { API_BASE } from "../../config/api";
import { statusOf, formatTripDate } from "./bookingStatus";
import "./BookingsPage.css";

interface Booking {
  uuid: string;
  seatsBooked: number;
  totalPrice: number;
  status: string;
  statusReason?: string;
  createdAt: string;
  trip: {
    uuid: string;
    departureCity: string;
    departureAddress?: string;
    arrivalCity: string;
    arrivalAddress?: string;
    departureTime: string;
    pricePerSeat: number;
    driverFirstname: string;
    driverLastname: string;
    driverPictureUrl?: string;
    driverAverageRating: number;
    driverPhoneNumber?: string;
    vehiculeBrand: string;
    vehiculeModel: string;
  };
}

/** F30 — Le passager consulte et gère ses réservations. */
const MyBookingsPage: React.FC = () => {
  const navigate = useNavigate();
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading]   = useState(true);
  const [error, setError]       = useState<string | null>(null);
  const [busy, setBusy]         = useState<string | null>(null);

  const headers = () => ({ Authorization: `Bearer ${localStorage.getItem("coshift_token") ?? ""}` });

  const load = async () => {
    try {
      const res = await axios.get(`${API_BASE}/api/bookings/mine`, { headers: headers() });
      setBookings(res.data);
    } catch (err: any) {
      setError(err.response?.data?.message ?? "Impossible de charger vos réservations.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const cancel = async (uuid: string) => {
    if (!window.confirm("Annuler cette réservation ?")) return;
    setBusy(uuid);
    setError(null);
    try {
      const res = await axios.patch(`${API_BASE}/api/bookings/${uuid}/cancel`, {}, { headers: headers() });
      setBookings((prev) => prev.map((b) => (b.uuid === uuid ? { ...b, ...res.data } : b)));
    } catch (err: any) {
      setError(err.response?.data?.message ?? "L'annulation a échoué.");
    } finally {
      setBusy(null);
    }
  };

  const isCancellable = (b: Booking) =>
    (b.status === "PENDING" || b.status === "CONFIRMED") &&
    new Date(b.trip.departureTime) > new Date();

  return (
    <div className="bk-container">
      <div className="bk-inner">

        <header className="bk-header">
          <div>
            <h1 className="bk-title">Mes réservations</h1>
            <p className="bk-subtitle">Vos demandes de place et leur suivi.</p>
          </div>
          <button className="bk-btn-primary" onClick={() => navigate("/trips/search")}>
            <FiSearch size={15} /> Trouver un trajet
          </button>
        </header>

        {error && <div className="bk-alert">{error}</div>}

        {loading ? (
          <div className="bk-loading"><div className="spinner" /></div>
        ) : bookings.length === 0 ? (
          <div className="bk-empty">
            <FaTicketAlt size={42} className="bk-empty-icon" />
            <h3>Aucune réservation</h3>
            <p>Vous n'avez pas encore réservé de place. Cherchez un trajet pour commencer.</p>
            <button className="bk-btn-primary" onClick={() => navigate("/trips/search")}>
              <FiSearch size={15} /> Trouver un trajet
            </button>
          </div>
        ) : (
          <div className="bk-list">
            {bookings.map((b) => {
              const st = statusOf(b.status);
              return (
                <article className={`bk-card tone-${st.tone}`} key={b.uuid}>

                  <div className="bk-card-head">
                    <span className={`bk-badge tone-${st.tone}`}>{st.label}</span>
                    <span className="bk-date">{formatTripDate(b.trip.departureTime)}</span>
                  </div>

                  <div className="bk-route">
                    <span className="bk-city">{b.trip.departureCity}</span>
                    <FiArrowRight size={14} className="bk-arrow" />
                    <span className="bk-city">{b.trip.arrivalCity}</span>
                  </div>

                  {b.statusReason && (
                    <p className="bk-reason">Motif : {b.statusReason}</p>
                  )}

                  <div className="bk-body">
                    <div className="bk-person">
                      {b.trip.driverPictureUrl
                        ? <img src={b.trip.driverPictureUrl} alt="" className="bk-avatar" />
                        : <div className="bk-avatar-initial">{b.trip.driverFirstname.charAt(0)}</div>}
                      <div>
                        <p className="bk-person-name">{b.trip.driverFirstname} {b.trip.driverLastname}</p>
                        <p className="bk-person-meta">
                          {b.trip.driverAverageRating > 0
                            ? <><FaStar size={10} style={{ color: "#fbbf24" }} /> {b.trip.driverAverageRating.toFixed(1)}</>
                            : "Nouveau conducteur"}
                          {" · "}
                          <FaCar size={10} /> {b.trip.vehiculeBrand} {b.trip.vehiculeModel}
                        </p>
                      </div>
                    </div>

                    <div className="bk-figures">
                      <span className="bk-seats">{b.seatsBooked} place{b.seatsBooked > 1 ? "s" : ""}</span>
                      <span className="bk-price">{b.totalPrice.toFixed(2)} €</span>
                    </div>
                  </div>

                  {/* Le téléphone n'arrive du serveur qu'une fois la réservation confirmée (F13bis). */}
                  {b.trip.driverPhoneNumber && (
                    <a className="bk-contact" href={`tel:${b.trip.driverPhoneNumber}`}>
                      <FaPhoneAlt size={11} /> {b.trip.driverPhoneNumber}
                    </a>
                  )}

                  <div className="bk-actions">
                    <button className="bk-btn-ghost" onClick={() => navigate(`/trips/${b.trip.uuid}`)}>
                      Voir le trajet
                    </button>
                    {isCancellable(b) && (
                      <button
                        className="bk-btn-danger"
                        onClick={() => cancel(b.uuid)}
                        disabled={busy === b.uuid}
                      >
                        {busy === b.uuid ? "Annulation..." : "Annuler"}
                      </button>
                    )}
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};

export default MyBookingsPage;
