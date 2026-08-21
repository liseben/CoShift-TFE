import { Link } from "react-router-dom";
import {
  FaCar, FaUsers, FaLeaf, FaShieldAlt, FaBuilding, FaUniversity, FaMusic,
} from "react-icons/fa";
import { FiArrowRight, FiCheck } from "react-icons/fi";
import { Button, Card } from "../../components/ui";
import Logo from "../../components/Logo/Logo";
import { PHOTOS } from "../../components/image_site";
import { useT } from "../../context/LangContext";
import { useSeo } from "../../hooks/useSeo";
import "./AboutPage.css";

/* Les trois tableaux ne portent plus que des clés : évalués au chargement du
   module, ils ne peuvent pas atteindre le contexte de langue. */
const ETAPES = ["1", "2", "3", "4"];

const PUBLICS = [
  { icon: <FaBuilding />,   cle: "public1" },
  { icon: <FaUniversity />, cle: "public2" },
  { icon: <FaMusic />,      cle: "public3" },
];

const PRINCIPES = [
  { icon: <FaUsers />,     cle: "principe1" },
  { icon: <FaLeaf />,      cle: "principe2" },
  { icon: <FaShieldAlt />, cle: "principe3" },
];

export default function AboutPage() {
  const t = useT();

  useSeo({
    titre: t("apropos.titre"),
    description: t("apropos.description"),
    chemin: "/a-propos",
  });

  return (
    <div className="about">
      {/* ── Ouverture ── */}
      <section className="about__hero">
        <div className="container container--prose">
          <Logo size={48} />
          <h1 className="about__title">{t("apropos.heroTitre")}</h1>
          <p className="about__lead">{t("apropos.heroAccroche")}</p>
        </div>
      </section>

      <figure className="about__figure about__figure--wide">
        <img src={PHOTOS.habitacle.src} alt={t(PHOTOS.habitacle.alt)}
             loading="lazy" decoding="async" />
      </figure>

      {/* ── Le problème ── */}
      <section className="container container--prose about__section">
        <h2>{t("apropos.constatTitre")}</h2>
        <p>{t("apropos.constatP1")}</p>
        <p>{t("apropos.constatP2")}</p>
        <p className="about__highlight">{t("apropos.constatFort")}</p>
      </section>

      {/* ── Comment ça marche ── */}
      <section className="about__steps-wrap">
        <div className="container container--wide">
          <h2 className="about__center">{t("apropos.fonctionnementTitre")}</h2>

          <figure className="about__figure">
            <img src={PHOTOS.rendezVous.src} alt={t(PHOTOS.rendezVous.alt)}
                 loading="lazy" decoding="async" />
            <figcaption>{t("apropos.fonctionnementLegende")}</figcaption>
          </figure>
          <ol className="about__steps">
            {ETAPES.map((n) => (
              <li key={n} className="about__step">
                <span className="about__step-n" aria-hidden="true">{n}</span>
                <div>
                  <h3>{t(`apropos.etape${n}Titre`)}</h3>
                  <p>{t(`apropos.etape${n}Texte`)}</p>
                </div>
              </li>
            ))}
          </ol>
        </div>
      </section>

      {/* ── Pour qui ── */}
      <section className="container container--wide about__section">
        <h2 className="about__center">{t("apropos.pourQui")}</h2>
        <div className="grid-auto">
          {PUBLICS.map((p) => (
            <Card key={p.cle} padding="lg">
              <span className="about__icon" aria-hidden="true">{p.icon}</span>
              <h3 className="about__card-title">{t(`apropos.${p.cle}Titre`)}</h3>
              <p className="about__card-text">{t(`apropos.${p.cle}Texte`)}</p>
            </Card>
          ))}
        </div>
      </section>

      {/* ── Nos partis pris ── */}
      <section className="about__steps-wrap">
        <div className="container container--wide">
          <h2 className="about__center">{t("apropos.partisPris")}</h2>
          <div className="grid-auto">
            {PRINCIPES.map((p) => (
              <Card key={p.cle} padding="lg">
                <span className="about__icon about__icon--eco" aria-hidden="true">
                  {p.icon}
                </span>
                <h3 className="about__card-title">{t(`apropos.${p.cle}Titre`)}</h3>
                <p className="about__card-text">{t(`apropos.${p.cle}Texte`)}</p>
              </Card>
            ))}
          </div>
        </div>
      </section>

      {/* ── Le projet ── */}
      <section className="container container--prose about__section">
        <h2>{t("apropos.origineTitre")}</h2>
        <p>{t("apropos.origineP1")}</p>
        <p>{t("apropos.origineP2")}</p>

        <ul className="about__state">
          {[1, 2, 3, 4, 5].map((i) => (
            <li key={i}>
              <FiCheck aria-hidden="true" /> {t(`apropos.fait${i}`)}
            </li>
          ))}
        </ul>

        <p className="about__note">{t("apropos.origineNote")}</p>
      </section>

      {/* ── Appel à l'action ── */}
      <section className="about__cta-wrap">
        <div className="container container--prose about__cta">
          <FaCar className="about__cta-icon" aria-hidden="true" />
          <h2>{t("apropos.ctaTitre")}</h2>
          <p>{t("apropos.ctaTexte")}</p>
          <div className="about__cta-actions">
            <Button to="/register" size="lg">{t("commun.creerCompte")}</Button>
            <Button to="/trips/search" variant="secondary" size="lg" icon={<FiArrowRight />}>
              {t("apropos.ctaVoirTrajets")}
            </Button>
          </div>
          <p className="about__contact">
            {t("apropos.ctaContact")}{" "}
            <Link to="/entreprises">{t("apropos.ctaContactLien")}</Link>
          </p>
        </div>
      </section>
    </div>
  );
}
