import { useState, useEffect } from "react";
import { FaStar, FaTicketAlt, FaPhoneAlt, FaCar, FaRegStar } from "react-icons/fa";
import { FiArrowRight, FiCheckCircle, FiSearch } from "react-icons/fi";
import axios from "axios";
import { API_BASE } from "../../config/api";
import { formatTripDate } from "./bookingStatus";
import {
  Alert, Avatar, Button, Card, EmptyState, Modal, RatingInput, Spinner,
  StatusBadge, Textarea, type Status,
} from "../../components/ui";
import { useAuth } from "../../context/AuthContext";
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
  completedAt?: string | null;
  reviewed?: boolean;
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
  const { rafraichir: rafraichirProfil } = useAuth();
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

  const confirmer = async (uuid: string) => {
    setBusy(uuid);
    setError(null);
    try {
      const res = await axios.patch(
        `${API_BASE}/api/bookings/${uuid}/complete`, {}, { headers: headers() },
      );
      setBookings((prev) => prev.map((b) => (b.uuid === uuid ? { ...b, ...res.data } : b)));
      /* Le compteur de trajets du profil vient de changer pour les deux
         participants : sans rechargement, l'en-tête du tableau de bord
         continuerait d'afficher l'ancienne valeur jusqu'à la prochaine
         connexion. */
      rafraichirProfil?.();
    } catch (err) {
      setError(
        (axios.isAxiosError(err) && err.response?.data?.message) ||
          t("reservations.confirmationEchouee"),
      );
    } finally {
      setBusy(null);
    }
  };

  /* Notation : la reservation visee, la note et le commentaire en cours. */
  const [aNoter, setANoter] = useState<Booking | null>(null);
  const [note, setNote] = useState(0);
  const [commentaire, setCommentaire] = useState("");
  const [erreurNote, setErreurNote] = useState<string | null>(null);
  const [succes, setSucces] = useState<string | null>(null);

  const ouvrirNotation = (b: Booking) => {
    setANoter(b);
    setNote(0);
    setCommentaire("");
    setErreurNote(null);
  };

  const envoyerAvis = async () => {
    if (!aNoter) return;
    /* Le serveur refuserait une note absente ; l'annoncer ici evite un
       aller-retour et un message d'erreur technique. */
    if (note < 1) {
      setErreurNote(t("reservations.noteRequise"));
      return;
    }
    const uuid = aNoter.uuid;
    setBusy(uuid);
    setError(null);
    try {
      await axios.post(
        `${API_BASE}/api/bookings/${uuid}/review`,
        { rating: note, comment: commentaire },
        { headers: headers() },
      );
      setBookings((prev) =>
        prev.map((b) => (b.uuid === uuid ? { ...b, reviewed: true } : b)));
      setANoter(null);
      setSucces(t("reservations.avisEnvoye"));
    } catch (err) {
      setErreurNote(
        (axios.isAxiosError(err) && err.response?.data?.message) ||
          t("reservations.avisEchoue"),
      );
    } finally {
      setBusy(null);
    }
  };

  const isCancellable = (b: Booking) =>
    (b.status === "PENDING" || b.status === "CONFIRMED") &&
    new Date(b.trip.departureTime) > new Date();

  /* La confirmation n'a de sens que sur une réservation acceptée dont le
     départ est passé. Le serveur applique la même règle — celle-ci ne fait
     qu'éviter d'afficher un bouton qui serait refusé. */
  const isConfirmable = (b: Booking) =>
    b.status === "CONFIRMED" && new Date(b.trip.departureTime) <= new Date();

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
      {succes && <Alert tone="success" onDismiss={() => setSucces(null)}>{succes}</Alert>}

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

              {/* Confirmer est irreversible : la date rappelle que c'est fait. */}
              {b.completedAt && (
                <p className="bk-reason">
                  {t("reservations.confirmeLe", {
                    date: new Date(b.completedAt).toLocaleDateString(balise),
                  })}
                </p>
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
                {b.status === "COMPLETED" && !b.reviewed && (
                  <Button
                    variant="secondary"
                    size="sm"
                    icon={<FaRegStar />}
                    onClick={() => ouvrirNotation(b)}
                  >
                    {t("reservations.noter")}
                  </Button>
                )}
                {isConfirmable(b) && (
                  <Button
                    size="sm"
                    icon={<FiCheckCircle />}
                    loading={busy === b.uuid}
                    onClick={() => confirmer(b.uuid)}
                  >
                    {t("reservations.confirmerTrajet")}
                  </Button>
                )}
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

      {/* Notation. Deposer un avis est definitif : l'intro le dit avant, pas
          apres. */}
      <Modal
        open={aNoter !== null}
        onClose={() => setANoter(null)}
        title={t("reservations.noterTitre")}
        size="sm"
        footer={
          <>
            <Button variant="ghost" onClick={() => setANoter(null)}>
              {t("commun.retour")}
            </Button>
            <Button loading={busy !== null} onClick={envoyerAvis}>
              {t("reservations.envoyerAvis")}
            </Button>
          </>
        }
      >
        {aNoter && (
          <div className="stack-4">
            <p className="bk-lead">
              {aNoter.trip.departureCity} → {aNoter.trip.arrivalCity} ·{" "}
              {aNoter.trip.driverFirstname} {aNoter.trip.driverLastname}
            </p>
            <p>{t("reservations.noterIntro")}</p>

            <RatingInput
              label={t("reservations.noteLabel")}
              value={note}
              onChange={(n) => {
                setNote(n);
                setErreurNote(null);
              }}
              libelles={[
                t("reservations.etoile_une"),
                t("reservations.etoile_deux"),
                t("reservations.etoile_trois"),
                t("reservations.etoile_quatre"),
                t("reservations.etoile_cinq"),
              ]}
              error={erreurNote ?? undefined}
            />

            <Textarea
              label={t("reservations.commentaireLabel")}
              placeholder={t("reservations.commentairePlaceholder")}
              value={commentaire}
              onChange={(e) => setCommentaire(e.target.value)}
              maxLength={500}
              rows={4}
            />
          </div>
        )}
      </Modal>
    </div>
  );
}
