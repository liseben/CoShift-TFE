import { useState, useEffect, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { FaCar, FaMapMarkerAlt, FaCalendarAlt, FaUsers, FaSuitcase, FaDog, FaMusic, FaComments } from "react-icons/fa";
import { FiArrowLeft, FiCheck } from "react-icons/fi";
import axios from "axios";
import { API_BASE } from "../../config/api";
import { Alert, Button, Card, EmptyState, Input, Spinner, Textarea } from "../../components/ui";
import { useT } from "../../context/LangContext";
import "./CreateTripPage.css";
import { useSeo } from "../../hooks/useSeo";

interface Vehicule {
  uuid: string;
  brand: string;
  model: string;
  seats: number;
  energy: string;
}

/* Icônes SVG plutôt qu'emojis : le rendu d'un emoji change d'un système à
   l'autre, et les lecteurs d'écran en énoncent le nom au milieu du libellé.
   Les libellés sont des clés : ce tableau est évalué au chargement du module,
   avant que la langue soit connue. */
const PREFERENCES = [
  { key: "acceptsLuggage", cle: "bagagesAcceptes",     icon: <FaSuitcase /> },
  { key: "acceptsPets",    cle: "animauxAcceptes",     icon: <FaDog /> },
  { key: "musicAllowed",   cle: "musiqueAutorisee",    icon: <FaMusic /> },
  { key: "talkingAllowed", cle: "discussionBienvenue", icon: <FaComments /> },
] as const;

export default function CreateTripPage() {
  const t = useT();

  /* Sans ces metadonnees, l'onglet gardait le titre francais
     d'index.html, quelle que soit la langue choisie. */
  useSeo({
    titre: t("pages.publierTitre"),
    description: t("pages.publierDescription"),
    chemin: "/trips/create",
    horsIndex: true,
  });
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
      .catch(() => setError(t("publier.vehiculesIndisponibles")))
      .finally(() => setLoading(false));
  }, []);

  const selected = vehicules.find((v) => v.uuid === form.vehiculeUuid);
  // Le conducteur occupe une place : on ne peut en proposer davantage.
  const maxSeats = selected ? selected.seats - 1 : 8;

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!form.vehiculeUuid) {
      setError(t("publier.selectionnezVehicule"));
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
          t("commun.erreurGenerique"),
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
          {t("commun.retour")}
        </Button>
        <div>
          <h1>{t("publier.titre")}</h1>
          <p className="ct__lead">{t("publier.accroche")}</p>
        </div>
      </header>

      {success && (
        <Alert tone="success" title={t("publier.publie")}>
          {t("publier.redirection")}
        </Alert>
      )}
      {error && <Alert tone="danger" onDismiss={() => setError(null)}>{error}</Alert>}

      {loading ? (
        <Spinner size="lg" center showLabel label={t("publier.chargementVehicules")} />
      ) : vehicules.length === 0 ? (
        <EmptyState
          icon={<FaCar />}
          title={t("publier.aucunVehicule")}
          description={t("publier.aucunVehiculeTexte")}
          action={<Button to="/dashboard?tab=vehicles">{t("publier.ajouterVehicule")}</Button>}
        />
      ) : (
        <form onSubmit={submit} className="stack-6">
          <Card title={<><FaMapMarkerAlt aria-hidden="true" /> {t("publier.itineraire")}</>}>
            <div className="ct__grid">
              <Input label={t("publier.villeDepart")} placeholder={t("publier.villeDepartExemple")} required
                value={form.departureCity} onChange={(e) => set("departureCity", e.target.value)} />
              <Input label={t("publier.villeArrivee")} placeholder={t("publier.villeArriveeExemple")} required
                value={form.arrivalCity} onChange={(e) => set("arrivalCity", e.target.value)} />
              <Input label={t("publier.pointDepart")} placeholder={t("publier.pointDepartExemple")}
                hint={t("publier.pointDepartAide")}
                value={form.departureAddress} onChange={(e) => set("departureAddress", e.target.value)} />
              <Input label={t("publier.pointArrivee")} placeholder={t("publier.pointArriveeExemple")}
                value={form.arrivalAddress} onChange={(e) => set("arrivalAddress", e.target.value)} />
            </div>
          </Card>

          <Card title={<><FaCalendarAlt aria-hidden="true" /> {t("publier.datePlaces")}</>}>
            <div className="ct__grid">
              <Input label={t("publier.dateHeure")} type="datetime-local" min={minDateTime} required
                hint={t("publier.dateHeureAide")}
                value={form.departureTime} onChange={(e) => set("departureTime", e.target.value)} />
              <Input label={t("publier.placesProposees")} type="number" min={1} max={maxSeats} required
                hint={selected ? t("publier.placesProposeesAide", { max: maxSeats }) : undefined}
                value={form.availableSeats}
                onChange={(e) => set("availableSeats", parseInt(e.target.value) || 1)} />
              <Input label={t("publier.prixParPlace")} type="number" min={0} step={0.5} placeholder="4.50" required
                hint={t("publier.prixParPlaceAide")}
                value={form.pricePerSeat} onChange={(e) => set("pricePerSeat", e.target.value)} />
            </div>
          </Card>

          <Card title={<><FaCar aria-hidden="true" /> {t("publier.vehicule")}</>}>
            <fieldset className="ct__vehicles">
              <legend className="sr-only">{t("publier.choisirVehicule")}</legend>
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
                      <FaUsers aria-hidden="true" />{" "}
                      {t("publier.placesVehicule", { n: v.seats })} ·{" "}
                      {t(`energie.${v.energy}`) || v.energy}
                    </span>
                  </span>
                  {form.vehiculeUuid === v.uuid && <FiCheck aria-hidden="true" className="ct__check" />}
                </label>
              ))}
            </fieldset>
          </Card>

          <Card title={t("publier.details")}>
            <Textarea
              label={t("publier.description")}
              hint={t("publier.descriptionAide")}
              maxLength={500}
              showCount
              value={form.description}
              onChange={(e) => set("description", e.target.value)}
            />

            <fieldset className="ct__prefs">
              <legend className="ct__prefs-legend">{t("publier.preferences")}</legend>
              {PREFERENCES.map(({ key, cle, icon }) => (
                <label
                  key={key}
                  className={`ct__pref ${(form as Record<string, unknown>)[key] ? "is-on" : ""}`}
                >
                  <input type="checkbox"
                    checked={!!(form as Record<string, unknown>)[key]}
                    onChange={(e) => set(key, e.target.checked)} />
                  <span aria-hidden="true">{icon}</span>
                  {t(`publier.${cle}`)}
                </label>
              ))}
            </fieldset>
          </Card>

          <div className="ct__footer">
            <Button type="button" variant="ghost" onClick={() => navigate(-1)} disabled={saving}>
              {t("commun.annuler")}
            </Button>
            <Button type="submit" size="lg" loading={saving}>
              {t("publier.publierLeTrajet")}
            </Button>
          </div>
        </form>
      )}
    </div>
  );
}
