import { useState, useEffect, useCallback, type ReactElement } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  FaStar, FaUsers, FaCar, FaBolt, FaLeaf, FaGasPump,
  FaSuitcase, FaDog, FaMusic, FaComments,
} from "react-icons/fa";
import { FiArrowLeft, FiClock, FiMapPin, FiXCircle } from "react-icons/fi";
import axios from "axios";
import { API_BASE } from "../../config/api";
import { useAuth } from "../../context/AuthContext";
import {
  Alert, Avatar, Button, Card, Modal, Spinner, StatusBadge, type Status,
} from "../../components/ui";
import "./TripDetailPage.css";

const ENERGY: Record<string, { label: string; icon: ReactElement }> = {
  ELECTRIC: { label: "Électrique", icon: <FaBolt /> },
  HYBRID:   { label: "Hybride",    icon: <FaLeaf /> },
  GASOLINE: { label: "Essence",    icon: <FaGasPump /> },
  DIESEL:   { label: "Diesel",     icon: <FaGasPump /> },
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
}

/** F26 — Détail d'un trajet, et point d'entrée de la réservation (F27). */
export default function TripDetailPage() {
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
          "La réservation n'a pas pu être enregistrée.",
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
          "Le trajet n'a pas pu être annulé.",
      );
    } finally {
      setCancelling(false);
    }
  };

  if (loading) {
    return (
      <div className="container page">
        <Spinner size="lg" center showLabel label="Chargement du trajet" />
      </div>
    );
  }

  if (!trip) {
    return (
      <div className="container page stack-6">
        <Alert tone="danger">{error ?? "Ce trajet est introuvable."}</Alert>
        <Button variant="secondary" icon={<FiArrowLeft />} to="/trips/search">
          Retour à la recherche
        </Button>
      </div>
    );
  }

  const dt = new Date(trip.departureTime);
  const jour = dt.toLocaleDateString("fr-FR", {
    weekday: "long", day: "numeric", month: "long", year: "numeric",
  });
  const heure = dt.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" });

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
    { on: trip.acceptsLuggage, icon: <FaSuitcase />, yes: "Bagages acceptés", no: "Bagages refusés" },
    { on: trip.acceptsPets, icon: <FaDog />, yes: "Animaux acceptés", no: "Animaux refusés" },
    { on: trip.musicAllowed, icon: <FaMusic />, yes: "Musique autorisée", no: "Sans musique" },
    { on: trip.talkingAllowed, icon: <FaComments />, yes: "Discussion bienvenue", no: "Trajet silencieux" },
  ];

  return (
    <div className="container page stack-6">
      <Button variant="ghost" size="sm" icon={<FiArrowLeft />} onClick={() => navigate(-1)}>
        Retour
      </Button>

      {success && (
        <Alert tone="success" title="Demande envoyée">
          Le conducteur doit maintenant l'accepter. Redirection vers vos réservations…
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
                  <span aria-hidden="true">{p.icon}</span> {p.on ? p.yes : p.no}
                </li>
              ))}
            </ul>
          </Card>

          <Card title="Conducteur">
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
                    "Nouveau conducteur · "
                  )}
                  {trip.driver.tripsCount} trajet{trip.driver.tripsCount !== 1 ? "s" : ""}
                </p>
              </div>
            </div>
          </Card>

          <Card title="Véhicule">
            <div className="td__person">
              <span className="td__vehicle-icon" aria-hidden="true"><FaCar /></span>
              <div>
                <p className="td__person-name">
                  {trip.vehicule.brand} {trip.vehicule.model}
                </p>
                <p className="td__person-meta">
                  <span aria-hidden="true">{energy.icon}</span> {energy.label} ·{" "}
                  <FaUsers aria-hidden="true" /> {trip.vehicule.seats} places
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
              <span className="td__price-unit"> / place</span>
            </p>

            <p className="td__seats">
              {trip.availableSeats > 0 ? (
                <>
                  <FaUsers aria-hidden="true" /> {trip.availableSeats} place
                  {trip.availableSeats > 1 ? "s" : ""} restante
                  {trip.availableSeats > 1 ? "s" : ""}
                </>
              ) : (
                "Complet"
              )}
            </p>

            {isBookable ? (
              <>
                <p className="td__label" id="td-seats-label">Nombre de places</p>
                <div className="td__picker" role="group" aria-labelledby="td-seats-label">
                  <button type="button" onClick={() => setSeats((s) => Math.max(1, s - 1))}
                          disabled={seats <= 1} aria-label="Retirer une place">−</button>
                  <output aria-live="polite">{seats}</output>
                  <button type="button"
                          onClick={() => setSeats((s) => Math.min(trip.availableSeats, s + 1))}
                          disabled={seats >= trip.availableSeats} aria-label="Ajouter une place">+</button>
                </div>

                <p className="td__total">
                  <span>Total</span>
                  <strong>{(trip.pricePerSeat * seats).toFixed(2)} €</strong>
                </p>

                <Button variant="eco" size="lg" block loading={booking}
                        disabled={success} onClick={book}>
                  {success ? "Demande envoyée" : "Réserver"}
                </Button>

                <p className="td__hint">
                  Votre demande part au conducteur. Elle n'est confirmée qu'après son accord.
                </p>
              </>
            ) : (
              <Alert tone="info">
                {isOwnTrip
                  ? "Vous êtes le conducteur de ce trajet."
                  : trip.status !== "PLANNED"
                    ? "Ce trajet n'accepte plus de réservation."
                    : "Il ne reste plus de place disponible."}
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
                  Annuler ce trajet
                </Button>
                <p className="td__hint">
                  Les demandes et réservations en cours seront annulées.
                </p>
              </div>
            )}
          </Card>
        </aside>
      </div>

      <Modal
        open={cancelOpen}
        onClose={() => setCancelOpen(false)}
        title="Annuler ce trajet ?"
        size="sm"
        footer={
          <>
            <Button variant="ghost" onClick={() => setCancelOpen(false)}>
              Retour
            </Button>
            <Button variant="danger" loading={cancelling} onClick={cancelTrip}>
              Annuler le trajet
            </Button>
          </>
        }
      >
        <p>
          Le trajet <strong>{trip.departureCity} → {trip.arrivalCity}</strong> du{" "}
          {jour} à {heure} sera retiré des recherches.
        </p>
        <p>
          Toutes les demandes en attente et les réservations déjà confirmées
          seront annulées avec le motif « Trajet annulé par le conducteur ».
          Cette action est définitive.
        </p>
      </Modal>
    </div>
  );
}
