import { useState, useEffect } from "react";
import { FaStar, FaTicketAlt, FaPhoneAlt, FaCar } from "react-icons/fa";
import { FiArrowRight, FiSearch } from "react-icons/fi";
import axios from "axios";
import { API_BASE } from "../../config/api";
import { formatTripDate } from "./bookingStatus";
import {
  Alert, Avatar, Button, Card, EmptyState, Modal, Spinner, StatusBadge,
  type Status,
} from "../../components/ui";
import { useLang } from "../../context/LangContext";
import { LANGUES } from "../../i18n";
import "./BookingsPage.css";
import { useSeo } from "../../hooks/useSeo";

interface Booking {
  uuid: string;
  seatsBooked: number;
  totalPrice: number;
  status: string;
  statusReason?: string;
  createdAt: string;
  trip: {
    uuid: string;
    departureCity: string;
    arrivalCity: string;
    departureTime: string;
    pricePerSeat: number;
    driverFirstname: string;
    driverLastname: string;
    driverPictureUrl?: string;
    driverAverageRating: number;
    driverPhoneNumber?: string;
    vehiculeBrand: string;
    vehiculeModel: string;
  };
}

/** Le liseré de la carte reprend la couleur du statut. */
const TONE: Record<string, "brand" | "eco" | "pending" | "danger" | undefined> = {
  PENDING: "pending",
  CONFIRMED: "eco",
  REJECTED: "danger",
  CANCELLED: "danger",
  COMPLETED: undefined,
};

/** F30 — Le passager consulte et gère ses réservations. */
export default function MyBookingsPage() {
  const { langue, t } = useLang();
  /* Le format de date suit la langue : il était figé en fr-FR. */

  /* Sans ces metadonnees, l'onglet gardait le titre francais
     d'index.html, quelle que soit la langue choisie. */
  useSeo({
    titre: t("pages.reservationsTitre"),
    description: t("pages.reservationsDescription"),
    chemin: "/bookings",
    horsIndex: true,
  });
  const balise = LANGUES[langue].balise;
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  /* Remplace window.confirm : le navigateur ne stylise pas ses fenetres et
     leur contenu echappe aux lecteurs d'ecran de la page. */
  const [toCancel, setToCancel] = useState<Booking | null>(null);

  const headers = () => ({
    Authorization: `Bearer ${localStorage.getItem("coshift_token") ?? ""}`,
  });

  useEffect(() => {
    (async () => {
      try {
        const res = await axios.get(`${API_BASE}/api/bookings/mine`, { headers: headers() });
        setBookings(res.data);
      } catch (err) {
        setError(
          (axios.isAxiosError(err) && err.response?.data?.message) ||
            t("reservations.indisponibles"),
        );
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const cancel = async () => {
    if (!toCancel) return;
    const uuid = toCancel.uuid;
    setBusy(uuid);
    setError(null);
    try {
      const res = await axios.patch(
        `${API_BASE}/api/bookings/${uuid}/cancel`, {}, { headers: headers() },
      );
      setBookings((prev) => prev.map((b) => (b.uuid === uuid ? { ...b, ...res.data } : b)));
      setToCancel(null);
    } catch (err) {
      setError(
        (axios.isAxiosError(err) && err.response?.data?.message) || t("reservations.annulationEchouee"),
      );
    } finally {
      setBusy(null);
    }
  };

  const isCancellable = (b: Booking) =>
    (b.status === "PENDING" || b.status === "CONFIRMED") &&
    new Date(b.trip.departureTime) > new Date();

  return (
    <div className="container page stack-8">
      <header className="bk-header">
        <div>
          <h1>{t("reservations.titre")}</h1>
          <p className="bk-lead">{t("reservations.accroche")}</p>
        </div>
        <Button to="/trips/search" icon={<FiSearch />}>
          {t("reservations.trouverTrajet")}
        </Button>
      </header>

      {error && <Alert tone="danger" onDismiss={() => setError(null)}>{error}</Alert>}

      {loading ? (
        <Spinner size="lg" center showLabel label={t("reservations.chargement")} />
      ) : bookings.length === 0 ? (
        <EmptyState
          icon={<FaTicketAlt />}
          title={t("reservations.aucune")}
          description={t("reservations.aucuneTexte")}
          action={<Button to="/trips/search" icon={<FiSearch />}>{t("reservations.trouverTrajet")}</Button>}
        />
      ) : (
        <div className="grid-auto">
          {bookings.map((b) => (
            <Card
              key={b.uuid}
              tone={TONE[b.status]}
              title={
                <span className="bk-route">
                  {b.trip.departureCity}
                  <FiArrowRight aria-hidden="true" />
                  {b.trip.arrivalCity}
                </span>
              }
              action={<StatusBadge status={b.status as Status} size="sm" />}
            >
              <p className="bk-date">
                {formatTripDate(b.trip.departureTime, balise, t("carte.aHeure"))}
              </p>

              {b.statusReason && (
                <p className="bk-reason">{t("reservations.motif")} {b.statusReason}</p>
              )}

              <div className="bk-body">
                <Avatar
                  src={b.trip.driverPictureUrl}
                  name={`${b.trip.driverFirstname} ${b.trip.driverLastname}`}
                />
                <div className="bk-person">
                  <p className="bk-person-name">
                    {b.trip.driverFirstname} {b.trip.driverLastname}
                  </p>
                  <p className="bk-person-meta">
                    {b.trip.driverAverageRating > 0 ? (
                      <>
                        <FaStar aria-hidden="true" className="bk-star" />
                        {b.trip.driverAverageRating.toFixed(1)}
                      </>
                    ) : (
                      t("reservations.nouveauConducteur")
                    )}
                    {" · "}
                    <FaCar aria-hidden="true" /> {b.trip.vehiculeBrand} {b.trip.vehiculeModel}
                  </p>
                </div>
                <p className="bk-price">{b.totalPrice.toFixed(2)} €</p>
              </div>

              <p className="bk-seats">
                {b.seatsBooked > 1
                  ? t("reservations.reservee_plusieurs", { n: b.seatsBooked })
                  : t("reservations.reservee_une", { n: b.seatsBooked })}
              </p>

              {/* Le téléphone n'arrive du serveur qu'une fois la réservation confirmée. */}
              {b.trip.driverPhoneNumber && (
                <a className="bk-contact" href={`tel:${b.trip.driverPhoneNumber}`}>
                  <FaPhoneAlt aria-hidden="true" /> {b.trip.driverPhoneNumber}
                </a>
              )}

              <div className="bk-actions">
                <Button variant="secondary" size="sm" to={`/trips/${b.trip.uuid}`}>
                  {t("reservations.voirLeTrajet")}
                </Button>
                {isCancellable(b) && (
                  <Button variant="ghost" size="sm" onClick={() => setToCancel(b)}>
                    {t("commun.annuler")}
                  </Button>
                )}
              </div>
            </Card>
          ))}
        </div>
      )}

      <Modal
        open={toCancel !== null}
        onClose={() => setToCancel(null)}
        title={t("reservations.annulerTitre")}
        size="sm"
        footer={
          <>
            <Button variant="ghost" onClick={() => setToCancel(null)}>{t("commun.retour")}</Button>
            <Button variant="danger" loading={busy !== null} onClick={cancel}>
              {t("reservations.confirmerAnnulation")}
            </Button>
          </>
        }
      >
        {toCancel && (
          <p>
            {t("reservations.annulerTexte", {
              trajet: `${toCancel.trip.departureCity} → ${toCancel.trip.arrivalCity}`,
            })}
          </p>
        )}
      </Modal>
    </div>
  );
}
