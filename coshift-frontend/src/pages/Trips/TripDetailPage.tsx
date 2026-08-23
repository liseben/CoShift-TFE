import { useState, useEffect, useCallback, type ReactElement } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  FaStar, FaUsers, FaCar, FaBolt, FaLeaf, FaGasPump,
  FaSuitcase, FaDog, FaMusic, FaComments, FaBuilding } from "react-icons/fa";
import { FiArrowLeft, FiClock, FiMapPin, FiXCircle } from "react-icons/fi";
import axios from "axios";
import { API_BASE } from "../../config/api";
import { useAuth } from "../../context/AuthContext";
import {
  Alert, Avatar, Button, Card, Modal, Spinner, StatusBadge, type Status,
} from "../../components/ui";
import { useLang } from "../../context/LangContext";
import { LANGUES } from "../../i18n";
import "./TripDetailPage.css";
import { useSeo } from "../../hooks/useSeo";

const ENERGY: Record<string, { label: string; icon: ReactElement }> = {
  ELECTRIC: { label: "ELECTRIC", icon: <FaBolt /> },
  HYBRID:   { label: "HYBRID",   icon: <FaLeaf /> },
  GASOLINE: { label: "GASOLINE", icon: <FaGasPump /> },
  DIESEL:   { label: "DIESEL",   icon: <FaGasPump /> },
  LPG:      { label: "GPL",        icon: <FaGasPump /> },
};

interface Trip {
  uuid: string;
  departureCity: string;
  departureAddress?: string;
  arrivalCity: string;
  arrivalAddress?: string;
  departureTime: string;
  availableSeats: number;
  pricePerSeat: number;
  description?: string;
  status: string;
  acceptsLuggage: boolean;
  acceptsPets: boolean;
  musicAllowed: boolean;
  talkingAllowed: boolean;
  driver: {
    uuid: string;
    firstname: string;
    lastname: string;
    pictureUrl?: string;
    averageRating: number;
    tripsCount: number;
  };
  vehicule: { brand: string; model: string; seats: number; energy: string; photoUrl?: string };
  /** Organisation a laquelle le trajet est ouvert. Absente s'il n'en a pas. */
  organization?: { uuid: string; name: string; slug: string; logoUrl?: string } | null;
}

