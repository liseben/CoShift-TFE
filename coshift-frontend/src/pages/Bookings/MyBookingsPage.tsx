import { useState, useEffect } from "react";
import { FaStar, FaTicketAlt, FaPhoneAlt, FaCar, FaRegStar } from "react-icons/fa";
import { FiArrowRight, FiCheckCircle, FiSearch , FiCreditCard} from "react-icons/fi";
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

/** Cle de traduction de l'etat comptable. */
const ETAT_PAIEMENT: Record<string, string> = {
  DUE: "paiement.du",
  PAID: "paiement.regle",
  REFUNDED: "paiement.rembourse",
  PARTIALLY_REFUNDED: "paiement.rembourseEnPartie",
  CANCELLED: "paiement.annule",
  FAILED: "paiement.echoue",
};

interface Booking {
  uuid: string;
  seatsBooked: number;
  totalPrice: number;
  status: string;
  statusReason?: string;
  completedAt?: string | null;
  reviewed?: boolean;
  createdAt: string;
  /** Etat comptable. Absent si la reservation precede le partage de frais. */
  paiement?: {
    uuid: string;
    amount: number;
    currency: string;
    status: "DUE" | "PAID" | "REFUNDED" | "PARTIALLY_REFUNDED" | "CANCELLED" | "FAILED";
    refundedAmount: number;
    refundReason?: string | null;
    paidAt?: string | null;
    provider: string;
  } | null;
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
  /* Ce qui serait rendu si l'annulation etait confirmee maintenant. Demande au
     serveur : le bareme est une regle metier, la recopier ici ouvrirait la
     possibilite que les deux versions divergent — et c'est l'ecran qui aurait
     tort. */
  const [bareme, setBareme] = useState<{ partRendue: number; montantRendu: number } | null>(null);

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

  const libellePaiement = (statut: string) => t(ETAT_PAIEMENT[statut] ?? "paiement.etat");

  /** Acquitte le montant du. */
  const regler = async (uuid: string) => {
    setBusy(uuid);
    setError(null);
    try {
      const res = await axios.post<Booking>(
        `${API_BASE}/api/bookings/${uuid}/payment`, {}, { headers: headers() },
      );
      setBookings((liste) => liste.map((b) => (b.uuid === uuid ? res.data : b)));
    } catch (e) {
      setError(
        (axios.isAxiosError(e) && e.response?.data?.message) || t("commun.erreurGenerique"),
      );
    } finally {
      setBusy(null);
    }
  };

  /* Le bareme est annonce AVANT la confirmation. Decouvrir apres coup qu'on ne
     recupere que la moitie est le genre de surprise qui vaut une reclamation. */
  const ouvrirAnnulation = (b: Booking) => {
    setToCancel(b);
    setBareme(null);
    axios
      .get<{ partRendue: number; montantRendu: number }>(
        `${API_BASE}/api/bookings/${b.uuid}/remboursement`, { headers: headers() },
      )
      .then((r) => setBareme(r.data))
      .catch(() => setBareme(null));
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
      {/* Le bouton dit « Régler », les conditions générales disent qu'aucun
          paiement n'est perçu. Les deux sont vrais, et l'écran doit dire
          pourquoi plutôt que de laisser le lecteur trancher : le prestataire
          enregistré est une simulation. */}
      {bookings.some((b) => b.paiement?.provider === "SIMULATION") && (
        <Alert tone="info">{t("paiement.simulation")}</Alert>
      )}

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

              {/* Etat comptable. Affiche meme quand il n'y a rien a faire : savoir
                  qu'un montant est regle fait partie de ce qu'on vient verifier. */}
              {b.paiement && (
                <p className={`bk-paiement bk-paiement--${b.paiement.status.toLowerCase()}`}>
                  <FiCreditCard aria-hidden="true" />
                  <span>{t("paiement.etat")} : {libellePaiement(b.paiement.status)}</span>
                  {b.paiement.refundedAmount > 0 && (
                    <span className="bk-paiement-detail">
                      {t("paiement.rembourseDe", { montant: b.paiement.refundedAmount.toFixed(2) })}
                    </span>
                  )}
                  {b.paiement.refundReason && (
                    <span className="bk-paiement-detail">
                      {t("paiement.motif", { motif: b.paiement.refundReason })}
                    </span>
                  )}
                </p>
              )}

              <div className="bk-actions">
                {b.paiement?.status === "DUE" && b.status !== "CANCELLED" && b.status !== "REJECTED" && (
                  <Button
                    size="sm"
                    icon={<FiCreditCard />}
                    loading={busy === b.uuid}
                    onClick={() => regler(b.uuid)}
                  >
                    {t("paiement.payer", { montant: b.paiement.amount.toFixed(2) })}
                  </Button>
                )}
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
                  <Button variant="ghost" size="sm" onClick={() => ouvrirAnnulation(b)}>
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
          <>
            <p>
              {t("reservations.annulerTexte", {
                trajet: `${toCancel.trip.departureCity} → ${toCancel.trip.arrivalCity}`,
              })}
            </p>

            {/* Ce qui sera rendu, annonce avant la decision et non apres. */}
            {bareme && toCancel.paiement?.status === "PAID" && (
              <div className="bk-bareme">
                <p className="bk-bareme-titre">{t("paiement.baremeTitre")}</p>
                <p>
                  {bareme.partRendue === 100
                    ? t("paiement.baremeIntegral", { montant: bareme.montantRendu.toFixed(2) })
                    : bareme.partRendue === 0
                      ? t("paiement.baremeRien")
                      : t("paiement.baremePartiel", {
                          part: bareme.partRendue,
                          montant: bareme.montantRendu.toFixed(2),
                        })}
                </p>
              </div>
            )}
          </>
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
