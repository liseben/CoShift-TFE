import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { FaCar, FaMapMarkerAlt, FaCalendarAlt, FaUsers, FaEuroSign } from "react-icons/fa";
import { FiArrowLeft, FiCheck } from "react-icons/fi";
import axios from "axios";
import "./TripsPage.css";

import { API_BASE } from "../../config/api";

interface Vehicule {
  uuid: string;
  brand: string;
  model: string;
  seats: number;
  energy: string;
}

const CreateTripPage: React.FC = () => {
  const navigate = useNavigate();
  const [vehicules, setVehicules] = useState<Vehicule[]>([]);
  const [loading, setLoading]     = useState(true);
  const [saving, setSaving]       = useState(false);
  const [error, setError]         = useState<string | null>(null);
  const [success, setSuccess]     = useState(false);

  const token = () => localStorage.getItem("coshift_token") ?? "";
  const headers = () => ({ Authorization: `Bearer ${token()}` });

  const [form, setForm] = useState({
    departureCity:    "",
    departureAddress: "",
    arrivalCity:      "",
    arrivalAddress:   "",
    departureTime:    "",
    availableSeats:   1,
    pricePerSeat:     "",
    vehiculeUuid:     "",
    description:      "",
    acceptsLuggage:   true,
    acceptsPets:      false,
    musicAllowed:     true,
    talkingAllowed:   true,
  });

  useEffect(() => {
    axios.get(`${API_BASE}/api/vehicules/mine`, { headers: headers() })
      .then((r) => { setVehicules(r.data); if (r.data.length > 0) setForm((f) => ({ ...f, vehiculeUuid: r.data[0].uuid })); })
      .catch(() => setError("Impossible de charger vos véhicules. Enregistrez d'abord un véhicule."))
      .finally(() => setLoading(false));
  }, []);

  const set = (key: string, value: unknown) => setForm((f) => ({ ...f, [key]: value }));

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!form.vehiculeUuid) { setError("Sélectionnez un véhicule."); return; }
    setSaving(true); setError(null);
    try {
      await axios.post(`${API_BASE}/api/trips`, {
        ...form,
        pricePerSeat: parseFloat(form.pricePerSeat as string),
      }, { headers: headers() });
      setSuccess(true);
      setTimeout(() => navigate("/dashboard"), 2000);
    } catch (err: any) {
      setError(err.response?.data?.message ?? "Une erreur est survenue.");
    } finally {
      setSaving(false);
    }
  };

  // Minimum : maintenant + 2h
  const minDateTime = new Date(Date.now() + 2 * 3600 * 1000)
    .toISOString().slice(0, 16);

  return (
    <div className="trips-container">
      <div className="trips-inner">

        {/* ── En-tête ── */}
        <div className="trips-page-header">
          <button className="btn-back" onClick={() => navigate(-1)}>
            <FiArrowLeft size={16} /> Retour
          </button>
          <div>
            <h1 className="trips-page-title">Proposer un trajet</h1>
            <p className="trips-page-subtitle">
              Partagez votre trajet et réduisez votre empreinte carbone.
            </p>
          </div>
        </div>

        {success && (
          <div className="trips-success">
            <FiCheck size={20} /> Trajet publié avec succès ! Redirection vers votre dashboard...
          </div>
        )}
        {error && <div className="trips-alert">{error}</div>}

        {loading ? (
          <div className="trips-loading"><div className="spinner" /></div>
        ) : vehicules.length === 0 ? (
          <div className="no-vehicle-notice">
            <FaCar size={40} />
            <h3>Aucun véhicule enregistré</h3>
            <p>Vous devez enregistrer un véhicule avant de pouvoir proposer un trajet.</p>
            <button className="btn-trip-primary" onClick={() => navigate("/dashboard?tab=vehicles")}>
              Ajouter un véhicule
            </button>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="create-trip-form">

            {/* ── Itinéraire ── */}
            <div className="form-section">
              <h3 className="form-section-title">
                <FaMapMarkerAlt /> Itinéraire
              </h3>
              <div className="form-grid-2">
                <div className="input-group">
                  <label className="input-label">Ville de départ *</label>
                  <input className="trip-input" placeholder="Ex: Liège" required
                    value={form.departureCity} onChange={(e) => set("departureCity", e.target.value)} />
                </div>
                <div className="input-group">
                  <label className="input-label">Ville d'arrivée *</label>
                  <input className="trip-input" placeholder="Ex: Bruxelles" required
                    value={form.arrivalCity} onChange={(e) => set("arrivalCity", e.target.value)} />
                </div>
                <div className="input-group">
                  <label className="input-label">Point de départ précis</label>
                  <input className="trip-input" placeholder="Ex: Gare de Liège-Guillemins"
                    value={form.departureAddress} onChange={(e) => set("departureAddress", e.target.value)} />
                </div>
                <div className="input-group">
                  <label className="input-label">Point d'arrivée précis</label>
                  <input className="trip-input" placeholder="Ex: Gare du Midi, Bruxelles"
                    value={form.arrivalAddress} onChange={(e) => set("arrivalAddress", e.target.value)} />
                </div>
              </div>
            </div>

            {/* ── Date & places ── */}
            <div className="form-section">
              <h3 className="form-section-title">
                <FaCalendarAlt /> Date & places
              </h3>
              <div className="form-grid-3">
                <div className="input-group">
                  <label className="input-label">Date et heure de départ *</label>
                  <input className="trip-input" type="datetime-local" min={minDateTime} required
                    value={form.departureTime} onChange={(e) => set("departureTime", e.target.value)} />
                </div>
                <div className="input-group">
                  <label className="input-label">Places disponibles *</label>
                  <input className="trip-input" type="number" min={1} max={8}
                    value={form.availableSeats} onChange={(e) => set("availableSeats", parseInt(e.target.value))} />
                </div>
                <div className="input-group">
                  <label className="input-label">Prix par place (€) *</label>
                  <div className="input-icon-wrapper">
                    <FaEuroSign className="input-icon" />
                    <input className="trip-input with-icon" type="number" min={0} step={0.5} placeholder="0.00"
                      value={form.pricePerSeat} onChange={(e) => set("pricePerSeat", e.target.value)} required />
                  </div>
                </div>
              </div>
            </div>

            {/* ── Véhicule ── */}
            <div className="form-section">
              <h3 className="form-section-title">
                <FaCar /> Véhicule
              </h3>
              <div className="vehicle-selector">
                {vehicules.map((v) => (
                  <label key={v.uuid} className={`vehicle-option ${form.vehiculeUuid === v.uuid ? "selected" : ""}`}>
                    <input type="radio" name="vehicule" value={v.uuid}
                      checked={form.vehiculeUuid === v.uuid}
                      onChange={() => set("vehiculeUuid", v.uuid)} />
                    <div className="vehicle-option-content">
                      <span className="vo-name">{v.brand} {v.model}</span>
                      <span className="vo-seats"><FaUsers size={11} /> {v.seats} places · {v.energy}</span>
                    </div>
                    {form.vehiculeUuid === v.uuid && <FiCheck className="vo-check" />}
                  </label>
                ))}
              </div>
            </div>

            {/* ── Description & préférences ── */}
            <div className="form-section">
              <h3 className="form-section-title">Détails & préférences</h3>
              <div className="input-group" style={{ marginBottom: 20 }}>
                <label className="input-label">Description (optionnel)</label>
                <textarea className="trip-input trip-textarea" rows={3}
                  placeholder="Précisez le point de ramassage, les étapes, etc."
                  value={form.description} onChange={(e) => set("description", e.target.value)} />
              </div>
              <div className="preferences-grid">
                {[
                  { key: "acceptsLuggage", label: "🧳 Bagages acceptés" },
                  { key: "acceptsPets",    label: "🐾 Animaux acceptés" },
                  { key: "musicAllowed",   label: "🎵 Musique autorisée" },
                  { key: "talkingAllowed", label: "💬 Discussion bienvenue" },
                ].map(({ key, label }) => (
                  <label key={key} className={`pref-toggle ${(form as Record<string, unknown>)[key] ? "on" : "off"}`}>
                    <input type="checkbox"
                      checked={!!(form as Record<string, unknown>)[key]}
                      onChange={(e) => set(key, e.target.checked)} />
                    <span>{label}</span>
                  </label>
                ))}
              </div>
            </div>

            <div className="create-trip-footer">
              <button type="button" className="btn-trip-secondary" onClick={() => navigate(-1)} disabled={saving}>
                Annuler
              </button>
              <button type="submit" className="btn-trip-primary" disabled={saving}>
                {saving ? "Publication en cours..." : "Publier le trajet"}
              </button>
            </div>

          </form>
        )}
      </div>
    </div>
  );
};

export default CreateTripPage;
