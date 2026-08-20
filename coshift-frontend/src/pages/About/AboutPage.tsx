import { Link } from "react-router-dom";
import {
  FaCar, FaUsers, FaLeaf, FaShieldAlt, FaBuilding, FaUniversity, FaMusic,
} from "react-icons/fa";
import { FiArrowRight, FiCheck } from "react-icons/fi";
import { Button, Card } from "../../components/ui";
import Logo from "../../components/Logo/Logo";
import { PHOTOS } from "../../components/image_site";
import { useSeo } from "../../hooks/useSeo";
import "./AboutPage.css";

const ETAPES = [
  {
    n: "1",
    titre: "Vous rejoignez votre organisation",
    texte:
      "L'inscription se fait avec votre adresse professionnelle. C'est elle qui vous rattache à votre entreprise, votre école ou l'événement, et qui garantit à vos collègues qu'ils partagent la route avec quelqu'un d'identifié.",
  },
  {
    n: "2",
    titre: "Vous publiez ou vous cherchez",
    texte:
      "Un conducteur déclare son véhicule, indique son itinéraire, son horaire et le nombre de places. Un passager cherche par ville, par date et par heure de départ.",
  },
  {
    n: "3",
    titre: "Le conducteur accepte",
    texte:
      "Une demande n'est pas une réservation ferme : le conducteur l'accepte ou la refuse, en motivant son refus. C'est lui qui décide qui monte dans sa voiture.",
  },
  {
    n: "4",
    titre: "Vous partagez la route et les frais",
    texte:
      "Le numéro de téléphone du conducteur n'est transmis qu'une fois la réservation confirmée. Le prix couvre le partage des frais, jamais un bénéfice.",
  },
];

const PUBLICS = [
  {
    icon: <FaBuilding />,
    titre: "Entreprises",
    texte:
      "Le stationnement sature, les horaires se ressemblent et les trajets se doublent. Regrouper les navetteurs d'un même site est le levier le plus simple.",
  },
  {
    icon: <FaUniversity />,
    titre: "Universités et hautes écoles",
    texte:
      "Des milliers d'étudiants convergent aux mêmes heures vers un campus rarement desservi comme un centre-ville. Le covoiturage comble ce que le train ne fait pas.",
  },
  {
    icon: <FaMusic />,
    titre: "Festivals et salons",
    texte:
      "Un événement crée un pic de circulation sur quelques heures. Organiser le partage en amont évite un parking improvisé dans un champ.",
  },
];

const PRINCIPES = [
  {
    icon: <FaUsers />,
    titre: "Un cercle fermé, pas une place publique",
    texte:
      "CoShift n'est pas un service ouvert à tous. On y covoiture avec les membres de son organisation, ce qui change entièrement le rapport de confiance.",
  },
  {
    icon: <FaLeaf />,
    titre: "Le partage plutôt que le trajet",
    texte:
      "Un trajet effectué à deux ne devient pas plus écologique : c'est la voiture restée au garage qui compte. Toute l'interface met en avant les places effectivement partagées.",
  },
  {
    icon: <FaShieldAlt />,
    titre: "Le minimum de données",
    texte:
      "Adresse professionnelle, nom, éventuellement un téléphone. Pas de géolocalisation continue, pas de suivi publicitaire, pas de revente.",
  },
];

