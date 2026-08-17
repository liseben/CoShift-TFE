import React, { useState, useEffect } from "react";
import { Navigate, useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { FaCar, FaTicketAlt, FaLeaf, FaStar, FaInbox, FaUsers } from "react-icons/fa";
import { FiEdit3, FiX, FiCamera, FiPhone, FiGrid, FiTruck, FiArrowRight } from "react-icons/fi";
import axios from "axios";
import VehiclePage from "./VehiclePage";
import ReceivedBookingsPage from "../Bookings/ReceivedBookingsPage";
import { statusOf, formatTripDate } from "../Bookings/bookingStatus";
import "./DashboardPage.css";

import { API_BASE } from "../../config/api";

type TabKey = "overview" | "requests" | "vehicles";

interface MyTrip {
  uuid: string;
  departureCity: string;
  arrivalCity: string;
  departureTime: string;
  availableSeats: number;
  pricePerSeat: number;
  status: string;
}

interface MyBooking {
  uuid: string;
  status: string;
  seatsBooked: number;
  trip: { uuid: string; departureCity: string; arrivalCity: string; departureTime: string };
}

const DashboardPage: React.FC = () => {
  const { user, isLoading, login } = useAuth();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  // CreateTripPage redirige vers /dashboard?tab=vehicles : le paramètre était
  // jusqu'ici ignoré, l'utilisateur retombait toujours sur la vue d'ensemble.
  const tabFromUrl = searchParams.get("tab") as TabKey | null;
  const [activeTab, setActiveTab] = useState<TabKey>(
    tabFromUrl && ["overview", "requests", "vehicles"].includes(tabFromUrl) ? tabFromUrl : "overview",
  );

  const [myTrips, setMyTrips]       = useState<MyTrip[]>([]);
  const [myBookings, setMyBookings] = useState<MyBooking[]>([]);
  const [pendingCount, setPending]  = useState(0);
  const [loadingData, setLoadingData] = useState(true);

  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editFirstname, setEditFirstname] = useState(user?.firstname ?? "");
  const [editLastname, setEditLastname]   = useState(user?.lastname  ?? "");
  const [editEmail, setEditEmail]         = useState(user?.email     ?? "");
  const [editPhone, setEditPhone]         = useState(user?.phoneNumber ?? "");
  const [isSaving, setIsSaving]           = useState(false);
  const [modalError, setModalError]       = useState<string | null>(null);
  const [isUploadingPhoto, setIsUploadingPhoto] = useState(false);

  // Les widgets affichaient un état vide codé en dur : aucune de ces données
  // n'était jamais demandée au serveur.
  useEffect(() => {
    if (!user) return;
    const headers = { Authorization: `Bearer ${localStorage.getItem("coshift_token") ?? ""}` };

    Promise.allSettled([
      axios.get(`${API_BASE}/api/trips/mine`, { headers }),
      axios.get(`${API_BASE}/api/bookings/mine`, { headers }),
      axios.get(`${API_BASE}/api/bookings/received`, { headers }),
    ]).then(([trips, bookings, received]) => {
      if (trips.status === "fulfilled") setMyTrips(trips.value.data);
      if (bookings.status === "fulfilled") setMyBookings(bookings.value.data);
      if (received.status === "fulfilled") {
        setPending(received.value.data.filter((b: MyBooking) => b.status === "PENDING").length);
      }
      setLoadingData(false);
    });
  }, [user]);

  const selectTab = (tab: TabKey) => {
    setActiveTab(tab);
    setSearchParams(tab === "overview" ? {} : { tab });
  };

  if (isLoading) {
    return (
      <div className="dashboard-loading">
        <div className="spinner" />
        <p>Chargement de votre espace...</p>
      </div>
    );
  }

  if (!user) return <Navigate to="/login" replace />;

  // ── Ouvrir la modale en synchronisant les champs avec les données actuelles ──
  const openModal = () => {
    setEditFirstname(user.firstname ?? "");
    setEditLastname(user.lastname  ?? "");
    setEditEmail(user.email        ?? "");
    setEditPhone(user.phoneNumber  ?? "");
    setModalError(null);
    setIsEditModalOpen(true);
  };

  // ── Enregistrer le profil ──
  const handleSaveProfile = async (e: { preventDefault(): void }) => {
    e.preventDefault();
    setIsSaving(true);
    setModalError(null);
    try {
      const res = await axios.put(
        `${API_BASE}/api/users/profile`,
        { firstname: editFirstname, lastname: editLastname, email: editEmail, phoneNumber: editPhone },
        { headers: { Authorization: `Bearer ${localStorage.getItem("coshift_token")}` } },
      );
      if (res.data?.token) login(res.data.token);
      setIsEditModalOpen(false);
    } catch (err: any) {
      setModalError(err.response?.data?.message ?? "Une erreur est survenue lors de la sauvegarde.");
    } finally {
      setIsSaving(false);
    }
  };

  // ── Upload de la photo de profil ──
  const handlePhotoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files?.length) return;
    const file = e.target.files[0];
    if (!["image/jpeg", "image/png"].includes(file.type)) {
      setModalError("Seuls les formats JPG et PNG sont acceptés.");
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      setModalError("La photo ne doit pas dépasser 2 Mo.");
      return;
    }
    setIsUploadingPhoto(true);
    setModalError(null);
    try {
      const formData = new FormData();
      formData.append("file", file);
      const res = await axios.post(`${API_BASE}/api/users/photo`, formData, {
        headers: {
          Authorization: `Bearer ${localStorage.getItem("coshift_token")}`,
          "Content-Type": "multipart/form-data",
        },
      });
      if (res.data?.pictureUrl) {
        // Rafraîchir le profil pour afficher la nouvelle photo
        login(localStorage.getItem("coshift_token") ?? "");
      }
    } catch (err: any) {
      setModalError(err.response?.data?.message ?? "Impossible d'uploader l'image.");
    } finally {
      setIsUploadingPhoto(false);
    }
  };

  const avatar = user.pictureUrl ? (
    <img src={user.pictureUrl} alt="Profil" className="profile-picture-large" />
  ) : (
    <div className="profile-initial-large">
      {user.firstname?.charAt(0).toUpperCase() ?? "U"}
    </div>
  );

  const modalAvatar = user.pictureUrl ? (
    <img src={user.pictureUrl} alt="Profil" className="modal-photo-preview" />
  ) : (
    <div className="modal-photo-preview placeholder">
      {user.firstname?.charAt(0).toUpperCase() ?? "U"}
    </div>
  );

  return (
    <div className="dashboard-container">
      <div className="dashboard-content">

        {/* ── EN-TÊTE DU PROFIL ── */}
        <div className="dashboard-header-card">
          <div className="profile-section">
            {avatar}
            <div className="profile-info">
              <h1>Bonjour, {user.firstname} {user.lastname} 👋</h1>
              <div className="profile-meta">
                <span className="profile-email">{user.email}</span>
                {user.phoneNumber && (
                  <>
                    <span className="profile-meta-sep">·</span>
                    <span className="profile-phone">
                      <FiPhone size={12} style={{ marginRight: 4, verticalAlign: "middle" }} />
                      {user.phoneNumber}
                    </span>
                  </>
                )}
              </div>
              <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                <span className="role-badge">
                  {user.role === "USER" ? "✦ Membre CoShift" : "⚡ Administrateur"}
                </span>
                {!user.emailVerified && (
                  <span className="badge-unverified">⚠ Email non vérifié</span>
                )}
              </div>
              <div className="profile-quick-stats">
                <span className="quick-stat">
                  <FaCar size={12} />
                  <span className="quick-stat-value">{user.tripsCount}</span> trajet{user.tripsCount !== 1 ? "s" : ""}
                </span>
                <span className="quick-stat">
                  <FaStar size={12} style={{ color: "#fbbf24" }} />
                  <span className="quick-stat-value">
                    {user.averageRating > 0 ? user.averageRating.toFixed(1) : "—"}
                  </span>
                  {user.averageRating > 0 && " / 5"}
                </span>
              </div>
            </div>
          </div>

          <div className="profile-actions">
            <button className="btn-outline-light" onClick={openModal}>
              <FiEdit3 size={15} />
              Modifier le profil
            </button>
          </div>
        </div>

        {/* ── ONGLETS ── */}
        <div className="dashboard-tabs">
          <button
            className={`tab-btn ${activeTab === "overview" ? "active" : ""}`}
            onClick={() => selectTab("overview")}
          >
            <FiGrid size={15} /> Vue d'ensemble
          </button>
          <button
            className={`tab-btn ${activeTab === "requests" ? "active" : ""}`}
            onClick={() => selectTab("requests")}
          >
            <FaInbox size={14} /> Demandes reçues
            {pendingCount > 0 && <span className="tab-badge">{pendingCount}</span>}
          </button>
          <button
            className={`tab-btn ${activeTab === "vehicles" ? "active" : ""}`}
            onClick={() => selectTab("vehicles")}
          >
            <FiTruck size={15} /> Mes véhicules
          </button>
        </div>

        {/* ── CONTENU PAR ONGLET ── */}
        {activeTab === "vehicles" ? (
          <VehiclePage />
        ) : activeTab === "requests" ? (
          <ReceivedBookingsPage />
        ) : (
        <div className="dashboard-grid">

          <div className="dashboard-widget">
            <div className="widget-header">
              <h3>
                <div className="icon-wrapper icon-car"><FaCar /></div>
                Mes trajets proposés
              </h3>
              {myTrips.length > 0 && (
                <button className="widget-action" onClick={() => navigate("/trips/create")}>
                  Nouveau trajet
                </button>
              )}
            </div>
            {loadingData ? (
              <div className="widget-body"><div className="widget-skeleton" /></div>
            ) : myTrips.length === 0 ? (
              <div className="widget-body empty-state">
                <p>Vous n'avez pas encore proposé de trajet pour vos collègues.</p>
                <button className="btn-primary-small" onClick={() => navigate("/trips/create")}>Proposer un trajet</button>
              </div>
            ) : (
              <div className="widget-body widget-list">
                {myTrips.slice(0, 4).map((t) => (
                  <button className="widget-row" key={t.uuid} onClick={() => navigate(`/trips/${t.uuid}`)}>
                    <div>
                      <p className="wr-title">{t.departureCity} → {t.arrivalCity}</p>
                      <p className="wr-meta">{formatTripDate(t.departureTime)}</p>
                    </div>
                    <div className="wr-right">
                      <span className={`wr-badge tone-${statusOf(t.status === "PLANNED" ? "CONFIRMED" : "COMPLETED").tone}`}>
                        <FaUsers size={10} /> {t.availableSeats}
                      </span>
                      <FiArrowRight size={14} />
                    </div>
                  </button>
                ))}
              </div>
            )}
          </div>

          <div className="dashboard-widget">
            <div className="widget-header">
              <h3>
                <div className="icon-wrapper icon-ticket"><FaTicketAlt /></div>
                Mes réservations
              </h3>
              {myBookings.length > 0 && (
                <button className="widget-action" onClick={() => navigate("/bookings")}>
                  Tout voir
                </button>
              )}
            </div>
            {loadingData ? (
              <div className="widget-body"><div className="widget-skeleton" /></div>
            ) : myBookings.length === 0 ? (
              <div className="widget-body empty-state">
                <p>Vous n'avez aucune réservation de covoiturage en cours.</p>
                <button className="btn-outline-small" onClick={() => navigate("/trips/search")}>Trouver un trajet</button>
              </div>
            ) : (
              <div className="widget-body widget-list">
                {myBookings.slice(0, 4).map((b) => {
                  const st = statusOf(b.status);
                  return (
                    <button className="widget-row" key={b.uuid} onClick={() => navigate("/bookings")}>
                      <div>
                        <p className="wr-title">{b.trip.departureCity} → {b.trip.arrivalCity}</p>
                        <p className="wr-meta">{formatTripDate(b.trip.departureTime)}</p>
                      </div>
                      <div className="wr-right">
                        <span className={`wr-badge tone-${st.tone}`}>{st.label}</span>
                        <FiArrowRight size={14} />
                      </div>
                    </button>
                  );
                })}
              </div>
            )}
          </div>

          <div className="dashboard-widget full-width">
            <div className="widget-header">
              <h3>
                <div className="icon-wrapper icon-leaf"><FaLeaf /></div>
                Mon impact carbone
              </h3>
            </div>
            <div className="widget-body impact-stats">
              <div className="stat-box">
                <span className="stat-number">0</span>
                <span className="stat-label">kg de CO₂ évités</span>
              </div>
              <div className="stat-box">
                <span className="stat-number">{myTrips.length}</span>
                <span className="stat-label">Trajets publiés</span>
              </div>
              <div className="stat-box">
                <span className="stat-number">{myBookings.length}</span>
                <span className="stat-label">Réservations</span>
              </div>
            </div>
          </div>

        </div>
        )}
      </div>

      {/* ── MODALE MODIFICATION DU PROFIL ── */}
      {isEditModalOpen && (
        <div
          className="modal-overlay"
          onClick={() => !isSaving && setIsEditModalOpen(false)}
        >
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Modifier mon profil</h2>
              <button
                className="modal-close-btn"
                onClick={() => setIsEditModalOpen(false)}
                disabled={isSaving}
              >
                <FiX size={18} />
              </button>
            </div>

            {modalError && <div className="modal-alert">⚠ {modalError}</div>}

            <form onSubmit={handleSaveProfile} className="modal-form">

              {/* Photo */}
              <div className="modal-photo-section">
                <div className="modal-photo-container">
                  {modalAvatar}
                  <label htmlFor="photo-upload" className="modal-photo-upload-btn">
                    <FiCamera size={13} />
                  </label>
                  <input
                    id="photo-upload"
                    type="file"
                    accept="image/png, image/jpeg"
                    style={{ display: "none" }}
                    onChange={handlePhotoUpload}
                    disabled={isSaving || isUploadingPhoto}
                  />
                </div>
                <div>
                  <p className="photo-title">Photo de profil</p>
                  <p className="photo-hint">JPG ou PNG · max 2 Mo</p>
                  {isUploadingPhoto && <p className="photo-uploading">Upload en cours...</p>}
                </div>
              </div>

              {/* Prénom / Nom */}
              <div className="form-row">
                <div className="input-group">
                  <label className="input-label">Prénom</label>
                  <input
                    type="text"
                    className="modal-input"
                    value={editFirstname}
                    onChange={(e) => setEditFirstname(e.target.value)}
                    disabled={isSaving}
                    required
                  />
                </div>
                <div className="input-group">
                  <label className="input-label">Nom</label>
                  <input
                    type="text"
                    className="modal-input"
                    value={editLastname}
                    onChange={(e) => setEditLastname(e.target.value)}
                    disabled={isSaving}
                    required
                  />
                </div>
              </div>

              {/* Email */}
              <div className="input-group">
                <label className="input-label">Adresse e-mail</label>
                <input
                  type="email"
                  className="modal-input"
                  value={editEmail}
                  onChange={(e) => setEditEmail(e.target.value)}
                  disabled={isSaving}
                  required
                />
                <p className="input-help-text">
                  Un e-mail de confirmation sera envoyé à votre nouvelle adresse.
                </p>
              </div>

              {/* Téléphone */}
              <div className="input-group">
                <label className="input-label">Téléphone (optionnel)</label>
                <input
                  type="tel"
                  className="modal-input"
                  placeholder="+32 470 00 00 00"
                  value={editPhone}
                  onChange={(e) => setEditPhone(e.target.value)}
                  disabled={isSaving}
                />
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  className="btn-cancel"
                  onClick={() => setIsEditModalOpen(false)}
                  disabled={isSaving}
                >
                  Annuler
                </button>
                <button type="submit" className="btn-save" disabled={isSaving}>
                  {isSaving ? "Enregistrement..." : "Enregistrer"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default DashboardPage;
