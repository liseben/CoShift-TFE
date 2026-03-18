import React from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import "./DashboardPage.css";

const DashboardPage: React.FC = () => {
  const { user, isLoading } = useAuth();

  // 1. Pendant que React vérifie le token au chargement
  if (isLoading) {
    return (
      <div className="dashboard-loading">
        <div className="spinner"></div>
        <p>Chargement de votre espace...</p>
      </div>
    );
  }

  // 2. Protection de la route : Si pas d'utilisateur, on le jette dehors !
  if (!user) {
    return <Navigate to="/login" replace />;
  }

  // 3. L'utilisateur est connecté, on affiche le Dashboard
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
            <button className="btn-outline-light">Modifier le profil</button>
          </div>
        </div>

        {/* GRILLE DES WIDGETS (Pour plus tard) */}
        <div className="dashboard-grid">
          <div className="dashboard-widget">
            <div className="widget-header">
              <h3>🚗 Mes Trajets Proposés</h3>
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
              <h3>🎟️ Mes Réservations</h3>
            </div>
            <div className="widget-body empty-state">
              <p>Vous n'avez aucune réservation de covoiturage en cours.</p>
              <button className="btn-outline-small">Trouver un trajet</button>
            </div>
          </div>

          <div className="dashboard-widget full-width">
            <div className="widget-header">
              <h3>🌱 Mon Impact Carbone</h3>
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
    </div>
  );
};

export default DashboardPage;
