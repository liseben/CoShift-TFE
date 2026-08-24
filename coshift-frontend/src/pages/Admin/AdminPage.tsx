import { useCallback, useEffect, useState, type FormEvent } from "react";
import { Link, Navigate } from "react-router-dom";
import axios from "axios";
import { FiSearch, FiShield, FiSlash, FiRotateCcw } from "react-icons/fi";
import { API_BASE } from "../../config/api";
import { Alert, Button, Card, Input, Modal, Spinner, Textarea , Select} from "../../components/ui";
import { useAuth } from "../../context/AuthContext";
import { useLang } from "../../context/LangContext";
import { LANGUES } from "../../i18n";
import { useSeo } from "../../hooks/useSeo";
import "./AdminPage.css";

interface Apercu {
  portee: "PLATEFORME" | "ORGANISATIONS";
  organisations: string[];
  membres: { total: number; verifies: number; suspendus: number; effaces: number };
  trajets: { aVenir: number; realises: number; annules: number; sansOrganisation: number };
  reservations: { enAttente: number; confirmees: number; honorees: number; annulees: number };
}

interface Membre {
  uuid: string;
  firstname: string;
  lastname: string;
  email: string;
  role: string;
  emailVerified: boolean;
  organisations: string[];
  tripsCount: number;
  averageRating: number;
  createdAt: string;
  suspendedAt: string | null;
  suspensionReason: string | null;
}

interface PageMembres {
  content: Membre[];
  number: number;
  totalPages: number;
  totalElements: number;
}

interface Blocage {
  adresseIp: string;
  compte: string;
  echecs: number;
  jusqua: string;
}

/**
 * Console de supervision et de modération.
 *
 * <h2>Ce que cet écran montre, et à qui</h2>
 *
 * <p>Un {@code SUPER_ADMIN} répond de la plateforme et voit tout. Un
 * {@code ADMIN} répond de ses organisations et ne voit qu'elles — la borne est
 * posée par le serveur, jamais ici : une restriction écrite dans l'interface
 * s'enlève avec les outils de développement du navigateur. L'écran se contente
 * d'<em>annoncer</em> sa portée, pour qu'un chiffre borné ne soit pas lu comme
 * un chiffre global.</p>
 *
 * <h2>Ce qu'il ne montre pas</h2>
 *
 * <p>Ni téléphone, ni adresse postale, ni photographie, ni le journal de
 * sécurité. Une console d'administration est le point le plus intéressant à
 * compromettre d'une application : chaque colonne affichée est une ligne de
 * l'annuaire qu'elle deviendrait le jour où elle tombe.</p>
 */
