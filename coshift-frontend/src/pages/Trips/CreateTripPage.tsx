import { useState, useEffect, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { FaCar, FaMapMarkerAlt, FaCalendarAlt, FaUsers, FaSuitcase, FaDog, FaMusic, FaComments } from "react-icons/fa";
import { FiArrowLeft, FiCheck } from "react-icons/fi";
import axios from "axios";
import { API_BASE } from "../../config/api";
import { Alert, Button, Card, EmptyState, Input, Spinner, Textarea } from "../../components/ui";
import "./CreateTripPage.css";

interface Vehicule {
  uuid: string;
  brand: string;
  model: string;
  seats: number;
  energy: string;
}

const ENERGY_LABEL: Record<string, string> = {
  ELECTRIC: "Électrique", HYBRID: "Hybride", GASOLINE: "Essence",
  DIESEL: "Diesel", LPG: "GPL",
};

/* Icônes SVG plutôt qu'emojis : le rendu d'un emoji change d'un système à
   l'autre, et les lecteurs d'écran en énoncent le nom au milieu du libellé. */
const PREFERENCES = [
  { key: "acceptsLuggage", label: "Bagages acceptés", icon: <FaSuitcase /> },
  { key: "acceptsPets", label: "Animaux acceptés", icon: <FaDog /> },
  { key: "musicAllowed", label: "Musique autorisée", icon: <FaMusic /> },
  { key: "talkingAllowed", label: "Discussion bienvenue", icon: <FaComments /> },
] as const;

export default function CreateTripPage() {
  const navigate = useNavigate();
  const [vehicules, setVehicules] = useState<Vehicule[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const headers = () => ({
    Authorization: `Bearer ${localStorage.getItem("coshift_token") ?? ""}`,
  });

  const [form, setForm] = useState({
    departureCity: "", departureAddress: "",
    arrivalCity: "", arrivalAddress: "",
    departureTime: "", availableSeats: 1, pricePerSeat: "",
    vehiculeUuid: "", description: "",
    acceptsLuggage: true, acceptsPets: false,
    musicAllowed: true, talkingAllowed: true,
  });

  const set = (key: string, value: unknown) => setForm((f) => ({ ...f, [key]: value }));

  useEffect(() => {
    axios
      .get<Vehicule[]>(`${API_BASE}/api/vehicules/mine`, { headers: headers() })
      .then((r) => {
        setVehicules(r.data);
        if (r.data.length > 0) set("vehiculeUuid", r.data[0].uuid);
      })
      .catch(() => setError("Impossible de charger vos véhicules."))
      .finally(() => setLoading(false));
  }, []);

  const selected = vehicules.find((v) => v.uuid === form.vehiculeUuid);
  // Le conducteur occupe une place : on ne peut en proposer davantage.
  const maxSeats = selected ? selected.seats - 1 : 8;

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!form.vehiculeUuid) {
      setError("Sélectionnez un véhicule.");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await axios.post(
        `${API_BASE}/api/trips`,
        { ...form, pricePerSeat: parseFloat(form.pricePerSeat) },
        { headers: headers() },
      );
      setSuccess(true);
      setTimeout(() => navigate("/dashboard"), 1800);
    } catch (err) {
      setError(
        (axios.isAxiosError(err) && err.response?.data?.message) ||
          "Une erreur est survenue.",
      );
    } finally {
      setSaving(false);
    }
  };

  // Un trajet ne se publie pas pour dans dix minutes : minimum deux heures.
  const minDateTime = new Date(Date.now() + 2 * 3600 * 1000).toISOString().slice(0, 16);

  return (
    <div className="container page stack-8">
      <header className="ct__header">
        <Button variant="ghost" size="sm" icon={<FiArrowLeft />} onClick={() => navigate(-1)}>
          Retour
        </Button>
        <div>
          <h1>Proposer un trajet</h1>
          <p className="ct__lead">
            Partagez votre trajet et réduisez votre empreinte carbone.
          </p>
        </div>
      </header>

      {success && (
        <Alert tone="success" title="Trajet publié">
          Redirection vers votre tableau de bord…
        </Alert>
      )}
      {error && <Alert tone="danger" onDismiss={() => setError(null)}>{error}</Alert>}

      {loading ? (
        <Spinner size="lg" center showLabel label="Chargement de vos véhicules" />
      ) : vehicules.length === 0 ? (
        <EmptyState
          icon={<FaCar />}
          title="Aucun véhicule enregistré"
          description="Un trajet se publie avec un véhicule. Enregistrez-en un d'abord."
          action={<Button to="/dashboard?tab=vehicles">Ajouter un véhicule</Button>}
        />
      ) : (
        <form onSubmit={submit} className="stack-6">
          <Card title={<><FaMapMarkerAlt aria-hidden="true" /> Itinéraire</>}>
            <div className="ct__grid">
              <Input label="Ville de départ" placeholder="Liège" required
                value={form.departureCity} onChange={(e) => set("departureCity", e.target.value)} />
              <Input label="Ville d'arrivée" placeholder="Bruxelles" required
                value={form.arrivalCity} onChange={(e) => set("arrivalCity", e.target.value)} />
              <Input label="Point de départ précis" placeholder="Gare de Liège-Guillemins"
                hint="Aide vos passagers à vous retrouver."
                value={form.departureAddress} onChange={(e) => set("departureAddress", e.target.value)} />
              <Input label="Point d'arrivée précis" placeholder="Gare du Midi, Bruxelles"
                value={form.arrivalAddress} onChange={(e) => set("arrivalAddress", e.target.value)} />
            </div>
          </Card>

          <Card title={<><FaCalendarAlt aria-hidden="true" /> Date et places</>}>
            <div className="ct__grid">
              <Input label="Date et heure de départ" type="datetime-local" min={minDateTime} required
                hint="Au plus tôt dans deux heures."
                value={form.departureTime} onChange={(e) => set("departureTime", e.target.value)} />
              <Input label="Places proposées" type="number" min={1} max={maxSeats} required
                hint={selected ? `Jusqu'à ${maxSeats}, votre siège déduit.` : undefined}
                value={form.availableSeats}
                onChange={(e) => set("availableSeats", parseInt(e.target.value) || 1)} />
              <Input label="Prix par place (€)" type="number" min={0} step={0.5} placeholder="4.50" required
                hint="Partage de frais, pas un bénéfice."
                value={form.pricePerSeat} onChange={(e) => set("pricePerSeat", e.target.value)} />
            </div>
          </Card>

          <Card title={<><FaCar aria-hidden="true" /> Véhicule</>}>
            <fieldset className="ct__vehicles">
              <legend className="sr-only">Choisissez le véhicule du trajet</legend>
              {vehicules.map((v) => (
                <label
                  key={v.uuid}
                  className={`ct__vehicle ${form.vehiculeUuid === v.uuid ? "is-selected" : ""}`}
                >
                  <input type="radio" name="vehicule" value={v.uuid}
                    checked={form.vehiculeUuid === v.uuid}
                    onChange={() => set("vehiculeUuid", v.uuid)} />
                  <span className="ct__vehicle-body">
                    <span className="ct__vehicle-name">{v.brand} {v.model}</span>
                    <span className="ct__vehicle-meta">
                      <FaUsers aria-hidden="true" /> {v.seats} places ·{" "}
                      {ENERGY_LABEL[v.energy] ?? v.energy}
                    </span>
                  </span>
                  {form.vehiculeUuid === v.uuid && <FiCheck aria-hidden="true" className="ct__check" />}
                </label>
              ))}
            </fieldset>
          </Card>

          <Card title="Détails et préférences">
            <Textarea
              label="Description"
              hint="Point de ramassage, étapes, contraintes particulières."
              maxLength={500}
              showCount
              value={form.description}
              onChange={(e) => set("description", e.target.value)}
            />

            <fieldset className="ct__prefs">
              <legend className="ct__prefs-legend">Préférences du trajet</legend>
              {PREFERENCES.map(({ key, label, icon }) => (
                <label
                  key={key}
                  className={`ct__pref ${(form as Record<string, unknown>)[key] ? "is-on" : ""}`}
                >
                  <input type="checkbox"
                    checked={!!(form as Record<string, unknown>)[key]}
                    onChange={(e) => set(key, e.target.checked)} />
                  <span aria-hidden="true">{icon}</span>
                  {label}
                </label>
              ))}
            </fieldset>
          </Card>

          <div className="ct__footer">
            <Button type="button" variant="ghost" onClick={() => navigate(-1)} disabled={saving}>
              Annuler
            </Button>
            <Button type="submit" size="lg" loading={saving}>
              Publier le trajet
            </Button>
          </div>
        </form>
      )}
    </div>
  );
}
