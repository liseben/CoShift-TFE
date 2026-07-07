import React, { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import { FaCar, FaPlus, FaTrash, FaEdit, FaGasPump, FaBolt, FaLeaf } from "react-icons/fa";
import { FiX } from "react-icons/fi";
import axios from "axios";
import "./VehiclePage.css";

const API_BASE = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

const ENERGY_LABELS: Record<string, { label: string; icon: JSX.Element; color: string }> = {
  ELECTRIC: { label: "Électrique", icon: <FaBolt />, color: "#34d399" },
  HYBRID:   { label: "Hybride",    icon: <FaLeaf />, color: "#60a5fa" },
  GASOLINE: { label: "Essence",    icon: <FaGasPump />, color: "#fbbf24" },
  DIESEL:   { label: "Diesel",     icon: <FaGasPump />, color: "#f87171" },
  LPG:      { label: "GPL",        icon: <FaGasPump />, color: "#a78bfa" },
};

interface Vehicule {
  uuid: string;
  brand: string;
  model: string;
  licensePlate: string;
  seats: number;
  energy: string;
  photoUrl?: string;
}

const emptyForm = { brand: "", model: "", licensePlate: "", seats: 2, energy: "GASOLINE", photoUrl: "" };

const VehiclePage: React.FC = () => {
  const { user } = useAuth();
  const [vehicules, setVehicules]   = useState<Vehicule[]>([]);
  const [loading, setLoading]       = useState(true);
  const [showModal, setShowModal]   = useState(false);
  const [editing, setEditing]       = useState<Vehicule | null>(null);
  const [form, setForm]             = useState({ ...emptyForm });
  const [saving, setSaving]         = useState(false);
  const [error, setError]           = useState<string | null>(null);

  const token = () => localStorage.getItem("coshift_token") ?? "";
  const headers = () => ({ Authorization: `Bearer ${token()}` });

  const fetchVehicules = async () => {
    try {
      const res = await axios.get(`${API_BASE}/api/vehicules/mine`, { headers: headers() });
      setVehicules(res.data);
    } catch {
      setError("Impossible de charger vos véhicules.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchVehicules(); }, []);

  const openAdd = () => {
    setEditing(null);
    setForm({ ...emptyForm });
    setError(null);
    setShowModal(true);
  };

  const openEdit = (v: Vehicule) => {
    setEditing(v);
    setForm({ brand: v.brand, model: v.model, licensePlate: v.licensePlate,
              seats: v.seats, energy: v.energy, photoUrl: v.photoUrl ?? "" });
    setError(null);
    setShowModal(true);
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      if (editing) {
        const res = await axios.put(
          `${API_BASE}/api/vehicules/${editing.uuid}`, form, { headers: headers() });
        setVehicules((prev) => prev.map((v) => v.uuid === editing.uuid ? res.data : v));
      } else {
        const res = await axios.post(`${API_BASE}/api/vehicules`, form, { headers: headers() });
        setVehicules((prev) => [...prev, res.data]);
      }
      setShowModal(false);
    } catch (err: any) {
      setError(err.response?.data?.message ?? "Une erreur est survenue.");
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (uuid: string) => {
    if (!window.confirm("Supprimer ce véhicule ?")) return;
    try {
      await axios.delete(`${API_BASE}/api/vehicules/${uuid}`, { headers: headers() });
      setVehicules((prev) => prev.filter((v) => v.uuid !== uuid));
    } catch (err: any) {
      setError(err.response?.data?.message ?? "Impossible de supprimer ce véhicule.");
    }
  };

  if (!user) return null;

  return (
    <div className="vehicle-page">
      <div className="vehicle-header">
        <div>
          <h2 className="vehicle-title">Mes véhicules</h2>
          <p className="vehicle-subtitle">
            Enregistrez vos véhicules pour pouvoir proposer des trajets.
          </p>
        </div>
        <button className="btn-add-vehicle" onClick={openAdd}>
          <FaPlus size={13} /> Ajouter un véhicule
        </button>
      </div>

      {error && <div className="vehicle-alert">{error}</div>}

      {loading ? (
        <div className="vehicle-loading"><div className="spinner" /></div>
      ) : vehicules.length === 0 ? (
        <div className="vehicle-empty">
          <FaCar size={48} className="vehicle-empty-icon" />
          <h3>Aucun véhicule enregistré</h3>
          <p>Ajoutez votre premier véhicule pour commencer à proposer des trajets.</p>
          <button className="btn-add-vehicle" onClick={openAdd}>
            <FaPlus size={13} /> Ajouter mon premier véhicule
          </button>
        </div>
      ) : (
        <div className="vehicle-grid">
          {vehicules.map((v) => {
            const energyInfo = ENERGY_LABELS[v.energy] ?? { label: v.energy, icon: <FaGasPump />, color: "#94a3b8" };
            return (
              <div className="vehicle-card" key={v.uuid}>
                <div className="vehicle-card-header">
                  {v.photoUrl ? (
                    <img src={v.photoUrl} alt={`${v.brand} ${v.model}`} className="vehicle-photo" />
                  ) : (
                    <div className="vehicle-photo-placeholder">
                      <FaCar size={32} />
                    </div>
                  )}
                  <div className="vehicle-card-actions">
                    <button className="vc-btn-edit" onClick={() => openEdit(v)} title="Modifier">
                      <FaEdit size={13} />
                    </button>
                    <button className="vc-btn-delete" onClick={() => handleDelete(v.uuid)} title="Supprimer">
                      <FaTrash size={13} />
                    </button>
                  </div>
                </div>
                <div className="vehicle-card-body">
                  <h3 className="vc-name">{v.brand} {v.model}</h3>
                  <p className="vc-plate">{v.licensePlate}</p>
                  <div className="vc-tags">
                    <span className="vc-tag" style={{ color: energyInfo.color, borderColor: `${energyInfo.color}33`, background: `${energyInfo.color}11` }}>
                      {energyInfo.icon} {energyInfo.label}
                    </span>
                    <span className="vc-tag vc-tag-seats">
                      {v.seats} places
                    </span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* ── MODALE AJOUT / MODIFICATION ── */}
      {showModal && (
        <div className="modal-overlay" onClick={() => !saving && setShowModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editing ? "Modifier le véhicule" : "Ajouter un véhicule"}</h2>
              <button className="modal-close-btn" onClick={() => setShowModal(false)} disabled={saving}>
                <FiX size={18} />
              </button>
            </div>

            {error && <div className="modal-alert">⚠ {error}</div>}

            <form onSubmit={handleSubmit} className="modal-form">
              <div className="form-row">
                <div className="input-group">
                  <label className="input-label">Marque</label>
                  <input className="modal-input" placeholder="Ex: Renault" value={form.brand}
                    onChange={(e) => setForm({ ...form, brand: e.target.value })}
                    required disabled={saving} />
                </div>
                <div className="input-group">
                  <label className="input-label">Modèle</label>
                  <input className="modal-input" placeholder="Ex: Clio" value={form.model}
                    onChange={(e) => setForm({ ...form, model: e.target.value })}
                    required disabled={saving} />
                </div>
              </div>

              <div className="form-row">
                <div className="input-group">
                  <label className="input-label">Immatriculation</label>
                  <input className="modal-input" placeholder="Ex: 1-ABC-123" value={form.licensePlate}
                    onChange={(e) => setForm({ ...form, licensePlate: e.target.value })}
                    required disabled={saving} />
                </div>
                <div className="input-group">
                  <label className="input-label">Nombre de places</label>
                  <input className="modal-input" type="number" min={2} max={9} value={form.seats}
                    onChange={(e) => setForm({ ...form, seats: parseInt(e.target.value) })}
                    required disabled={saving} />
                </div>
              </div>

              <div className="input-group">
                <label className="input-label">Type de carburant</label>
                <select className="modal-input modal-select" value={form.energy}
                  onChange={(e) => setForm({ ...form, energy: e.target.value })} disabled={saving}>
                  {Object.entries(ENERGY_LABELS).map(([key, val]) => (
                    <option key={key} value={key}>{val.label}</option>
                  ))}
                </select>
              </div>

              <div className="input-group">
                <label className="input-label">URL photo du véhicule (optionnel)</label>
                <input className="modal-input" type="url" placeholder="https://..." value={form.photoUrl}
                  onChange={(e) => setForm({ ...form, photoUrl: e.target.value })} disabled={saving} />
                <p className="input-help-text">Lien vers une photo de votre véhicule.</p>
              </div>

              <div className="modal-footer">
                <button type="button" className="btn-cancel" onClick={() => setShowModal(false)} disabled={saving}>
                  Annuler
                </button>
                <button type="submit" className="btn-save" disabled={saving}>
                  {saving ? "Enregistrement..." : editing ? "Mettre à jour" : "Ajouter le véhicule"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default VehiclePage;