export default function AboutPage() {
  useSeo({
    titre: "À propos — pourquoi CoShift existe",
    description:
      "CoShift est né d'un constat : la plupart des voitures qui vont au travail chaque matin transportent une seule personne. Voici la démarche et les choix du projet.",
    chemin: "/a-propos",
  });

  return (
    <div className="about">
      {/* ── Ouverture ── */}
      <section className="about__hero">
        <div className="container container--prose">
          <Logo size={48} />
          <h1 className="about__title">
            Le covoiturage qui commence à la porte de votre organisation
          </h1>
          <p className="about__lead">
            CoShift met en relation les personnes qui font le même trajet, au
            même moment, pour aller au même endroit — leur lieu de travail, leur
            campus, un événement. Rien de plus, et c'est déjà beaucoup.
          </p>
        </div>
      </section>

      <figure className="about__figure about__figure--wide">
        <img src={PHOTOS.habitacle.src} alt={PHOTOS.habitacle.alt}
             loading="lazy" decoding="async" />
      </figure>

      {/* ── Le problème ── */}
      <section className="container container--prose about__section">
        <h2>Le constat de départ</h2>
        <p>
          Chaque matin, des voitures parcourent le même itinéraire, à la même
          heure, vers la même destination — avec une seule personne à bord. Ce
          n'est pas un choix : c'est l'absence d'alternative pratique. Le train
          ne dessert pas le zoning, le bus impose deux correspondances, et
          personne ne sait qui, parmi ses collègues, part du même quartier.
        </p>
        <p>
          Les plateformes de covoiturage grand public répondent mal à ce
          besoin. Elles sont conçues pour le trajet exceptionnel — un
          Bruxelles-Paris un vendredi soir — pas pour les vingt kilomètres
          répétés deux fois par jour, cinq jours par semaine, avec des gens
          qu'on retrouve à la machine à café.
        </p>
        <p className="about__highlight">
          CoShift part de l'organisation, pas du trajet. C'est ce déplacement du
          point de départ qui change tout le reste.
        </p>
      </section>

      {/* ── Comment ça marche ── */}
      <section className="about__steps-wrap">
        <div className="container container--wide">
          <h2 className="about__center">Comment ça fonctionne</h2>

          <figure className="about__figure">
            <img src={PHOTOS.rendezVous.src} alt={PHOTOS.rendezVous.alt}
                 loading="lazy" decoding="async" />
            <figcaption>
              Le point de rendez-vous est indiqué par le conducteur au moment
              où il publie son trajet.
            </figcaption>
          </figure>
          <ol className="about__steps">
            {ETAPES.map((e) => (
              <li key={e.n} className="about__step">
                <span className="about__step-n" aria-hidden="true">{e.n}</span>
                <div>
                  <h3>{e.titre}</h3>
                  <p>{e.texte}</p>
                </div>
              </li>
            ))}
          </ol>
        </div>
      </section>

      {/* ── Pour qui ── */}
      <section className="container container--wide about__section">
        <h2 className="about__center">Pour qui</h2>
        <div className="grid-auto">
          {PUBLICS.map((p) => (
            <Card key={p.titre} padding="lg">
              <span className="about__icon" aria-hidden="true">{p.icon}</span>
              <h3 className="about__card-title">{p.titre}</h3>
              <p className="about__card-text">{p.texte}</p>
            </Card>
          ))}
        </div>
      </section>

      {/* ── Nos partis pris ── */}
      <section className="about__steps-wrap">
        <div className="container container--wide">
          <h2 className="about__center">Nos partis pris</h2>
          <div className="grid-auto">
            {PRINCIPES.map((p) => (
              <Card key={p.titre} padding="lg">
                <span className="about__icon about__icon--eco" aria-hidden="true">
                  {p.icon}
                </span>
                <h3 className="about__card-title">{p.titre}</h3>
                <p className="about__card-text">{p.texte}</p>
              </Card>
            ))}
          </div>
        </div>
      </section>

      {/* ── Le projet ── */}
      <section className="container container--prose about__section">
        <h2>D'où vient CoShift</h2>
        <p>
          CoShift est né d'un travail de fin d'études, développé de bout en bout
          — interface, API, base de données. Ce n'est pas une entreprise avec
          des années d'existence derrière elle, et la page que vous lisez ne
          prétendra pas le contraire.
        </p>
        <p>
          Ce que le projet revendique, en revanche, c'est d'être fonctionnel :
          la publication d'un trajet, la recherche, la demande de place,
          l'acceptation par le conducteur et le suivi des réservations
          fonctionnent réellement, avec une vraie base de données derrière.
        </p>

        <ul className="about__state">
          <li><FiCheck aria-hidden="true" /> Publication et recherche de trajets</li>
          <li><FiCheck aria-hidden="true" /> Réservation, acceptation, refus motivé, annulation</li>
          <li><FiCheck aria-hidden="true" /> Gestion des véhicules et du profil</li>
          <li><FiCheck aria-hidden="true" /> Vérification de l'adresse par code à six chiffres</li>
          <li><FiCheck aria-hidden="true" /> Flux d'actualités mobilité</li>
        </ul>

        <p className="about__note">
          L'espace dédié aux organisations, la notation entre membres et la
          messagerie interne sont les chantiers suivants. Les annoncer comme
          disponibles serait plus vendeur, mais faux.
        </p>
      </section>

      {/* ── Appel à l'action ── */}
      <section className="about__cta-wrap">
        <div className="container container--prose about__cta">
          <FaCar className="about__cta-icon" aria-hidden="true" />
          <h2>Prêt à partager la route ?</h2>
          <p>
            Créez votre compte avec votre adresse professionnelle et voyez qui,
            autour de vous, fait déjà le même trajet.
          </p>
          <div className="about__cta-actions">
            <Button to="/register" size="lg">Créer un compte</Button>
            <Button to="/trips/search" variant="secondary" size="lg" icon={<FiArrowRight />}>
              Voir les trajets
            </Button>
          </div>
          <p className="about__contact">
            Une question sur le déploiement dans votre organisation ?{" "}
            <Link to="/entreprises">Espace entreprises</Link>
          </p>
        </div>
      </section>
    </div>
  );
}
