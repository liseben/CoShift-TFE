import React, { useState } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { FaCar, FaTicketAlt, FaLeaf } from "react-icons/fa";
import { FiEdit3, FiX, FiCamera } from "react-icons/fi";
import axios from "axios";
import "./DashboardPage.css";

// 1. CORRECTION : On déclare API_BASE ici pour pouvoir joindre Spring Boot
const API_BASE = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

const DashboardPage: React.FC = () => {
  // 2. CORRECTION : On ajoute 'login' ici pour pouvoir l'utiliser plus bas
  const { user, isLoading, login } = useAuth();

  const [isEditModalOpen, setIsEditModalOpen] = useState(false);

  // Les champs sont pré-remplis. S'ils ne sont pas modifiés, ils gardent leur valeur d'origine.
  const [editFirstname, setEditFirstname] = useState(user?.firstname || "");
  const [editLastname, setEditLastname] = useState(user?.lastname || "");
  const [editEmail, setEditEmail] = useState(user?.email || "");
  const [isSaving, setIsSaving] = useState(false);
  const [modalError, setModalError] = useState<string | null>(null);

  if (isLoading) {
    return (
      <div className="dashboard-loading">
        <div className="spinner"></div>
        <p>Chargement de votre espace...</p>
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  const handleSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);
    setModalError(null);

    try {
      const response = await axios.put(
        `${API_BASE}/api/users/profile`,
        {
          firstname: editFirstname,
          lastname: editLastname,
          email: editEmail,
        },
        {
          headers: {
            Authorization: `Bearer ${localStorage.getItem("coshift_token")}`,
          },
        },
      );

      if (response.data && response.data.token) {
        login(response.data.token);
      } else {
        console.log(
          "Profil mis à jour ! Rechargez la page ou mettez à jour le contexte.",
        );
      }

      setIsEditModalOpen(false);
    } catch (err: any) {
      console.error("Erreur lors de la mise à jour :", err);
      if (err.response?.data?.message) {
        setModalError(err.response.data.message);
      } else {
        setModalError("Une erreur est survenue lors de la sauvegarde.");
      }
    } finally {
      setIsSaving(false);
    }
  };

  const handlePhotoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const selectedFile = e.target.files[0];

      const formData = new FormData();
      formData.append("file", selectedFile);

      try {
        alert(
          `Vous avez sélectionné ${selectedFile.name}. \nL'upload automatique via FormData vers /api/users/photo est à configurer ici.`,
        );
      } catch (err) {
        console.error("Erreur upload photo", err);
        setModalError("Impossible d'uploader l'image.");
      }
    }
  };

  return (
    <div className="dashboard-container">
      <div className="dashboard-content">
        {/* EN-TÊTE DU PROFIL */}
        <div className="dashboard-header-card">
          <div className="profile-section">
            {user.pictureUrl ? (
              <img
                src={user.pictureUrl}
                alt="Profil"
                className="profile-picture-large"
              />
            ) : (
              <div className="profile-initial-large">
                {user.firstname ? user.firstname.charAt(0).toUpperCase() : "U"}
              </div>
            )}

            <div className="profile-info">
              <h1>
                Bonjour, {user.firstname} {user.lastname} 👋
              </h1>
              <p className="profile-email">{user.email}</p>
              <span className="role-badge">
                {user.role === "USER" ? "Membre CoShift" : "Administrateur"}
              </span>
            </div>
          </div>

          <div className="profile-actions">
            <button
              className="btn-outline-light"
              onClick={() => setIsEditModalOpen(true)}
            >
              <FiEdit3 size={18} />
              Modifier le profil
            </button>
          </div>
        </div>

        {/* GRILLE DES WIDGETS */}
        <div className="dashboard-grid">
          <div className="dashboard-widget">
            <div className="widget-header">
              <h3>
                <div className="icon-wrapper icon-car">
                  <FaCar />
                </div>
                Mes Trajets Proposés
              </h3>
            </div>
            <div className="widget-body empty-state">
              <p>
                Vous n'avez pas encore proposé de trajet pour vos collègues.
              </p>
              <button className="btn-primary-small">Proposer un trajet</button>
            </div>
          </div>

          <div className="dashboard-widget">
            <div className="widget-header">
              <h3>
                <div className="icon-wrapper icon-ticket">
                  <FaTicketAlt />
                </div>
                Mes Réservations
              </h3>
            </div>
            <div className="widget-body empty-state">
              <p>Vous n'avez aucune réservation de covoiturage en cours.</p>
              <button className="btn-outline-small">Trouver un trajet</button>
            </div>
          </div>

          <div className="dashboard-widget full-width">
            <div className="widget-header">
              <h3>
                <div className="icon-wrapper icon-leaf">
                  <FaLeaf />
                </div>
                Mon Impact Carbone
              </h3>
            </div>
            <div className="widget-body impact-stats">
              <div className="stat-box">
                <span className="stat-number">0</span>
                <span className="stat-label">kg de CO2 évités</span>
              </div>
              <div className="stat-box">
                <span className="stat-number">0</span>
                <span className="stat-label">Trajets partagés</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* MODALE DE MODIFICATION DU PROFIL */}
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
                <FiX size={24} />
              </button>
            </div>

            {modalError && (
              <div
                style={{
                  color: "#ef4444",
                  background: "#fef2f2",
                  padding: "10px",
                  borderRadius: "8px",
                  marginBottom: "15px",
                  fontSize: "0.9rem",
                }}
              >
                ⚠️ {modalError}
              </div>
            )}

            <form onSubmit={handleSaveProfile} className="modal-form">
              {/* SECTION PHOTO DE PROFIL */}
              <div className="modal-photo-section">
                <div className="modal-photo-container">
                  {user?.pictureUrl ? (
                    <img
                      src={user.pictureUrl}
                      alt="Profil"
                      className="modal-photo-preview"
                    />
                  ) : (
                    <div className="modal-photo-preview placeholder">
                      {user?.firstname
                        ? user.firstname.charAt(0).toUpperCase()
                        : "U"}
                    </div>
                  )}
                  <label
                    htmlFor="photo-upload"
                    className="modal-photo-upload-btn"
                  >
                    <FiCamera size={16} />
                  </label>
                  <input
                    id="photo-upload"
                    type="file"
                    accept="image/png, image/jpeg"
                    style={{ display: "none" }}
                    onChange={handlePhotoUpload}
                    disabled={isSaving}
                  />
                </div>
                <div className="modal-photo-text">
                  <p className="photo-title">Photo de profil</p>
                  <p className="photo-hint">
                    Format JPG ou PNG. Poids maximum 2Mo.
                  </p>
                </div>
              </div>

              <div className="form-row">
                <div className="input-group">
                  <label className="input-label">Prénom</label>
                  {/* 3. CORRECTION : setEditFirstname au lieu de setFirstname */}
                  <input
                    type="text"
                    className="modal-input"
                    value={editFirstname}
                    onChange={(e) => setEditFirstname(e.target.value)}
                    disabled={isSaving}
                  />
                </div>

                <div className="input-group">
                  <label className="input-label">Nom</label>
                  {/* 3. CORRECTION : setEditLastname au lieu de setLastname */}
                  <input
                    type="text"
                    className="modal-input"
                    value={editLastname}
                    onChange={(e) => setEditLastname(e.target.value)}
                    disabled={isSaving}
                  />
                </div>
              </div>

              <div className="input-group">
                <label className="input-label">
                  Adresse e-mail professionnelle
                </label>
                <input
                  type="email"
                  className="modal-input"
                  value={editEmail}
                  onChange={(e) => setEditEmail(e.target.value)}
                  disabled={isSaving}
                />
                <p className="input-help-text">
                  Un e-mail de confirmation sera envoyé à votre nouvelle
                  adresse.
                </p>
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
                  {isSaving
                    ? "Enregistrement..."
                    : "Enregistrer les modifications"}
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
