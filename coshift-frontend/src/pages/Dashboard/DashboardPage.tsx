import { useState, useEffect, type ChangeEvent, type FormEvent } from "react";
import { Navigate, useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { FaCar, FaTicketAlt, FaLeaf, FaStar, FaInbox, FaUsers } from "react-icons/fa";
import { FiEdit3, FiCamera, FiGrid, FiTruck, FiArrowRight } from "react-icons/fi";
import axios from "axios";
import VehiclePage from "./VehiclePage";
import ReceivedBookingsPage from "../Bookings/ReceivedBookingsPage";
import { formatTripDate } from "../Bookings/bookingStatus";
import { API_BASE } from "../../config/api";
import {
  Alert, Avatar, Button, Card, Input, Modal, Spinner, StatusBadge, type Status,
} from "../../components/ui";
import "./DashboardPage.css";

type TabKey = "overview" | "requests" | "vehicles";

interface MyTrip {
  uuid: string;
  departureCity: string;
  arrivalCity: string;
  departureTime: string;
  availableSeats: number;
  pricePerSeat: number;
  status: string;
}

interface MyBooking {
  uuid: string;
  status: string;
  seatsBooked: number;
  trip: { uuid: string; departureCity: string; arrivalCity: string; departureTime: string };
}

const TABS: { key: TabKey; label: string; icon: React.ReactElement }[] = [
  { key: "overview", label: "Vue d'ensemble", icon: <FiGrid /> },
  { key: "requests", label: "Demandes reçues", icon: <FaInbox /> },
  { key: "vehicles", label: "Mes véhicules", icon: <FiTruck /> },
];

export default function DashboardPage() {
  const { user, isLoading, login, logout } = useAuth();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  // CreateTripPage redirige vers /dashboard?tab=vehicles : le paramètre doit
  // ouvrir directement le bon onglet.
  const tabFromUrl = searchParams.get("tab") as TabKey | null;
  const [activeTab, setActiveTab] = useState<TabKey>(
    tabFromUrl && TABS.some((t) => t.key === tabFromUrl) ? tabFromUrl : "overview",
  );

  const [myTrips, setMyTrips] = useState<MyTrip[]>([]);
  const [myBookings, setMyBookings] = useState<MyBooking[]>([]);
  const [pendingCount, setPending] = useState(0);
  const [loadingData, setLoadingData] = useState(true);

  const [editOpen, setEditOpen] = useState(false);
  const [form, setForm] = useState({ firstname: "", lastname: "", email: "", phoneNumber: "" });
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [modalError, setModalError] = useState<string | null>(null);

  const headers = () => ({
    Authorization: `Bearer ${localStorage.getItem("coshift_token") ?? ""}`,
  });

  useEffect(() => {
    if (!user) return;
    Promise.allSettled([
      axios.get(`${API_BASE}/api/trips/mine`, { headers: headers() }),
      axios.get(`${API_BASE}/api/bookings/mine`, { headers: headers() }),
      axios.get(`${API_BASE}/api/bookings/received`, { headers: headers() }),
    ]).then(([trips, bookings, received]) => {
      if (trips.status === "fulfilled") setMyTrips(trips.value.data);
      if (bookings.status === "fulfilled") setMyBookings(bookings.value.data);
      if (received.status === "fulfilled") {
        setPending(received.value.data.filter((b: MyBooking) => b.status === "PENDING").length);
      }
      setLoadingData(false);
    });
  }, [user]);

  const selectTab = (tab: TabKey) => {
    setActiveTab(tab);
    setSearchParams(tab === "overview" ? {} : { tab });
  };

  if (isLoading) {
    return (
      <div className="container page">
        <Spinner size="lg" center showLabel label="Chargement de votre espace" />
      </div>
    );
  }

  if (!user) return <Navigate to="/login" replace />;

  const openEdit = () => {
    setForm({
      firstname: user.firstname ?? "",
      lastname: user.lastname ?? "",
      email: user.email ?? "",
      phoneNumber: user.phoneNumber ?? "",
    });
    setModalError(null);
    setEditOpen(true);
  };

  const saveProfile = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setModalError(null);
    try {
      const res = await axios.put(`${API_BASE}/api/users/profile`, form, { headers: headers() });

      /* Changer d'adresse remet le compte en attente de vérification : le
         serveur ne renvoie alors aucun jeton et l'accès est coupé dès la
         requête suivante. Sans cette redirection, l'utilisateur restait sur un
         tableau de bord qui se mettait à répondre 401 sans rien expliquer. */
      if (res.data?.emailVerified === false) {
        logout();
        navigate(`/verify-email?email=${encodeURIComponent(form.email)}`);
        return;
      }

      if (res.data?.token) login(res.data.token);
      setEditOpen(false);
    } catch (err) {
      setModalError(
        (axios.isAxiosError(err) && err.response?.data?.message) ||
          "Une erreur est survenue lors de l'enregistrement.",
      );
    } finally {
      setSaving(false);
    }
  };

  const uploadPhoto = async (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!["image/jpeg", "image/png"].includes(file.type)) {
      setModalError("Seuls les formats JPG et PNG sont acceptés.");
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      setModalError("La photo ne doit pas dépasser 2 Mo.");
      return;
    }
    setUploading(true);
    setModalError(null);
    try {
      const data = new FormData();
      data.append("file", file);
      const res = await axios.post(`${API_BASE}/api/users/photo`, data, {
        headers: { ...headers(), "Content-Type": "multipart/form-data" },
      });
      if (res.data?.pictureUrl) login(localStorage.getItem("coshift_token") ?? "");
    } catch (err) {
      setModalError(
        (axios.isAxiosError(err) && err.response?.data?.message) ||
          "Impossible d'envoyer l'image.",
      );
    } finally {
      setUploading(false);
    }
  };

  const fullName = `${user.firstname} ${user.lastname}`;

  /* Places effectivement partagées : seules les réservations confirmées ou
     terminées comptent. Une demande en attente ne partage encore rien. */
  const sharedSeats = myBookings
    .filter((b) => b.status === "CONFIRMED" || b.status === "COMPLETED")
    .reduce((sum, b) => sum + b.seatsBooked, 0);

  return (
    <div className="container page stack-6">
      {/* ── Carte de profil ── */}
      <Card padding="lg">
        <div className="db__profile">
          <Avatar src={user.pictureUrl} name={fullName} size="xl" verified={user.emailVerified} />

          <div className="db__identity">
            <h1 className="db__name">{fullName}</h1>

            <p className="db__contact">
              {user.email}
              {user.phoneNumber && <> · {user.phoneNumber}</>}
            </p>

            <div className="db__badges">
              <span className="db__badge">
                {user.role === "USER" ? "Membre CoShift" : "Administrateur"}
              </span>
              {!user.emailVerified && (
                <span className="db__badge db__badge--warn">E-mail non vérifié</span>
              )}
            </div>

            <div className="db__stats">
              <span className="db__stat">
                <FaCar aria-hidden="true" />
                <strong>{user.tripsCount}</strong> trajet{user.tripsCount !== 1 ? "s" : ""}
              </span>
              <span className="db__stat">
                <FaStar aria-hidden="true" className="db__star" />
                <strong>{user.averageRating > 0 ? user.averageRating.toFixed(1) : "—"}</strong>
                {user.averageRating > 0 && " / 5"}
              </span>
            </div>
          </div>

          <Button variant="secondary" icon={<FiEdit3 />} onClick={openEdit}>
            Modifier le profil
          </Button>
        </div>
      </Card>

      {!user.emailVerified && (
        <Alert tone="warning" title="Votre adresse n'est pas vérifiée">
          Vous ne pourrez pas réserver de trajet tant que votre e-mail n'est pas confirmé.
        </Alert>
      )}

      {/* ── Onglets ── */}
      <div className="db__tabs" role="tablist" aria-label="Sections du tableau de bord">
        {TABS.map((t) => (
          <button
            key={t.key}
            role="tab"
            aria-selected={activeTab === t.key}
            className={`db__tab ${activeTab === t.key ? "is-active" : ""}`}
            onClick={() => selectTab(t.key)}
          >
            <span aria-hidden="true">{t.icon}</span>
            {t.label}
            {t.key === "requests" && pendingCount > 0 && (
              <span className="db__tab-count">
                {pendingCount}
                <span className="sr-only"> demandes en attente</span>
              </span>
            )}
          </button>
        ))}
      </div>

      {activeTab === "vehicles" ? (
        <VehiclePage />
      ) : activeTab === "requests" ? (
        <ReceivedBookingsPage />
      ) : loadingData ? (
        <Spinner size="lg" center showLabel label="Chargement de vos données" />
      ) : (
        <div className="db__grid">
          <Card
            title={<><FaCar aria-hidden="true" /> Mes trajets proposés</>}
            action={
              myTrips.length > 0 && (
                <Button variant="ghost" size="sm" to="/trips/create">Nouveau</Button>
              )
            }
          >
            {myTrips.length === 0 ? (
              <div className="db__empty">
                <p>Vous n'avez pas encore proposé de trajet à vos collègues.</p>
                <Button size="sm" to="/trips/create">Proposer un trajet</Button>
              </div>
            ) : (
              <ul className="db__list">
                {myTrips.slice(0, 4).map((t) => (
                  <li key={t.uuid}>
                    <a className="db__row" href={`/trips/${t.uuid}`}>
                      <span className="db__row-main">
                        <span className="db__row-title">
                          {t.departureCity} → {t.arrivalCity}
                        </span>
                        <span className="db__row-meta">{formatTripDate(t.departureTime)}</span>
                      </span>
                      <span className="db__row-side">
                        {/* Statut de trajet, pas de réservation : les deux
                            vocabulaires étaient confondus jusqu'ici. */}
                        <StatusBadge status={t.status as Status} size="sm" />
                        <span className="db__row-seats">
                          <FaUsers aria-hidden="true" /> {t.availableSeats}
                        </span>
                        <FiArrowRight aria-hidden="true" />
                      </span>
                    </a>
                  </li>
                ))}
              </ul>
            )}
          </Card>

          <Card
            title={<><FaTicketAlt aria-hidden="true" /> Mes réservations</>}
            action={
              myBookings.length > 0 && (
                <Button variant="ghost" size="sm" to="/bookings">Tout voir</Button>
              )
            }
          >
            {myBookings.length === 0 ? (
              <div className="db__empty">
                <p>Vous n'avez aucune réservation en cours.</p>
                <Button variant="secondary" size="sm" to="/trips/search">Trouver un trajet</Button>
              </div>
            ) : (
              <ul className="db__list">
                {myBookings.slice(0, 4).map((b) => (
                  <li key={b.uuid}>
                    <a className="db__row" href="/bookings">
                      <span className="db__row-main">
                        <span className="db__row-title">
                          {b.trip.departureCity} → {b.trip.arrivalCity}
                        </span>
                        <span className="db__row-meta">{formatTripDate(b.trip.departureTime)}</span>
                      </span>
                      <span className="db__row-side">
                        <StatusBadge status={b.status as Status} size="sm" />
                        <FiArrowRight aria-hidden="true" />
                      </span>
                    </a>
                  </li>
                ))}
              </ul>
            )}
          </Card>

          <Card
            className="db__wide"
            title={<><FaLeaf aria-hidden="true" /> Mon activité de partage</>}
          >
            <div className="db__counters">
              <p className="db__counter">
                <strong>{myTrips.length}</strong>
                <span>trajet{myTrips.length !== 1 ? "s" : ""} publié{myTrips.length !== 1 ? "s" : ""}</span>
              </p>
              <p className="db__counter">
                <strong>{myBookings.length}</strong>
                <span>réservation{myBookings.length !== 1 ? "s" : ""}</span>
              </p>
              <p className="db__counter db__counter--eco">
                <strong>{sharedSeats}</strong>
                <span>place{sharedSeats !== 1 ? "s" : ""} effectivement partagée{sharedSeats !== 1 ? "s" : ""}</span>
              </p>
            </div>
            <p className="db__note">
              Le CO₂ évité n'est pas affiché : son calcul demande la distance de
              chaque trajet, que l'API ne fournit pas encore.
            </p>
          </Card>
        </div>
      )}

      {/* ── Modification du profil ── */}
      <Modal
        open={editOpen}
        onClose={() => setEditOpen(false)}
        title="Modifier mon profil"
        footer={
          <>
            <Button variant="ghost" onClick={() => setEditOpen(false)}>Annuler</Button>
            <Button type="submit" form="db-profile" loading={saving}>Enregistrer</Button>
          </>
        }
      >
        {modalError && <Alert tone="danger">{modalError}</Alert>}

        <div className="db__photo">
          <Avatar src={user.pictureUrl} name={fullName} size="lg" />
          <label className="db__photo-btn">
            {uploading ? <Spinner size="sm" /> : <FiCamera aria-hidden="true" />}
            {uploading ? "Envoi…" : "Changer la photo"}
            <input type="file" accept="image/jpeg,image/png" onChange={uploadPhoto}
                   disabled={uploading} />
          </label>
          <p className="db__photo-hint">JPG ou PNG, 2 Mo maximum.</p>
        </div>

        <form id="db-profile" onSubmit={saveProfile} className="stack">
          <div className="db__form-grid">
            <Input label="Prénom" required value={form.firstname}
                   onChange={(e) => setForm({ ...form, firstname: e.target.value })} />
            <Input label="Nom" required value={form.lastname}
                   onChange={(e) => setForm({ ...form, lastname: e.target.value })} />
          </div>
          <Input label="Adresse e-mail" type="email" required value={form.email}
                 hint="La changer demandera une nouvelle vérification."
                 onChange={(e) => setForm({ ...form, email: e.target.value })} />
          <Input label="Téléphone" type="tel" value={form.phoneNumber}
                 hint="Communiqué à vos passagers une fois la réservation confirmée."
                 onChange={(e) => setForm({ ...form, phoneNumber: e.target.value })} />
        </form>
      </Modal>
    </div>
  );
}