export default function AdminPage() {
  const { langue, t } = useLang();
  const { user, isLoading } = useAuth();
  const balise = LANGUES[langue].balise;

  useSeo({
    titre: t("admin.titre"),
    description: t("admin.description"),
    chemin: "/administration",
    horsIndex: true,
  });

  const [apercu, setApercu] = useState<Apercu | null>(null);
  const [membres, setMembres] = useState<PageMembres | null>(null);
  const [blocages, setBlocages] = useState<Blocage[]>([]);
  const [recherche, setRecherche] = useState("");
  const [page, setPage] = useState(0);
  const [chargement, setChargement] = useState(true);
  const [erreur, setErreur] = useState<string | null>(null);

  /** Compte visé par la modale de suspension, ou null. */
  const [aSuspendre, setASuspendre] = useState<Membre | null>(null);
  /* Membre dont on change le role, et le role vise. */
  const [aPromouvoir, setAPromouvoir] = useState<Membre | null>(null);
  const [roleVise, setRoleVise] = useState("USER");
  const [motif, setMotif] = useState("");
  const [envoi, setEnvoi] = useState(false);

  const estSuperAdmin = user?.role === "SUPER_ADMIN";
  const entetes = useCallback(
    () => ({ Authorization: `Bearer ${localStorage.getItem("coshift_token") ?? ""}` }),
    [],
  );

  const chargerMembres = useCallback(
    (q: string, p: number) =>
      axios
        .get<PageMembres>(`${API_BASE}/api/admin/membres`, {
          headers: entetes(),
          params: { q: q || undefined, page: p, taille: 20 },
        })
        .then((r) => setMembres(r.data)),
    [entetes],
  );

  useEffect(() => {
    if (!user) return;
    Promise.all([
      axios.get<Apercu>(`${API_BASE}/api/admin/apercu`, { headers: entetes() }).then((r) => setApercu(r.data)),
      chargerMembres("", 0),
      /* Les freinages ne concernent que la plateforme : les demander en tant
         qu'ADMIN produirait un 403 attendu, donc un bruit d'erreur inutile. */
      estSuperAdmin
        ? axios.get<Blocage[]>(`${API_BASE}/api/admin/blocages`, { headers: entetes() })
            .then((r) => setBlocages(r.data))
        : Promise.resolve(),
    ])
      .catch(() => setErreur(t("commun.erreurReseau")))
      .finally(() => setChargement(false));
  }, [user, estSuperAdmin, entetes, chargerMembres]);

  const chercher = (e: FormEvent) => {
    e.preventDefault();
    setPage(0);
    chargerMembres(recherche, 0).catch(() => setErreur(t("commun.erreurReseau")));
  };

  const allerPage = (p: number) => {
    setPage(p);
    chargerMembres(recherche, p).catch(() => setErreur(t("commun.erreurReseau")));
  };

  const suspendre = async () => {
    if (!aSuspendre) return;
    setEnvoi(true);
    setErreur(null);
    try {
      await axios.patch(
        `${API_BASE}/api/admin/membres/${aSuspendre.uuid}/suspension`,
        { motif },
        { headers: entetes() },
      );
      setASuspendre(null);
      setMotif("");
      await chargerMembres(recherche, page);
    } catch (err) {
      setErreur(
        (axios.isAxiosError(err) && err.response?.data?.message) || t("commun.erreurGenerique"),
      );
    } finally {
      setEnvoi(false);
    }
  };

  const changerRole = async () => {
    if (!aPromouvoir) return;
    setEnvoi(true);
    setErreur(null);
    try {
      await axios.patch(
        `${API_BASE}/api/admin/membres/${aPromouvoir.uuid}/role`,
        { role: roleVise },
        { headers: entetes() },
      );
      setAPromouvoir(null);
      await chargerMembres(recherche, page);
    } catch (err) {
      setErreur(
        (axios.isAxiosError(err) && err.response?.data?.message) || t("commun.erreurGenerique"),
      );
    } finally {
      setEnvoi(false);
    }
  };

  const reactiver = async (m: Membre) => {
    setErreur(null);
    try {
      await axios.delete(`${API_BASE}/api/admin/membres/${m.uuid}/suspension`, { headers: entetes() });
      await chargerMembres(recherche, page);
    } catch (err) {
      setErreur(
        (axios.isAxiosError(err) && err.response?.data?.message) || t("commun.erreurGenerique"),
      );
    }
  };

  const date = (iso: string) => new Date(iso).toLocaleDateString(balise, { dateStyle: "medium" });
  const dateHeure = (iso: string) => new Date(iso).toLocaleString(balise, { timeStyle: "short", dateStyle: "short" });

  if (isLoading) return <Spinner center label={t("commun.chargementEnCours")} />;
  if (!user) return <Navigate to="/login" replace />;

  /* Garde d'affichage, doublant celle du serveur. Elle n'ajoute aucune
     sécurité — un @PreAuthorize refuse déjà chaque appel — mais elle évite de
     présenter à un membre ordinaire un écran vide et quatre erreurs 403. */
  if (user.role !== "ADMIN" && user.role !== "SUPER_ADMIN") {
    return (
      <div className="container page stack-6">
        <Alert tone="warning">{t("admin.refuse")}</Alert>
      </div>
    );
  }

  if (chargement) return <Spinner center label={t("commun.chargementEnCours")} />;

  return (
    <div className="container container--wide page stack-8">
      <header className="ad__header">
        <h1>
          <FiShield aria-hidden="true" /> {t("admin.heroTitre")}
        </h1>
        {estSuperAdmin && (
          /* La redaction du blog est la voix editoriale de la plateforme :
             elle suit le meme role que la supervision de plateforme. */
          <p className="ad__liens">
            <Link to="/administration/blog">{t("blogAdmin.lien")}</Link>
          </p>
        )}
        {apercu && (
          <p className="ad__portee">
            {apercu.portee === "PLATEFORME"
              ? t("admin.porteePlateforme")
              : t("admin.porteeOrganisations", { noms: apercu.organisations.join(", ") })}
          </p>
        )}
      </header>

      {erreur && <Alert tone="danger" onDismiss={() => setErreur(null)}>{erreur}</Alert>}

      {apercu && (
        <div className="ad__blocs">
          <Card title={t("admin.membres")}>
            <ul className="ad__chiffres">
              <Chiffre v={apercu.membres.total} l={t("admin.membresTotal")} />
              <Chiffre v={apercu.membres.verifies} l={t("admin.membresVerifies")} />
              <Chiffre v={apercu.membres.suspendus} l={t("admin.membresSuspendus")} alerte={apercu.membres.suspendus > 0} />
              {apercu.portee === "PLATEFORME" && (
                <Chiffre v={apercu.membres.effaces} l={t("admin.membresEfface")} />
              )}
            </ul>
          </Card>

          <Card title={t("admin.trajets")}>
            <ul className="ad__chiffres">
              <Chiffre v={apercu.trajets.aVenir} l={t("admin.trajetsAVenir")} />
              <Chiffre v={apercu.trajets.realises} l={t("admin.trajetsRealises")} />
              <Chiffre v={apercu.trajets.annules} l={t("admin.trajetsAnnules")} />
              {apercu.portee === "PLATEFORME" && (
                <Chiffre v={apercu.trajets.sansOrganisation} l={t("admin.trajetsSansOrganisation")} />
              )}
            </ul>
          </Card>

          <Card title={t("admin.reservations")}>
            <ul className="ad__chiffres">
              <Chiffre v={apercu.reservations.enAttente} l={t("admin.reservationsEnAttente")} />
              <Chiffre v={apercu.reservations.confirmees} l={t("admin.reservationsConfirmees")} />
              <Chiffre v={apercu.reservations.honorees} l={t("admin.reservationsHonorees")} />
              <Chiffre v={apercu.reservations.annulees} l={t("admin.reservationsAnnulees")} />
            </ul>
          </Card>
        </div>
      )}

      <Card title={t("admin.listeTitre")}>
        <form className="ad__recherche" onSubmit={chercher} role="search">
          <Input
            label={t("admin.rechercher")}
            hint={t("admin.rechercherAide")}
            value={recherche}
            onChange={(e) => setRecherche(e.target.value)}
          />
          <Button type="submit" icon={<FiSearch />}>{t("commun.rechercher")}</Button>
        </form>

        {membres && membres.content.length === 0 && (
          <p className="ad__vide">{t("admin.aucunMembre")}</p>
        )}

        {membres && membres.content.length > 0 && (
          <div className="ad__table-wrap">
            <table className="ad__table">
              <thead>
                <tr>
                  <th scope="col">{t("admin.colonneMembre")}</th>
                  <th scope="col">{t("admin.colonneOrganisations")}</th>
                  <th scope="col">{t("admin.colonneRole")}</th>
                  <th scope="col">{t("admin.colonneEtat")}</th>
                  <th scope="col">{t("admin.colonneInscrit")}</th>
                  {estSuperAdmin && <th scope="col">{t("admin.colonneAction")}</th>}
                </tr>
              </thead>
              <tbody>
                {membres.content.map((m) => (
                  <tr key={m.uuid} className={m.suspendedAt ? "is-suspendu" : ""}>
                    <th scope="row">
                      <span className="ad__nom">{m.firstname} {m.lastname}</span>
                      <span className="ad__email">{m.email}</span>
                    </th>
                    <td>{m.organisations.join(", ") || "—"}</td>
                    <td>{t(`admin.role${m.role}`)}</td>
                    <td>
                      {m.suspendedAt ? (
                        <>
                          <span className="ad__etat ad__etat--suspendu">{t("admin.etatSuspendu")}</span>
                          <span className="ad__detail">
                            {t("admin.depuisLe", { date: date(m.suspendedAt) })}
                          </span>
                          {m.suspensionReason && (
                            <span className="ad__detail">
                              {t("admin.suspenduPour", { motif: m.suspensionReason })}
                            </span>
                          )}
                        </>
                      ) : m.emailVerified ? (
                        <span className="ad__etat ad__etat--actif">{t("admin.etatActif")}</span>
                      ) : (
                        <span className="ad__etat ad__etat--attente">{t("admin.etatNonVerifie")}</span>
                      )}
                    </td>
                    <td>{date(m.createdAt)}</td>
                    {estSuperAdmin && (
                      <td>
                        {/* Le role se change meme sur un compte suspendu : ce sont
                            deux mesures independantes. */}
                        <Button size="sm" variant="ghost" icon={<FiShield />}
                                onClick={() => { setAPromouvoir(m); setRoleVise(m.role); }}>
                          {t("admin.colonneRole")}
                        </Button>
                        {m.role === "SUPER_ADMIN" ? (
                          "—"
                        ) : m.suspendedAt ? (
                          <Button size="sm" variant="secondary" icon={<FiRotateCcw />}
                                  onClick={() => void reactiver(m)}>
                            {t("admin.reactiver")}
                          </Button>
                        ) : (
                          <Button size="sm" variant="danger" icon={<FiSlash />}
                                  onClick={() => { setASuspendre(m); setMotif(""); }}>
                            {t("admin.suspendre")}
                          </Button>
                        )}
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {membres && membres.totalPages > 1 && (
          <nav className="ad__pagination" aria-label={t("admin.listeTitre")}>
            <Button size="sm" variant="ghost" disabled={page === 0} onClick={() => allerPage(page - 1)}>
              {t("admin.precedente")}
            </Button>
            <span>{t("admin.page", { n: membres.number + 1, total: membres.totalPages })}</span>
            <Button size="sm" variant="ghost" disabled={page >= membres.totalPages - 1}
                    onClick={() => allerPage(page + 1)}>
              {t("admin.suivante")}
            </Button>
          </nav>
        )}
      </Card>

      {estSuperAdmin && (
        <Card title={t("admin.blocages")}>
          <p className="ad__note">{t("admin.blocagesTexte")}</p>
          {blocages.length === 0 ? (
            <p className="ad__vide">{t("admin.aucunBlocage")}</p>
          ) : (
            <div className="ad__table-wrap">
              <table className="ad__table">
                <thead>
                  <tr>
                    <th scope="col">{t("admin.colonneIp")}</th>
                    <th scope="col">{t("admin.colonneCompte")}</th>
                    <th scope="col">{t("admin.colonneEchecs")}</th>
                    <th scope="col">{t("admin.colonneJusqua")}</th>
                  </tr>
                </thead>
                <tbody>
                  {blocages.map((b) => (
                    <tr key={`${b.adresseIp}|${b.compte}`}>
                      <th scope="row">{b.adresseIp}</th>
                      <td>{b.compte || "—"}</td>
                      <td>{b.echecs}</td>
                      <td>{dateHeure(b.jusqua)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      )}

      <Modal
        open={aPromouvoir !== null}
        onClose={() => setAPromouvoir(null)}
        title={t("admin.changerRole", {
          nom: aPromouvoir ? `${aPromouvoir.firstname} ${aPromouvoir.lastname}` : "",
        })}
        size="sm"
        footer={
          <>
            <Button variant="ghost" onClick={() => setAPromouvoir(null)}>{t("commun.annuler")}</Button>
            <Button loading={envoi} disabled={roleVise === aPromouvoir?.role}
                    onClick={() => void changerRole()}>
              {t("admin.confirmerRole")}
            </Button>
          </>
        }
      >
        <p className="ad__note">{t("admin.changerRoleAide")}</p>
        <Select
          label={t("admin.colonneRole")}
          value={roleVise}
          onChange={(e) => setRoleVise(e.target.value)}
          options={["USER", "ADMIN", "SUPER_ADMIN"].map((r) => ({
            value: r,
            label: t(`admin.role${r}`),
          }))}
        />
      </Modal>

      <Modal
        open={aSuspendre !== null}
        onClose={() => setASuspendre(null)}
        title={t("admin.suspendreTitre", {
          nom: aSuspendre ? `${aSuspendre.firstname} ${aSuspendre.lastname}` : "",
        })}
        footer={
          <>
            <Button variant="ghost" onClick={() => setASuspendre(null)}>{t("commun.annuler")}</Button>
            <Button variant="danger" loading={envoi} disabled={!motif.trim()}
                    onClick={() => void suspendre()}>
              {t("admin.confirmerSuspension")}
            </Button>
          </>
        }
      >
        <p className="ad__note">{t("admin.suspendreTexte")}</p>
        <Textarea
          label={t("admin.motif")}
          hint={t("admin.motifAide")}
          required
          maxLength={255}
          showCount
          value={motif}
          onChange={(e) => setMotif(e.target.value)}
        />
      </Modal>
    </div>
  );
}

/** Un chiffre et ce qu'il compte. Le libellé n'est jamais séparé de sa valeur. */
function Chiffre({ v, l, alerte = false }: { v: number; l: string; alerte?: boolean }) {
  return (
    <li className={`ad__chiffre ${alerte ? "ad__chiffre--alerte" : ""}`}>
      <span className="ad__valeur">{v}</span>
      <span className="ad__libelle">{l}</span>
    </li>
  );
}
