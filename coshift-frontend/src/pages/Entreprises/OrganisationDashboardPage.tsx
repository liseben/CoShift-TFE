import { useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import axios from "axios";
import { FiInfo } from "react-icons/fi";
import { API_BASE } from "../../config/api";
import { Alert, Card, EmptyState, Spinner } from "../../components/ui";
import { useAuth } from "../../context/AuthContext";
import { useLang } from "../../context/LangContext";
import { LANGUES } from "../../i18n";
import { useSeo } from "../../hooks/useSeo";
import "./OrganisationDashboardPage.css";

interface TableauDeBord {
  uuid: string;
  name: string;
  slug: string;
  logoUrl?: string | null;
  volumes: {
    trajetsPublies: number;
    trajetsAnnules: number;
    trajetsRealises: number;
    placesPartagees: number;
    placesRestantes: number;
    tauxRemplissage: number;
  };
  participation: { membres: number; conducteurs: number; passagers: number };
  parMois: { mois: string; trajets: number; placesPartagees: number }[];
  nonMesure: { distanceParcourue: boolean; emissionsEvitees: boolean; motif: string };
}

/** Libellé du mois « 2026-05 » dans la langue courante. */
function libelleMois(mois: string, balise: string): string {
  const [annee, m] = mois.split("-").map(Number);
  return new Date(annee, m - 1, 1).toLocaleDateString(balise, {
    month: "long",
    year: "numeric",
  });
}

/**
 * Chiffres de covoiturage des organisations dont on est membre.
 *
 * <h2>Pourquoi il n'y a ni kilomètres ni CO₂</h2>
 *
 * <p>C'est ce qu'un tableau de bord d'employeur affiche d'ordinaire, et c'est
 * précisément ce que CoShift ne mesure pas : un trajet porte une ville de
 * départ et une ville d'arrivée, pas une distance. Rien n'empêcherait
 * d'afficher un ordre de grandeur — ce serait le chiffre le plus facile à
 * produire de cet écran, et le seul que personne ne songerait à vérifier. Le
 * bloc « ce que ces chiffres ne disent pas » l'annonce plutôt que de le
 * combler.</p>
 *
 * <h2>Pourquoi aucun seuil d'anonymat</h2>
 *
 * <p>Le jeu de données ouvert en applique un, parce qu'il s'adresse à un
 * lecteur anonyme. Ici le lecteur est membre du cercle qu'il consulte : les
 * trajets comptés sont ceux qu'il voit déjà un par un dans la recherche.
 * Masquer un agrégat dont le détail est à portée de clic serait une précaution
 * de façade.</p>
 */
export default function OrganisationDashboardPage() {
  const { langue, t } = useLang();
  const { user, isLoading } = useAuth();
  const balise = LANGUES[langue].balise;

  useSeo({
    titre: t("organisation.titre"),
    description: t("organisation.description"),
    chemin: "/entreprises/tableau-de-bord",
    horsIndex: true,
  });

  const [organisations, setOrganisations] = useState<TableauDeBord[]>([]);
  const [chargement, setChargement] = useState(true);
  const [erreur, setErreur] = useState<string | null>(null);

  useEffect(() => {
    if (!user) return;
    axios
      .get<TableauDeBord[]>(`${API_BASE}/api/organizations/mine`, {
        headers: { Authorization: `Bearer ${localStorage.getItem("coshift_token") ?? ""}` },
      })
      .then((r) => setOrganisations(r.data))
      .catch(() => setErreur(t("commun.erreurReseau")))
      .finally(() => setChargement(false));
  }, [user]);

  /* Ces chiffres sont ceux d'un cercle : ils n'ont de sens que pour quelqu'un
     qui en fait partie. On attend de savoir si une session existe avant de
     renvoyer vers la connexion, sans quoi un rechargement de page ejecterait
     une personne pourtant connectee. */
  if (isLoading) return <Spinner center label={t("commun.chargementEnCours")} />;
  if (!user) return <Navigate to="/login" replace />;
  if (chargement) return <Spinner center label={t("commun.chargementEnCours")} />;

  return (
    <div className="container page stack-8">
      <header className="od__header">
        <h1>{t("organisation.heroTitre")}</h1>
        <p className="od__lead">{t("organisation.heroAccroche")}</p>
      </header>

      {erreur && <Alert tone="danger">{erreur}</Alert>}

      {!erreur && organisations.length === 0 && (
        <EmptyState
          title={t("organisation.videTitre")}
          description={t("organisation.videTexte")}
        />
      )}

      {organisations.map((o) => (
        <section className="od__org stack-6" key={o.uuid} aria-labelledby={`od-${o.slug}`}>
          <h2 id={`od-${o.slug}`} className="od__org-nom">{o.name}</h2>

          <Card title={t("organisation.volumes")}>
            <ul className="od__chiffres">
              <Chiffre valeur={o.volumes.trajetsPublies} libelle={t("organisation.trajetsPublies")} />
              <Chiffre valeur={o.volumes.trajetsRealises} libelle={t("organisation.trajetsRealises")} />
              <Chiffre valeur={o.volumes.trajetsAnnules} libelle={t("organisation.trajetsAnnules")} />
              <Chiffre valeur={o.volumes.placesPartagees} libelle={t("organisation.placesPartagees")} />
              <Chiffre valeur={o.volumes.placesRestantes} libelle={t("organisation.placesRestantes")} />
              <Chiffre
                valeur={`${o.volumes.tauxRemplissage} %`}
                libelle={t("organisation.tauxRemplissage")}
                accent
              />
            </ul>
          </Card>

          <Card title={t("organisation.participation")}>
            <ul className="od__chiffres">
              <Chiffre valeur={o.participation.membres} libelle={t("organisation.membres")} />
              <Chiffre valeur={o.participation.conducteurs} libelle={t("organisation.conducteurs")} />
              <Chiffre valeur={o.participation.passagers} libelle={t("organisation.passagers")} />
            </ul>
          </Card>

          <Card title={t("organisation.parMois")}>
            {o.parMois.length === 0 ? (
              <p className="od__vide">{t("organisation.moisVide")}</p>
            ) : (
              /* Le tableau est la source, la barre n'en est que la lecture :
                 une barre seule n'est ni lisible au lecteur d'écran ni
                 copiable dans un tableur. */
              <div className="od__table-wrap">
                <table className="od__table">
                  <thead>
                    <tr>
                      <th scope="col">{t("organisation.colonneMois")}</th>
                      <th scope="col">{t("organisation.colonneTrajets")}</th>
                      <th scope="col">{t("organisation.colonnePlaces")}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {o.parMois.map((m) => {
                      const maximum = Math.max(...o.parMois.map((x) => x.trajets), 1);
                      return (
                        <tr key={m.mois}>
                          <th scope="row">{libelleMois(m.mois, balise)}</th>
                          <td>
                            <span className="od__barre" aria-hidden="true">
                              <span style={{ width: `${(m.trajets / maximum) * 100}%` }} />
                            </span>
                            {m.trajets}
                          </td>
                          <td>{m.placesPartagees}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </Card>

          <Card title={<><FiInfo aria-hidden="true" /> {t("organisation.nonMesureTitre")}</>}>
            <p className="od__non-mesure">{o.nonMesure.motif}</p>
          </Card>
        </section>
      ))}
    </div>
  );
}

/** Un chiffre et ce qu'il compte. Le libellé n'est jamais séparé de sa valeur. */
function Chiffre({
  valeur,
  libelle,
  accent = false,
}: {
  valeur: number | string;
  libelle: string;
  accent?: boolean;
}) {
  return (
    <li className={`od__chiffre ${accent ? "od__chiffre--accent" : ""}`}>
      <span className="od__valeur">{valeur}</span>
      <span className="od__libelle">{libelle}</span>
    </li>
  );
}
