import { useEffect, useState, type ReactNode } from "react";
import { Link } from "react-router-dom";
import { FiExternalLink } from "react-icons/fi";
import { useSeo, useDonneesStructurees } from "../../hooks/useSeo";
import { DATE_MAJ, DATE_MAJ_ISO, EDITEUR } from "../../config/legal";
import "./legal.css";

/** Une section de la page : le sommaire est déduit de cette liste. */
export interface Section {
  /** Ancre : sert d'`id` au titre et de cible au sommaire. */
  id: string;
  titre: string;
}

interface Props {
  titre: string;
  /** Phrase d'accroche affichée sous le titre. */
  chapeau: string;
  /** Description destinée aux moteurs. */
  description: string;
  chemin: string;
  version: string;
  sections: Section[];
  children: ReactNode;
}

/** Les quatre pages légales, pour la navigation latérale. */
const PAGES = [
  { chemin: "/mentions-legales", libelle: "Mentions légales" },
  { chemin: "/confidentialite", libelle: "Politique de confidentialité" },
  { chemin: "/cgu", libelle: "Conditions générales" },
  { chemin: "/cookies", libelle: "Cookies et traceurs" },
];

/**
 * Chrome commun aux pages légales.
 *
 * <h2>Pourquoi un sommaire ancré</h2>
 *
 * Une politique de confidentialité complète fait plusieurs milliers de mots.
 * L'article 12.1 du RGPD exige une information « concise, transparente,
 * compréhensible et aisément accessible » : la longueur est imposée par le
 * contenu, l'accessibilité dépend donc entièrement de la navigation. Un
 * sommaire qui suit le défilement transforme un mur de texte en document
 * consultable — on y cherche une réponse, on ne le lit pas d'un bout à l'autre.
 *
 * <h2>Le suivi de lecture</h2>
 *
 * La section courante est détectée par `IntersectionObserver` plutôt que par un
 * calcul de position au défilement : le navigateur fait le travail hors du fil
 * principal, et la page ne saccade pas.
 */
export default function LegalLayout({
  titre, chapeau, description, chemin, version, sections, children,
}: Props) {
  const [active, setActive] = useState(sections[0]?.id ?? "");

  useSeo({ titre, description, chemin });

  useDonneesStructurees({
    "@context": "https://schema.org",
    "@type": "WebPage",
    name: titre,
    description,
    inLanguage: "fr-BE",
    dateModified: DATE_MAJ_ISO,
    publisher: { "@type": "Organization", name: EDITEUR.denomination },
  });

  useEffect(() => {
    /* La marge haute écarte la zone masquée par l'en-tête fixe ; la marge basse
       très négative ne retient qu'une bande étroite près du haut de l'écran,
       sans quoi deux sections seraient actives en même temps. */
    const observateur = new IntersectionObserver(
      (entrees) => {
        const visible = entrees.filter((e) => e.isIntersecting);
        if (visible.length > 0) setActive(visible[0].target.id);
      },
      { rootMargin: "-96px 0px -70% 0px", threshold: 0 },
    );

    sections.forEach(({ id }) => {
      const cible = document.getElementById(id);
      if (cible) observateur.observe(cible);
    });

    return () => observateur.disconnect();
  }, [sections]);

  return (
    <div className="legal">
      <header className="legal__hero">
        <div className="container container--wide">
          <nav className="legal__fil" aria-label="Fil d'Ariane">
            <Link to="/">Accueil</Link>
            <span aria-hidden="true">/</span>
            <span aria-current="page">{titre}</span>
          </nav>
          <h1 className="legal__titre">{titre}</h1>
          <p className="legal__chapeau">{chapeau}</p>
          <p className="legal__meta">
            Version {version} · Dernière mise à jour le{" "}
            <time dateTime={DATE_MAJ_ISO}>{DATE_MAJ}</time>
          </p>
        </div>
      </header>

      <div className="container container--wide legal__corps">
        <aside className="legal__aside">
          <nav className="legal__sommaire" aria-label="Sommaire de la page">
            <p className="legal__aside-titre">Sur cette page</p>
            <ol>
              {sections.map((s) => (
                <li key={s.id}>
                  <a
                    href={`#${s.id}`}
                    className={active === s.id ? "is-active" : undefined}
                    aria-current={active === s.id ? "location" : undefined}
                  >
                    {s.titre}
                  </a>
                </li>
              ))}
            </ol>
          </nav>

          <nav className="legal__autres" aria-label="Autres documents légaux">
            <p className="legal__aside-titre">Autres documents</p>
            <ul>
              {PAGES.filter((p) => p.chemin !== chemin).map((p) => (
                <li key={p.chemin}>
                  <Link to={p.chemin}>{p.libelle}</Link>
                </li>
              ))}
            </ul>
          </nav>
        </aside>

        <main className="legal__contenu">
          <div className="legal__avertissement" role="note">
            <p>
              <strong>Projet académique.</strong> CoShift est un travail de fin
              d'études. Les données d'identification de l'éditeur sont fictives :
              le numéro d'entreprise {EDITEUR.bce} porte une somme de contrôle
              volontairement invalide et ne peut désigner aucune société réelle.
              Les analyses juridiques, en revanche, portent sur le
              fonctionnement réel de l'application.
            </p>
          </div>

          {children}

          <footer className="legal__pied">
            <p>
              Une question sur ce document ?{" "}
              <a href={`mailto:${EDITEUR.contact}`}>
                {EDITEUR.contact} <FiExternalLink aria-hidden="true" />
              </a>
            </p>
          </footer>
        </main>
      </div>
    </div>
  );
}

/** Section de contenu : porte l'ancre observée par le sommaire. */
export function LegalSection({
  id, titre, children,
}: { id: string; titre: string; children: ReactNode }) {
  return (
    <section className="legal__section" aria-labelledby={id}>
      <h2 id={id}>{titre}</h2>
      {children}
    </section>
  );
}

/**
 * Encadré citant le texte applicable.
 *
 * <p>Affirmer une obligation sans la rattacher à sa source oblige le lecteur à
 * nous croire sur parole. L'encadré nomme l'article, ce qui rend l'affirmation
 * vérifiable — et réfutable.</p>
 */
export function LegalSource({ children }: { children: ReactNode }) {
  return (
    <aside className="legal__source">
      <span className="legal__source-etiquette">Fondement</span>
      <p>{children}</p>
    </aside>
  );
}
