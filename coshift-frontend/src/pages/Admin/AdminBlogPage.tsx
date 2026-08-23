import { useCallback, useEffect, useState, type FormEvent } from "react";
import { Link, Navigate } from "react-router-dom";
import axios from "axios";
import { FiArrowLeft, FiEdit3, FiPlus, FiTrash2 } from "react-icons/fi";
import { API_BASE } from "../../config/api";
import { cleRubrique, type Billet } from "../../config/blogApi";
import { Alert, Button, Card, Input, Modal, Select, Spinner, Textarea } from "../../components/ui";
import { useAuth } from "../../context/AuthContext";
import { useLang } from "../../context/LangContext";
import { LANGUES, type Langue } from "../../i18n";
import { useSeo } from "../../hooks/useSeo";
import "./AdminPage.css";

interface Traduction {
  locale: string;
  title: string;
  lead: string;
  body: string;
}

interface Formulaire {
  uuid: string | null;
  slug: string;
  category: Billet["category"];
  readingMinutes: number;
  publie: boolean;
  traductions: Traduction[];
}

const RUBRIQUES: Billet["category"][] = ["PRODUIT", "CONFIDENTIALITE", "OUVERTURE", "CONCEPTION"];

const VIDE: Formulaire = {
  uuid: null,
  slug: "",
  category: "CONCEPTION",
  readingMinutes: 3,
  publie: false,
  traductions: [{ locale: "fr", title: "", lead: "", body: "" }],
};

/**
 * Rédaction des billets du blog.
 *
 * <h2>Pourquoi cet écran existe</h2>
 *
 * <p>Les billets vivaient dans le code : publier un texte demandait une
 * modification de `config/blog.ts`, une traduction dans deux catalogues et un
 * redéploiement. Le commentaire de ce fichier annonçait la bascule ; elle a
 * lieu ici.</p>
 *
 * <h2>Ce que le formulaire refuse, et ce qu'il laisse faire</h2>
 *
 * <p>Il exige au moins une traduction, mais aucune en particulier : un billet
 * rédigé d'abord en anglais est un billet valable. Il fige l'adresse dès la
 * publication — elle est indexée et partagée, la changer casserait chaque lien
 * en circulation. Et il laisse repasser un billet publié en brouillon : c'est
 * le seul moyen de retirer un texte du site sans le détruire.</p>
 */
