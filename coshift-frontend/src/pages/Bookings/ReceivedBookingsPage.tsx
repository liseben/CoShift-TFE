import React, { useState, useEffect } from "react";
import { FaStar, FaInbox, FaPhoneAlt, FaUsers } from "react-icons/fa";
import { FiCheck, FiX } from "react-icons/fi";
import axios from "axios";
import { API_BASE } from "../../config/api";
import { statusOf } from "./bookingStatus";
import "./BookingsPage.css";

interface Received {
  uuid: string;
  seatsBooked: number;
  totalPrice: number;
  status: string;
  statusReason?: string;
  createdAt: string;
  passenger: {
    uuid: string;
    firstname: string;
    lastname: string;
    pictureUrl?: string;
    averageRating: number;
    tripsCount: number;
    phoneNumber?: string;
  };
}

/**
 * F19 / F20 — Le conducteur consulte les demandes reçues sur ses trajets,
 * puis les accepte ou les refuse. S'affiche comme onglet du tableau de bord.
 */
const ReceivedBookingsPage: React.FC = () => {
  const [items, setItems]   = useState<Received[]>([]);
  const [loading, setLoad]  = useState(true);
  const [error, setError]   = useState<string | null>(null);
  const [busy, setBusy]     = useState<string | null>(null);
  const [rejecting, setRejecting] = useState<string | null>(null);
  const [reason, setReason] = useState("");

  const headers = () => ({ Authorization: `Bearer ${localStorage.getItem("coshift_token") ?? ""}` });

  const load = async () => {
    try {
      const res = await axios.get(`${API_BASE}/api/bookings/received`, { headers: headers() });
      setItems(res.data);
    } catch (err: any) {
      setError(err.response?.data?.message ?? "Impossible de charger les demandes reçues.");
    } finally {
      setLoad(false);
    }
  };

  useEffect(() => { load(); }, []);

  const decide = async (uuid: string, action: "accept" | "reject", motif?: string) => {
    setBusy(uuid);
    setError(null);
    try {
      const res = await axios.patch(
        `${API_BASE}/api/bookings/${uuid}/${action}`,
        action === "reject" ? { reason: motif ?? "" } : {},
        { headers: headers() },
      );
      setItems((prev) => prev.map((b) => (b.uuid === uuid ? { ...b, ...res.data } : b)));
      setRejecting(null);
      setReason("");
    } catch (err: any) {
      setError(err.response?.data?.message ?? "L'opération a échoué.");
    } finally {
      setBusy(null);
    }
  };

  const pending = items.filter((b) => b.status === "PENDING").length;

  return (
    <div className="bk-embedded">

      <header className="bk-header">
        <div>
          <h2 className="bk-title-sm">Demandes reçues</h2>
          <p className="bk-subtitle">
            {pending > 0
              ? `${pending} demande${pending > 1 ? "s" : ""} en attente de votre réponse.`
              : "Les passagers qui ont demandé une place dans vos trajets."}
          </p>
        </div>
      </header>

      {error && <div className="bk-alert">{error}</div>}

      {loading ? (
        <div className="bk-loading"><div className="spinner" /></div>
      ) : items.length === 0 ? (
        <div className="bk-empty">
          <FaInbox size={40} className="bk-empty-icon" />
          <h3>Aucune demande pour l'instant</h3>
          <p>Les demandes de réservation sur vos trajets apparaîtront ici.</p>
        </div>
      ) : (
        <div className="bk-list">
          {items.map((b) => {
            const st = statusOf(b.status);
            return (
              <article className={`bk-card tone-${st.tone}`} key={b.uuid}>

                <div className="bk-card-head">
                  <span className={`bk-badge tone-${st.tone}`}>{st.label}</span>
                  <span className="bk-date">
                    <FaUsers size={11} /> {b.seatsBooked} place{b.seatsBooked > 1 ? "s" : ""} · {b.totalPrice.toFixed(2)} €
                  </span>
                </div>

                <div className="bk-body">
                  <div className="bk-person">
                    {b.passenger.pictureUrl
                      ? <img src={b.passenger.pictureUrl} alt="" className="bk-avatar" />
                      : <div className="bk-avatar-initial">{b.passenger.firstname.charAt(0)}</div>}
                    <div>
                      <p className="bk-person-name">{b.passenger.firstname} {b.passenger.lastname}</p>
                      <p className="bk-person-meta">
                        {b.passenger.averageRating > 0
                          ? <><FaStar size={10} style={{ color: "#fbbf24" }} /> {b.passenger.averageRating.toFixed(1)}</>
                          : "Nouveau passager"}
                        {" · "}{b.passenger.tripsCount} trajet{b.passenger.tripsCount !== 1 ? "s" : ""}
                      </p>
                    </div>
                  </div>
                </div>

                {b.statusReason && <p className="bk-reason">Motif : {b.statusReason}</p>}

                {/* Le serveur ne transmet le téléphone qu'après confirmation (F13bis). */}
                {b.passenger.phoneNumber && (
                  <a className="bk-contact" href={`tel:${b.passenger.phoneNumber}`}>
                    <FaPhoneAlt size={11} /> {b.passenger.phoneNumber}
                  </a>
                )}

                {b.status === "PENDING" && (
                  rejecting === b.uuid ? (
                    <div className="bk-reject-form">
                      <input
                        className="bk-input"
                        placeholder="Motif du refus (optionnel)"
                        value={reason}
                        maxLength={500}
                        onChange={(e) => setReason(e.target.value)}
                        autoFocus
                      />
                      <div className="bk-actions">
                        <button className="bk-btn-ghost" onClick={() => { setRejecting(null); setReason(""); }}>
                          Retour
                        </button>
                        <button
                          className="bk-btn-danger"
                          onClick={() => decide(b.uuid, "reject", reason)}
                          disabled={busy === b.uuid}
                        >
                          {busy === b.uuid ? "Envoi..." : "Confirmer le refus"}
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div className="bk-actions">
                      <button
                        className="bk-btn-ghost"
                        onClick={() => setRejecting(b.uuid)}
                        disabled={busy === b.uuid}
                      >
                        <FiX size={14} /> Refuser
                      </button>
                      <button
                        className="bk-btn-accept"
                        onClick={() => decide(b.uuid, "accept")}
                        disabled={busy === b.uuid}
                      >
                        <FiCheck size={14} /> {busy === b.uuid ? "..." : "Accepter"}
                      </button>
                    </div>
                  )
                )}
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default ReceivedBookingsPage;
