import { useState, useEffect, type FormEvent, type ReactElement } from "react";
import { useAuth } from "../../context/AuthContext";
import { FaCar, FaPlus, FaTrash, FaEdit, FaGasPump, FaBolt, FaLeaf, FaUsers } from "react-icons/fa";
import axios from "axios";
import { API_BASE } from "../../config/api";
import {
  Alert, Button, Card, EmptyState, Input, Modal, Select, Spinner,
} from "../../components/ui";
import { useT } from "../../context/LangContext";
import "./VehiclePage.css";

const ENERGY: Record<string, { label: string; icon: ReactElement; tone: string }> = {
  ELECTRIC: { label: "ELECTRIC", icon: <FaBolt />, tone: "eco" },
  HYBRID:   { label: "HYBRID",   icon: <FaLeaf />, tone: "eco" },
  GASOLINE: { label: "GASOLINE", icon: <FaGasPump />, tone: "pending" },
  DIESEL:   { label: "DIESEL",   icon: <FaGasPump />, tone: "danger" },
  LPG:      { label: "LPG",      icon: <FaGasPump />, tone: "brand" },
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

const EMPTY = { brand: "", model: "", licensePlate: "", seats: 5, energy: "GASOLINE", photoUrl: "" };

export default function VehiclePage() {
  const t = useT();
  const { user } = useAuth();
  const [vehicules, setVehicules] = useState<Vehicule[]>([]);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Vehicule | null>(null);
  const [toDelete, setToDelete] = useState<Vehicule | null>(null);
  const [form, setForm] = useState({ ...EMPTY });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const headers = () => ({
    Authorization: `Bearer ${localStorage.getItem("coshift_token") ?? ""}`,
  });

  useEffect(() => {
    axios
      .get<Vehicule[]>(`${API_BASE}/api/vehicules/mine`, { headers: headers() })
      .then((r) => setVehicules(r.data))
      .catch(() => setError(t("vehicules.indisponibles")))
      .finally(() => setLoading(false));
  }, []);

  const set = (k: string, v: unknown) => setForm((f) => ({ ...f, [k]: v }));

  const openAdd = () => {
    setEditing(null);
    setForm({ ...EMPTY });
    setError(null);
    setOpen(true);
  };

  const openEdit = (v: Vehicule) => {
    setEditing(v);
    setForm({
      brand: v.brand, model: v.model, licensePlate: v.licensePlate,
      seats: v.seats, energy: v.energy, photoUrl: v.photoUrl ?? "",
    });
    setError(null);
    setOpen(true);
  };

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      if (editing) {
        const res = await axios.put<Vehicule>(
          `${API_BASE}/api/vehicules/${editing.uuid}`, form, { headers: headers() },
        );
        setVehicules((prev) => prev.map((v) => (v.uuid === editing.uuid ? res.data : v)));
      } else {
        const res = await axios.post<Vehicule>(
          `${API_BASE}/api/vehicules`, form, { headers: headers() },
        );
        setVehicules((prev) => [...prev, res.data]);
      }
      setOpen(false);
    } catch (err) {
      setError(
        (axios.isAxiosError(err) && err.response?.data?.message) || "Une erreur est survenue.",
      );
    } finally {
      setSaving(false);
    }
  };

  const remove = async () => {
    if (!toDelete) return;
    setSaving(true);
    try {
      await axios.delete(`${API_BASE}/api/vehicules/${toDelete.uuid}`, { headers: headers() });
      setVehicules((prev) => prev.filter((v) => v.uuid !== toDelete.uuid));
      setToDelete(null);
    } catch (err) {
      setError(
        (axios.isAxiosError(err) && err.response?.data?.message) ||
          t("vehicules.suppressionImpossible"),
      );
      setToDelete(null);
    } finally {
      setSaving(false);
    }
  };

  if (!user) return null;

  return (
    <div className="stack-6">
      <header className="vc__header">
        <div>
          <h2>{t("vehicules.titre")}</h2>
          <p className="vc__lead">
            {t("vehicules.accroche")}
          </p>
        </div>
        <Button icon={<FaPlus />} onClick={openAdd}>{t("vehicules.ajouter")}</Button>
      </header>

      {error && !open && <Alert tone="danger" onDismiss={() => setError(null)}>{error}</Alert>}

      {loading ? (
        <Spinner size="lg" center showLabel label={t("vehicules.chargement")} />
      ) : vehicules.length === 0 ? (
        <EmptyState
          icon={<FaCar />}
          title={t("vehicules.aucun")}
          description={t("vehicules.aucunTexte")}
          action={<Button icon={<FaPlus />} onClick={openAdd}>{t("vehicules.ajouter")}</Button>}
        />
      ) : (
        <div className="grid-auto">
          {vehicules.map((v) => {
            const e = ENERGY[v.energy] ?? { label: v.energy, icon: <FaCar />, tone: "brand" };
            return (
              <Card key={v.uuid} title={`${v.brand} ${v.model}`}>
                <p className="vc__plate">{v.licensePlate}</p>

                <ul className="vc__tags">
                  <li className={`vc__tag vc__tag--${e.tone}`}>
                    <span aria-hidden="true">{e.icon}</span> {t(`energie.${e.label}`) || e.label}
                  </li>
                  <li className="vc__tag">
                    <FaUsers aria-hidden="true" /> {t("vehicules.places", { n: v.seats })}
                  </li>
                </ul>

                <div className="vc__actions">
                  <Button variant="secondary" size="sm" icon={<FaEdit />} onClick={() => openEdit(v)}>
                    {t("vehicules.modifier")}
                  </Button>
                  <Button variant="ghost" size="sm" icon={<FaTrash />} onClick={() => setToDelete(v)}>
                    {t("vehicules.supprimer")}
                  </Button>
                </div>
              </Card>
            );
          })}
        </div>
      )}

      {/* ── Ajout et modification ── */}
      <Modal
        open={open}
        onClose={() => setOpen(false)}
        title={editing ? t("vehicules.modifierTitre") : t("vehicules.ajouter")}
        footer={
          <>
            <Button variant="ghost" onClick={() => setOpen(false)}>{t("commun.annuler")}</Button>
            <Button type="submit" form="vc-form" loading={saving}>
              {editing ? t("commun.enregistrer") : t("vehicules.ajouterAction")}
            </Button>
          </>
        }
      >
        {error && <Alert tone="danger">{error}</Alert>}

        <form id="vc-form" onSubmit={submit} className="stack">
          <div className="vc__grid">
            <Input label={t("vehicules.marque")} placeholder={t("vehicules.marqueExemple")} required
              value={form.brand} onChange={(e) => set("brand", e.target.value)} />
            <Input label={t("vehicules.modele")} placeholder={t("vehicules.modeleExemple")} required
              value={form.model} onChange={(e) => set("model", e.target.value)} />
          </div>

          <Input label={t("vehicules.plaque")} placeholder={t("vehicules.plaqueExemple")} required
            hint={t("vehicules.plaqueAide")}
            value={form.licensePlate}
            onChange={(e) => set("licensePlate", e.target.value.toUpperCase())} />

          <div className="vc__grid">
            <Input label={t("vehicules.nombreDePlaces")} type="number" min={2} max={9} required
              hint={t("vehicules.nombreDePlacesAide")}
              value={form.seats}
              onChange={(e) => set("seats", parseInt(e.target.value) || 2)} />
            <Select label={t("vehicules.motorisation")} value={form.energy}
              onChange={(e) => set("energy", e.target.value)}
              options={Object.keys(ENERGY).map((value) => ({
                value,
                label: t(`energie.${value}`) || value,
              }))} />
          </div>
        </form>
      </Modal>

      {/* ── Suppression ── */}
      <Modal
        open={toDelete !== null}
        onClose={() => setToDelete(null)}
        title={t("vehicules.supprimerTitre")}
        size="sm"
        footer={
          <>
            <Button variant="ghost" onClick={() => setToDelete(null)}>{t("commun.retour")}</Button>
            <Button variant="danger" loading={saving} onClick={remove}>{t("vehicules.supprimer")}</Button>
          </>
        }
      >
        {toDelete && (
          <p>
            {t("vehicules.supprimerTexte", {
              vehicule: `${toDelete.brand} ${toDelete.model} (${toDelete.licensePlate})`,
            })}
          </p>
        )}
      </Modal>
    </div>
  );
}
