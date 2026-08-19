import { useState, useEffect } from "react";
import { FaStar, FaInbox, FaPhoneAlt, FaUsers } from "react-icons/fa";
import { FiCheck, FiX } from "react-icons/fi";
import axios from "axios";
import { API_BASE } from "../../config/api";
import {
  Alert, Avatar, Button, Card, EmptyState, Modal, Spinner, StatusBadge, Textarea,
  type Status,
} from "../../components/ui";
import "./BookingsPage.css";

interface Received {
  uuid: string;
  seatsBooked: number;
  totalPrice: number;
  status: string;
  statusReason?: string;
  createdAt: string;
  passenger: {
    uuid: string;
    firstname: string;
    lastname: string;
    pictureUrl?: string;
    averageRating: number;
    tripsCount: number;
    phoneNumber?: string;
  };
}

const TONE: Record<string, "brand" | "eco" | "pending" | "danger" | undefined> = {
  PENDING: "pending",
  CONFIRMED: "eco",
  REJECTED: "danger",
  CANCELLED: "danger",
  COMPLETED: undefined,
};

/**
 * F19 / F20 — Le conducteur consulte les demandes reçues sur ses trajets,
 * puis les accepte ou les refuse. S'affiche comme onglet du tableau de bord.
 */
export default function ReceivedBookingsPage() {
  const [items, setItems] = useState<Received[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [toReject, setToReject] = useState<Received | null>(null);
  const [reason, setReason] = useState("");

  const headers = () => ({
    Authorization: `Bearer ${localStorage.getItem("coshift_token") ?? ""}`,
  });

  useEffect(() => {
    (async () => {
      try {
        const res = await axios.get(`${API_BASE}/api/bookings/received`, { headers: headers() });
        setItems(res.data);
      } catch (err) {
        setError(
          (axios.isAxiosError(err) && err.response?.data?.message) ||
            "Impossible de charger les demandes reçues.",
        );
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const decide = async (uuid: string, action: "accept" | "reject", motif?: string) => {
    setBusy(uuid);
    setError(null);
    try {
      const res = await axios.patch(
        `${API_BASE}/api/bookings/${uuid}/${action}`,
        action === "reject" ? { reason: motif ?? "" } : {},
        { headers: headers() },
      );
      setItems((prev) => prev.map((b) => (b.uuid === uuid ? { ...b, ...res.data } : b)));
      setToReject(null);
      setReason("");
    } catch (err) {
      setError(
        (axios.isAxiosError(err) && err.response?.data?.message) || "L'opération a échoué.",
      );
    } finally {
      setBusy(null);
    }
  };

  const pending = items.filter((b) => b.status === "PENDING").length;

  return (
    <div className="stack-6">
      <header className="bk-header">
        <div>
          <h2>Demandes reçues</h2>
          <p className="bk-lead">
            {pending > 0
              ? `${pending} demande${pending > 1 ? "s" : ""} en attente de votre réponse.`
              : "Les passagers qui ont demandé une place dans vos trajets."}
          </p>
        </div>
      </header>

      {error && <Alert tone="danger" onDismiss={() => setError(null)}>{error}</Alert>}

      {loading ? (
        <Spinner size="lg" center showLabel label="Chargement des demandes" />
      ) : items.length === 0 ? (
        <EmptyState
          icon={<FaInbox />}
          title="Aucune demande pour l'instant"
          description="Les demandes de réservation sur vos trajets apparaîtront ici."
        />
      ) : (
        <div className="grid-auto">
          {items.map((b) => {
            const name = `${b.passenger.firstname} ${b.passenger.lastname}`;
            return (
              <Card
                key={b.uuid}
                tone={TONE[b.status]}
                title={name}
                action={<StatusBadge status={b.status as Status} size="sm" />}
              >
                <p className="bk-date">
                  <FaUsers aria-hidden="true" /> {b.seatsBooked} place
                  {b.seatsBooked > 1 ? "s" : ""} · {b.totalPrice.toFixed(2)} €
                </p>

                <div className="bk-body">
                  <Avatar src={b.passenger.pictureUrl} name={name} />
                  <div className="bk-person">
                    <p className="bk-person-meta">
                      {b.passenger.averageRating > 0 ? (
                        <>
                          <FaStar aria-hidden="true" className="bk-star" />
                          {b.passenger.averageRating.toFixed(1)}
                        </>
                      ) : (
                        "Nouveau passager"
                      )}
                      {" · "}
                      {b.passenger.tripsCount} trajet
                      {b.passenger.tripsCount > 1 ? "s" : ""}
                    </p>
                  </div>
                </div>

                {b.statusReason && <p className="bk-reason">Motif : {b.statusReason}</p>}

                {b.passenger.phoneNumber && (
                  <a className="bk-contact" href={`tel:${b.passenger.phoneNumber}`}>
                    <FaPhoneAlt aria-hidden="true" /> {b.passenger.phoneNumber}
                  </a>
                )}

                {b.status === "PENDING" && (
                  <div className="bk-actions">
                    <Button
                      variant="eco"
                      size="sm"
                      icon={<FiCheck />}
                      loading={busy === b.uuid}
                      onClick={() => decide(b.uuid, "accept")}
                    >
                      Accepter
                    </Button>
                    <Button
                      variant="secondary"
                      size="sm"
                      icon={<FiX />}
                      onClick={() => setToReject(b)}
                    >
                      Refuser
                    </Button>
                  </div>
                )}
              </Card>
            );
          })}
        </div>
      )}

      <Modal
        open={toReject !== null}
        onClose={() => setToReject(null)}
        title="Refuser cette demande ?"
        size="sm"
        footer={
          <>
            <Button variant="ghost" onClick={() => setToReject(null)}>Retour</Button>
            <Button
              variant="danger"
              loading={busy !== null}
              onClick={() => toReject && decide(toReject.uuid, "reject", reason)}
            >
              Refuser
            </Button>
          </>
        }
      >
        <Textarea
          label="Motif du refus"
          hint="Facultatif, mais le passager le verra. Une raison brève évite les malentendus."
          maxLength={200}
          showCount
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          placeholder="La voiture est déjà complète pour ce trajet."
        />
      </Modal>
    </div>
  );
}
