import {
  FaBuilding, FaCarSide, FaChartLine, FaLeaf, FaParking, FaUserShield,
} from "react-icons/fa";
import { FiArrowRight, FiCheck } from "react-icons/fi";
import { Button, Card } from "../../components/ui";
import { PHOTOS } from "../../components/image_site";
import { useT } from "../../context/LangContext";
import { useSeo } from "../../hooks/useSeo";
import "./EntreprisesPage.css";

/* Comme ailleurs dans le projet, ces tableaux ne portent que des clés : ils
   sont évalués au chargement du module, avant que la langue soit connue. Une
   chaîne écrite ici resterait figée en français. */

const CHIFFRES = ["chiffre1", "chiffre2", "chiffre3"] as const;

const BENEFICES = [
  { icon: <FaParking />,    cle: "parking" },
  { icon: <FaLeaf />,       cle: "empreinte" },
  { icon: <FaUserShield />, cle: "cohesion" },
  { icon: <FaChartLine />,  cle: "mesure" },
] as const;

const ETAPES = ["1", "2", "3"] as const;

/**
 * Page de présentation à destination des organisations.
 *
 * <p>C'est la page qui porte le positionnement du projet. CoShift ne met pas
 * en relation des inconnus sur de longues distances : il s'adresse à des
 * personnes qui partagent déjà un lieu et des horaires. Tout le reste — le
 * cercle fermé, la confiance préexistante, la régularité des trajets — découle
 * de cette différence, et c'est ici qu'elle s'explique.</p>
 */
export default function EntreprisesPage() {
  const t = useT();

  useSeo({
    titre: t("entreprises.titre"),
    description: t("entreprises.description"),
    chemin: "/entreprises",
  });

  return (
    <div className="ent">
      {/* ── Ouverture ── */}
      <section className="ent__hero">
        <div className="container container--prose">
          <p className="ent__eyebrow">
            <FaBuilding aria-hidden="true" /> {t("entreprises.eyebrow")}
          </p>
          <h1 className="ent__title">{t("entreprises.heroTitre")}</h1>
          <p className="ent__lead">{t("entreprises.heroAccroche")}</p>
          <div className="ent__hero-actions">
            <Button to="/register" icon={<FiArrowRight />}>
              {t("entreprises.heroBouton")}
            </Button>
            <Button variant="secondary" to="/a-propos">
              {t("entreprises.heroSecondaire")}
            </Button>
          </div>
        </div>
      </section>

      <figure className="ent__figure">
        <img
          src={PHOTOS.depart.src}
          alt={t(PHOTOS.depart.alt)}
          loading="lazy"
          decoding="async"
        />
      </figure>

      {/* ── Le constat, chiffré ── */}
      <section className="container container--wide ent__section">
        <h2 className="ent__h2">{t("entreprises.constatTitre")}</h2>
        <p className="ent__intro">{t("entreprises.constatIntro")}</p>

        <div className="ent__chiffres">
          {CHIFFRES.map((cle) => (
            <div className="ent__chiffre" key={cle}>
              <span className="ent__chiffre-valeur">
                {t(`entreprises.${cle}Valeur`)}
              </span>
              <span className="ent__chiffre-libelle">
                {t(`entreprises.${cle}Libelle`)}
              </span>
            </div>
          ))}
        </div>

        <p className="ent__source">{t("entreprises.constatSource")}</p>
      </section>

      {/* ── Ce qui change avec un cercle fermé ── */}
      <section className="container container--prose ent__section">
        <h2 className="ent__h2">{t("entreprises.differenceTitre")}</h2>
        <p>{t("entreprises.differenceP1")}</p>
        <p>{t("entreprises.differenceP2")}</p>
        <p className="ent__highlight">{t("entreprises.differenceFort")}</p>
      </section>

      {/* ── Bénéfices ── */}
      <section className="container container--wide ent__section">
        <h2 className="ent__h2">{t("entreprises.beneficesTitre")}</h2>

        <div className="grid-auto">
          {BENEFICES.map(({ icon, cle }) => (
            <Card key={cle}>
              <div className="ent__benefice">
                <span className="ent__benefice-icon" aria-hidden="true">
                  {icon}
                </span>
                <div>
                  <h3 className="ent__benefice-titre">
                    {t(`entreprises.${cle}Titre`)}
                  </h3>
                  <p className="ent__benefice-texte">
                    {t(`entreprises.${cle}Texte`)}
                  </p>
                </div>
              </div>
            </Card>
          ))}
        </div>
      </section>

      {/* ── Mise en place ── */}
      <section className="container container--wide ent__section">
        <h2 className="ent__h2">{t("entreprises.etapesTitre")}</h2>
        <p className="ent__intro">{t("entreprises.etapesIntro")}</p>

        <ol className="ent__etapes">
          {ETAPES.map((n) => (
            <li className="ent__etape" key={n}>
              <span className="ent__etape-n" aria-hidden="true">
                {n}
              </span>
              <div>
                <h3 className="ent__etape-titre">
                  {t(`entreprises.etape${n}Titre`)}
                </h3>
                <p className="ent__etape-texte">
                  {t(`entreprises.etape${n}Texte`)}
                </p>
              </div>
            </li>
          ))}
        </ol>
      </section>

      {/* ── Ce qui est protégé ── */}
      <section className="container container--prose ent__section">
        <h2 className="ent__h2">{t("entreprises.confidentialiteTitre")}</h2>
        <p>{t("entreprises.confidentialiteIntro")}</p>

        <ul className="ent__liste">
          {["c1", "c2", "c3", "c4"].map((cle) => (
            <li key={cle}>
              <FiCheck aria-hidden="true" />
              <span>{t(`entreprises.confidentialite${cle}`)}</span>
            </li>
          ))}
        </ul>

        <p className="ent__note">{t("entreprises.confidentialiteNote")}</p>
      </section>

      {/* ── Données ouvertes : l'argument qui distingue ── */}
      <section className="container container--prose ent__section">
        <h2 className="ent__h2">{t("entreprises.ouvertesTitre")}</h2>
        <p>{t("entreprises.ouvertesP1")}</p>
        <p>{t("entreprises.ouvertesP2")}</p>
      </section>

      {/* ── Appel ── */}
      <section className="container container--wide ent__section">
        <div className="ent__cta">
          <span className="ent__cta-icon" aria-hidden="true">
            <FaCarSide />
          </span>
          <h2 className="ent__cta-titre">{t("entreprises.ctaTitre")}</h2>
          <p className="ent__cta-texte">{t("entreprises.ctaTexte")}</p>
          <div className="ent__cta-actions">
            <Button to="/register" icon={<FiArrowRight />}>
              {t("entreprises.ctaBouton")}
            </Button>
            <Button variant="secondary" to="/mentions-legales">
              {t("entreprises.ctaContact")}
            </Button>
          </div>
          <p className="ent__cta-note">{t("entreprises.ctaNote")}</p>
        </div>
      </section>
    </div>
  );
}