export default function AdminBlogPage() {
  const { langue, t } = useLang();
  const { user, isLoading } = useAuth();
  const balise = LANGUES[langue].balise;

  useSeo({
    titre: t("blogAdmin.titre"),
    description: t("blogAdmin.description"),
    chemin: "/administration/blog",
    horsIndex: true,
  });

  const [billets, setBillets] = useState<Billet[]>([]);
  const [chargement, setChargement] = useState(true);
  const [erreur, setErreur] = useState<string | null>(null);
  const [form, setForm] = useState<Formulaire | null>(null);
  const [envoi, setEnvoi] = useState(false);

  const estSuperAdmin = user?.role === "SUPER_ADMIN";
  const entetes = useCallback(
    () => ({ Authorization: `Bearer ${localStorage.getItem("coshift_token") ?? ""}` }),
    [],
  );

  const charger = useCallback(
    () =>
      axios
        .get<Billet[]>(`${API_BASE}/api/blog/administration`, { headers: entetes() })
        .then((r) => setBillets(r.data)),
    [entetes],
  );

  useEffect(() => {
    if (!estSuperAdmin) {
      setChargement(false);
      return;
    }
    charger()
      .catch(() => setErreur(t("commun.erreurReseau")))
      .finally(() => setChargement(false));
  }, [estSuperAdmin, charger]);

  /* L'édition recharge le billet dans chaque langue disponible : la liste ne
     porte qu'une traduction, celle servie à l'affichage. Modifier depuis cette
     seule version effacerait les autres, puisque le serveur remplace au lieu
     de fusionner. */
  const ouvrirModification = async (b: Billet) => {
    setErreur(null);
    try {
      const versions = await Promise.all(
        b.languesDisponibles.map((loc) =>
          axios
            .get<Billet>(`${API_BASE}/api/blog/${b.slug}`, {
              headers: { ...entetes(), "Accept-Language": loc },
            })
            .then((r) => ({
              locale: loc,
              title: r.data.title ?? "",
              lead: r.data.lead ?? "",
              body: r.data.paragraphes.join("\n\n"),
            })),
        ),
      );
      setForm({
        uuid: b.uuid,
        slug: b.slug,
        category: b.category,
        readingMinutes: b.readingMinutes,
        publie: b.publishedAt !== null,
        traductions: versions,
      });
    } catch {
      setErreur(t("commun.erreurReseau"));
    }
  };

  const enregistrer = async (e: FormEvent) => {
    e.preventDefault();
    if (!form) return;
    setEnvoi(true);
    setErreur(null);
    try {
      const corps = {
        slug: form.slug,
        category: form.category,
        readingMinutes: form.readingMinutes,
        publie: form.publie,
        traductions: form.traductions,
      };
      if (form.uuid) {
        await axios.put(`${API_BASE}/api/blog/${form.uuid}`, corps, { headers: entetes() });
      } else {
        await axios.post(`${API_BASE}/api/blog`, corps, { headers: entetes() });
      }
      setForm(null);
      await charger();
    } catch (err) {
      setErreur(
        (axios.isAxiosError(err) && err.response?.data?.message) || t("commun.erreurGenerique"),
      );
    } finally {
      setEnvoi(false);
    }
  };

  const supprimer = async (b: Billet) => {
    if (!window.confirm(t("blogAdmin.confirmerSuppression", { titre: b.title ?? b.slug }))) return;
    setErreur(null);
    try {
      await axios.delete(`${API_BASE}/api/blog/${b.uuid}`, { headers: entetes() });
      await charger();
    } catch (err) {
      setErreur(
        (axios.isAxiosError(err) && err.response?.data?.message) || t("commun.erreurGenerique"),
      );
    }
  };

  const majTraduction = (i: number, champ: keyof Traduction, valeur: string) =>
    setForm((f) =>
      f
        ? { ...f, traductions: f.traductions.map((tr, k) => (k === i ? { ...tr, [champ]: valeur } : tr)) }
        : f,
    );

  const languesLibres = (i: number) =>
    (Object.keys(LANGUES) as Langue[]).filter(
      (l) => !form?.traductions.some((tr, k) => k !== i && tr.locale === l),
    );

  if (isLoading) return <Spinner center label={t("commun.chargementEnCours")} />;
  if (!user) return <Navigate to="/login" replace />;

  if (!estSuperAdmin) {
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
        <Link className="blog__retour" to="/administration">
          <FiArrowLeft aria-hidden="true" />
          {t("admin.lien")}
        </Link>
        <h1>{t("blogAdmin.heroTitre")}</h1>
        <p className="ad__portee">{t("blogAdmin.heroAccroche")}</p>
      </header>

      {erreur && <Alert tone="danger" onDismiss={() => setErreur(null)}>{erreur}</Alert>}

      <Card
        title={t("blogAdmin.titre")}
        action={
          <Button size="sm" icon={<FiPlus />} onClick={() => setForm({ ...VIDE })}>
            {t("blogAdmin.nouveau")}
          </Button>
        }
      >
        {billets.length === 0 ? (
          <p className="ad__vide">{t("blogAdmin.aucun")}</p>
        ) : (
          <div className="ad__table-wrap">
            <table className="ad__table">
              <thead>
                <tr>
                  <th scope="col">{t("blogAdmin.colonneTitre")}</th>
                  <th scope="col">{t("blogAdmin.colonneRubrique")}</th>
                  <th scope="col">{t("blogAdmin.colonneEtat")}</th>
                  <th scope="col">{t("blogAdmin.colonneLangues")}</th>
                  <th scope="col">{t("blogAdmin.colonneDate")}</th>
                  <th scope="col">{t("blogAdmin.colonneAction")}</th>
                </tr>
              </thead>
              <tbody>
                {billets.map((b) => (
                  <tr key={b.uuid}>
                    <th scope="row">
                      <span className="ad__nom">{b.title}</span>
                      <span className="ad__email">/blog/{b.slug}</span>
                    </th>
                    <td>{t(`blog.rubrique.${cleRubrique(b.category)}`)}</td>
                    <td>
                      <span
                        className={`ad__etat ${b.publishedAt ? "ad__etat--actif" : "ad__etat--attente"}`}
                      >
                        {b.publishedAt ? t("blogAdmin.etatPublie") : t("blogAdmin.etatBrouillon")}
                      </span>
                    </td>
                    <td>{b.languesDisponibles.join(", ").toUpperCase()}</td>
                    <td>
                      {b.publishedAt
                        ? new Date(b.publishedAt).toLocaleDateString(balise, { dateStyle: "medium" })
                        : "—"}
                    </td>
                    <td className="ad__actions-cellule">
                      <Button size="sm" variant="secondary" icon={<FiEdit3 />}
                              onClick={() => void ouvrirModification(b)}>
                        {t("blogAdmin.modifier")}
                      </Button>
                      <Button size="sm" variant="ghost" icon={<FiTrash2 />}
                              onClick={() => void supprimer(b)}>
                        {t("blogAdmin.supprimer")}
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <Modal
        open={form !== null}
        onClose={() => setForm(null)}
        size="lg"
        title={form?.uuid ? t("blogAdmin.formTitreModification") : t("blogAdmin.formTitreCreation")}
        footer={
          <>
            <Button variant="ghost" onClick={() => setForm(null)}>{t("commun.annuler")}</Button>
            <Button loading={envoi} form="form-billet" type="submit">
              {t("blogAdmin.enregistrer")}
            </Button>
          </>
        }
      >
        {form && (
          <form id="form-billet" onSubmit={enregistrer} className="stack-6">
            <Input
              label={t("blogAdmin.slug")}
              hint={t("blogAdmin.slugAide")}
              required
              value={form.slug}
              onChange={(e) => setForm({ ...form, slug: e.target.value })}
            />

            <Select
              label={t("blogAdmin.rubrique")}
              value={form.category}
              onChange={(e) => setForm({ ...form, category: e.target.value as Billet["category"] })}
              options={RUBRIQUES.map((r) => ({
                value: r,
                label: t(`blog.rubrique.${cleRubrique(r)}`),
              }))}
            />

            <Input
              label={t("blogAdmin.lecture")}
              type="number"
              min={1}
              max={60}
              value={form.readingMinutes}
              onChange={(e) => setForm({ ...form, readingMinutes: parseInt(e.target.value) || 1 })}
            />

            <label className="ad__case">
              <input
                type="checkbox"
                checked={form.publie}
                onChange={(e) => setForm({ ...form, publie: e.target.checked })}
              />
              <span>
                {t("blogAdmin.publier")}
                <span className="ad__case-aide">{t("blogAdmin.publierAide")}</span>
              </span>
            </label>

            {form.traductions.map((tr, i) => (
              <fieldset className="ad__traduction" key={i}>
                <legend>
                  {t("blogAdmin.langue")} — {LANGUES[tr.locale as Langue]?.nom ?? tr.locale}
                </legend>

                <Select
                  label={t("blogAdmin.langue")}
                  value={tr.locale}
                  onChange={(e) => majTraduction(i, "locale", e.target.value)}
                  options={languesLibres(i).map((l) => ({ value: l, label: LANGUES[l].nom }))}
                />
                <Input
                  label={t("blogAdmin.titreBillet")}
                  required
                  maxLength={200}
                  value={tr.title}
                  onChange={(e) => majTraduction(i, "title", e.target.value)}
                />
                <Textarea
                  label={t("blogAdmin.chapeau")}
                  hint={t("blogAdmin.chapeauAide")}
                  required
                  maxLength={500}
                  showCount
                  value={tr.lead}
                  onChange={(e) => majTraduction(i, "lead", e.target.value)}
                />
                <Textarea
                  label={t("blogAdmin.corps")}
                  hint={t("blogAdmin.corpsAide")}
                  required
                  rows={12}
                  value={tr.body}
                  onChange={(e) => majTraduction(i, "body", e.target.value)}
                />

                {form.traductions.length > 1 && (
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() =>
                      setForm({
                        ...form,
                        traductions: form.traductions.filter((_, k) => k !== i),
                      })
                    }
                  >
                    {t("blogAdmin.retirerLangue")}
                  </Button>
                )}
              </fieldset>
            ))}

            {form.traductions.length < Object.keys(LANGUES).length && (
              <Button
                variant="secondary"
                size="sm"
                icon={<FiPlus />}
                onClick={() =>
                  setForm({
                    ...form,
                    traductions: [
                      ...form.traductions,
                      { locale: languesLibres(-1)[0], title: "", lead: "", body: "" },
                    ],
                  })
                }
              >
                {t("blogAdmin.ajouterLangue")}
              </Button>
            )}
          </form>
        )}
      </Modal>
    </div>
  );
}