/** F26 — Détail d'un trajet, et point d'entrée de la réservation (F27). */
export default function TripDetailPage() {
  const { langue, t } = useLang();
  /* Le format de date suit la langue : il etait fige en fr-FR. */

  /* Sans ces metadonnees, l'onglet gardait le titre francais
     d'index.html, quelle que soit la langue choisie. */
  useSeo({
    titre: t("pages.detailTitre"),
    description: t("pages.detailDescription"),
    horsIndex: true,
  });
  const balise = LANGUES[langue].balise;
  const { uuid } = useParams<{ uuid: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [trip, setTrip] = useState<Trip | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [seats, setSeats] = useState(1);
  const [booking, setBooking] = useState(false);
  const [success, setSuccess] = useState(false);
  const [cancelOpen, setCancelOpen] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [cancelDone, setCancelDone] = useState(false);

  const headers = () => ({
    Authorization: `Bearer ${localStorage.getItem("coshift_token") ?? ""}`,
  });

  const load = useCallback(async () => {
    try {
      const res = await axios.get<Trip>(`${API_BASE}/api/trips/${uuid}`, { headers: headers() });
      setTrip(res.data);
    } catch (err) {
      setError(
        (axios.isAxiosError(err) && err.response?.data?.message) || "Ce trajet est introuvable.",
      );
    } finally {
      setLoading(false);
    }
  }, [uuid]);

  useEffect(() => { load(); }, [load]);

  const book = async () => {
    setBooking(true);
    setError(null);
    try {
      await axios.post(
        `${API_BASE}/api/bookings`,
        { tripUuid: uuid, seatsBooked: seats },
        { headers: headers() },
      );
      setSuccess(true);
      // Le conducteur doit encore accepter : on rafraîchit pour refléter l'état réel.
      await load();
      setTimeout(() => navigate("/bookings"), 2000);
    } catch (err) {
      setError(
        (axios.isAxiosError(err) && err.response?.data?.message) ||
          t("detail.reservationImpossible"),
      );
    } finally {
      setBooking(false);
    }
  };

  /* F18 — Annulation par le conducteur. Le backend passe en cascade toutes les
     réservations en attente ou confirmées en CANCELLED : d'où la confirmation
     explicite avant l'appel, l'action étant irréversible. */
  const cancelTrip = async () => {
    setCancelling(true);
    setError(null);
    try {
      const res = await axios.patch<Trip>(
        `${API_BASE}/api/trips/${uuid}/cancel`,
        {},
        { headers: headers() },
      );
      setTrip(res.data);
      setCancelOpen(false);
      setCancelDone(true);
    } catch (err) {
      setCancelOpen(false);
      setError(
        (axios.isAxiosError(err) && err.response?.data?.message) ||
          t("detail.annulationImpossible"),
      );
    } finally {
      setCancelling(false);
    }
  };

  if (loading) {
    return (
      <div className="container page">
        <Spinner size="lg" center showLabel label={t("detail.chargement")} />
      </div>
    );
  }

  if (!trip) {
    return (
      <div className="container page stack-6">
        <Alert tone="danger">{error ?? t("detail.introuvable")}</Alert>
        <Button variant="secondary" icon={<FiArrowLeft />} to="/trips/search">
          {t("detail.retourRecherche")}
        </Button>
      </div>
    );
  }

  const dt = new Date(trip.departureTime);
  const jour = dt.toLocaleDateString(balise, {
    weekday: "long", day: "numeric", month: "long", year: "numeric",
  });
  const heure = dt.toLocaleTimeString(balise, { hour: "2-digit", minute: "2-digit" });

  /* Comparaison sur l'identifiant public et non sur le nom : deux homonymes
     seraient sinon confondus, et l'un se verrait refuser la réservation. */
  const isOwnTrip = !!user?.uuid && user.uuid === trip.driver.uuid;
  const isBookable = trip.status === "PLANNED" && trip.availableSeats > 0 && !isOwnTrip;

  /* Le backend refuse d'annuler un trajet déjà parti, et la tâche planifiée
     bascule seule les trajets passés en COMPLETED : le bouton n'a de sens que
     sur un trajet à venir encore actif. */
  const isPast = dt.getTime() < Date.now();
  const isCancellable =
    isOwnTrip && !isPast && (trip.status === "PLANNED" || trip.status === "FULL");
  const energy = ENERGY[trip.vehicule.energy] ?? { label: trip.vehicule.energy, icon: <FaCar /> };
  const driverName = `${trip.driver.firstname} ${trip.driver.lastname}`;

  const prefs = [
    { on: trip.acceptsLuggage, icon: <FaSuitcase />, yes: "bagagesAcceptes", no: "bagagesRefuses" },
    { on: trip.acceptsPets, icon: <FaDog />, yes: "animauxAcceptes", no: "animauxRefuses" },
    { on: trip.musicAllowed, icon: <FaMusic />, yes: "musiqueAutorisee", no: "sansMusique" },
    { on: trip.talkingAllowed, icon: <FaComments />, yes: "discussionBienvenue", no: "trajetSilencieux" },
  ];

  return (
    <div className="container page stack-6">
      <Button variant="ghost" size="sm" icon={<FiArrowLeft />} onClick={() => navigate(-1)}>
        {t("commun.retour")}
      </Button>

      {success && (
        <Alert tone="success" title={t("detail.demandeEnvoyee")}>
          {t("detail.demandeEnvoyeeTexte")}
        </Alert>
      )}
      {cancelDone && (
        <Alert tone="success" title="Trajet annulé">
          Il n'apparaît plus dans les recherches. Les réservations qui le
          concernaient ont été annulées.
        </Alert>
      )}
      {error && !success && <Alert tone="danger" onDismiss={() => setError(null)}>{error}</Alert>}

      <div className="td__grid">
        <div className="stack-6">
          <Card
            title={`${trip.departureCity} → ${trip.arrivalCity}`}
            action={<StatusBadge status={trip.status as Status} size="sm" />}
          >
            <p className="td__date">
              <FiClock aria-hidden="true" /> {jour}
            </p>

            {/* Le rattachement figure sur la fiche et non sur les cartes de
                resultats : dans une recherche, toutes les cartes portent la
                meme organisation, l'information n'y apprend rien. Ici, elle
                repond a la question « pourquoi est-ce que je vois ce trajet ». */}
            {trip.organization && (
              <p className="td__cercle">
                <FaBuilding aria-hidden="true" />
                {t("organisation.porteLe", { nom: trip.organization.name })}
              </p>
            )}

            {/* Itinéraire en liste ordonnée : l'ordre des étapes porte du sens. */}
            <ol className="td__route">
              <li className="td__stop">
                <span className="td__time">{heure}</span>
                <span className="td__dot td__dot--dep" aria-hidden="true" />
                <span className="td__place">
                  <span className="td__city">{trip.departureCity}</span>
                  {trip.departureAddress && (
                    <span className="td__address">
                      <FiMapPin aria-hidden="true" /> {trip.departureAddress}
                    </span>
                  )}
                </span>
              </li>
              <li className="td__stop">
                <span className="td__time" aria-hidden="true" />
                <span className="td__dot td__dot--arr" aria-hidden="true" />
                <span className="td__place">
                  <span className="td__city">{trip.arrivalCity}</span>
                  {trip.arrivalAddress && (
                    <span className="td__address">
                      <FiMapPin aria-hidden="true" /> {trip.arrivalAddress}
                    </span>
                  )}
                </span>
              </li>
            </ol>

            {trip.description && (
              <div className="td__description">
                <p className="td__label">Précisions du conducteur</p>
                <p>{trip.description}</p>
              </div>
            )}

            <ul className="td__prefs">
              {prefs.map((p, i) => (
                <li key={i} className={`td__chip ${p.on ? "is-on" : ""}`}>
                  <span aria-hidden="true">{p.icon}</span> {t(`detail.${p.on ? p.yes : p.no}`)}
                </li>
              ))}
            </ul>
          </Card>

          <Card title={t("detail.conducteur")}>
            <div className="td__person">
              <Avatar src={trip.driver.pictureUrl} name={driverName} size="lg" />
              <div>
                <p className="td__person-name">{driverName}</p>
                <p className="td__person-meta">
                  {trip.driver.averageRating > 0 ? (
                    <>
                      <FaStar aria-hidden="true" className="td__star" />
                      {trip.driver.averageRating.toFixed(1)} / 5 ·{" "}
                    </>
                  ) : (
                    `${t("detail.nouveauConducteur")} · `
                  )}
                  {trip.driver.tripsCount !== 1
                    ? t("detail.trajet_plusieurs", { n: trip.driver.tripsCount })
                    : t("detail.trajet_un", { n: trip.driver.tripsCount })}
                </p>
              </div>
            </div>
          </Card>

          <Card title={t("detail.vehicule")}>
            <div className="td__person">
              <span className="td__vehicle-icon" aria-hidden="true"><FaCar /></span>
              <div>
                <p className="td__person-name">
                  {trip.vehicule.brand} {trip.vehicule.model}
                </p>
                <p className="td__person-meta">
                  <span aria-hidden="true">{energy.icon}</span>{" "}
                  {t(`energie.${energy.label}`)} ·{" "}
                  <FaUsers aria-hidden="true" />{" "}
                  {t("detail.placesVehicule", { n: trip.vehicule.seats })}
                </p>
              </div>
            </div>
          </Card>
        </div>

        {/* ── Panneau de réservation ── */}
        <aside className="td__aside">
          <Card padding="lg">
            <p className="td__price">
              {trip.pricePerSeat.toFixed(2)} €
              <span className="td__price-unit">{t("detail.parPlace")}</span>
            </p>

            <p className="td__seats">
              {trip.availableSeats > 0 ? (
                <>
                  <FaUsers aria-hidden="true" />{" "}
                  {trip.availableSeats > 1
                    ? t("detail.placesRestantes_plusieurs", { n: trip.availableSeats })
                    : t("detail.placesRestantes_une", { n: trip.availableSeats })}
                </>
              ) : (
                t("detail.complet")
              )}
            </p>

            {isBookable ? (
              <>
                <p className="td__label" id="td-seats-label">{t("detail.nombreDePlaces")}</p>
                <div className="td__picker" role="group" aria-labelledby="td-seats-label">
                  <button type="button" onClick={() => setSeats((s) => Math.max(1, s - 1))}
                          disabled={seats <= 1} aria-label={t("detail.retirerPlace")}>−</button>
                  <output aria-live="polite">{seats}</output>
                  <button type="button"
                          onClick={() => setSeats((s) => Math.min(trip.availableSeats, s + 1))}
                          disabled={seats >= trip.availableSeats} aria-label={t("detail.ajouterPlace")}>+</button>
                </div>

                <p className="td__total">
                  <span>{t("detail.total")}</span>
                  <strong>{(trip.pricePerSeat * seats).toFixed(2)} €</strong>
                </p>

                <Button variant="eco" size="lg" block loading={booking}
                        disabled={success} onClick={book}>
                  {success ? t("detail.demandeEnvoyee") : t("detail.reserver")}
                </Button>

                <p className="td__hint">{t("detail.indicationDemande")}</p>
              </>
            ) : (
              <Alert tone="info">
                {isOwnTrip
                  ? t("detail.vousEtesConducteur")
                  : trip.status !== "PLANNED"
                    ? t("detail.plusDeReservation")
                    : t("detail.plusDePlace")}
              </Alert>
            )}

            {isCancellable && (
              <div className="td__owner-actions">
                <Button
                  variant="danger"
                  size="lg"
                  block
                  icon={<FiXCircle />}
                  onClick={() => setCancelOpen(true)}
                >
                  {t("detail.annulerCeTrajet")}
                </Button>
                <p className="td__hint">{t("detail.annulerIndication")}</p>
              </div>
            )}
          </Card>
        </aside>
      </div>

      <Modal
        open={cancelOpen}
        onClose={() => setCancelOpen(false)}
        title={t("detail.annulerTitre")}
        size="sm"
        footer={
          <>
            <Button variant="ghost" onClick={() => setCancelOpen(false)}>
              {t("commun.retour")}
            </Button>
            <Button variant="danger" loading={cancelling} onClick={cancelTrip}>
              {t("detail.annulerBouton")}
            </Button>
          </>
        }
      >
        <p>
          {t("detail.annulerP1", {
            trajet: `${trip.departureCity} → ${trip.arrivalCity}`,
            quand: `${jour}${t("carte.aHeure")}${heure}`,
          })}
        </p>
        <p>{t("detail.annulerP2")}</p>
      </Modal>
    </div>
  );
}
