import { useEffect, useId, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { FiMap, FiShield } from "react-icons/fi";
import { FcGoogle } from "react-icons/fc";
import { Button } from "../ui";
import { useConsent } from "../../context/ConsentContext";
import { useT } from "../../context/LangContext";
import "./ConsentBanner.css";

/**
 * Bandeau de recueil du consentement.
 *
 * <h2>Ce que la mise en forme doit garantir</h2>
 *
 * La conformité d'un bandeau ne tient pas à son texte mais à sa géométrie.
 * Trois exigences se lisent directement dans le rendu :
 *
 * <ol>
 *   <li><strong>Refuser aussi simple qu'accepter.</strong> Les deux boutons
 *       ont la même taille, le même style et le même rang : un clic chacun.
 *       Un « Tout accepter » coloré face à un « Paramétrer » en petit texte
 *       gris est le motif que les autorités de contrôle sanctionnent le plus
 *       souvent, parce qu'il rend le refus plus coûteux que l'acceptation.</li>
 *   <li><strong>Aucun choix par défaut.</strong> Les interrupteurs du panneau
 *       détaillé sont fermés à l'ouverture. Une case pré-cochée ne vaut pas
 *       consentement — la Cour de justice l'a jugé dans l'affaire
 *       <em>Planet49</em> (C-673/17, 1er octobre 2019).</li>
 *   <li><strong>Aucune fermeture sans réponse.</strong> Le bandeau ne porte pas
 *       de croix. Fermer sans choisir laisserait un état ambigu que le code
 *       devrait interpréter, et l'interprétation par défaut est précisément ce
 *       que le consentement doit exclure.</li>
 * </ol>
 *
 * <p>Le bandeau n'obscurcit pas la page et ne bloque pas la lecture : le
 * consentement doit être libre, et une page rendue illisible tant qu'on n'a pas
 * accepté exerce une contrainte.</p>
 */
export default function ConsentBanner() {
  const {
    aRepondu, accepterTout, refuserTout, enregistrer,
    panneauOuvert, ouvrirPanneau, fermerPanneau,
  } = useConsent();

  const t = useT();
  const [google, setGoogle] = useState(false);
  const [mapbox, setMapbox] = useState(false);
  const titreId = useId();
  const bandeau = useRef<HTMLDivElement>(null);

  /* À chaque réouverture, les interrupteurs repartent fermés : un panneau qui
     conserverait les positions d'un choix précédent proposerait un défaut. */
  useEffect(() => {
    if (panneauOuvert) {
      setGoogle(false);
      setMapbox(false);
    }
  }, [panneauOuvert]);

  /* Le bandeau prend le focus à l'apparition : une personne qui navigue au
     clavier ou au lecteur d'écran doit pouvoir répondre sans traverser toute
     la page. */
  useEffect(() => {
    if (!aRepondu) bandeau.current?.focus();
  }, [aRepondu]);

  if (aRepondu) return null;

  return (
    <div
      className="consent"
      role="dialog"
      aria-labelledby={titreId}
      aria-describedby={`${titreId}-texte`}
      ref={bandeau}
      tabIndex={-1}
    >
      <div className="consent__boite">
        <div className="consent__entete">
          <span className="consent__icone" aria-hidden="true">
            <FiShield />
          </span>
          <div>
            <h2 className="consent__titre" id={titreId}>
              {t("consentement.titre")}
            </h2>
            <p className="consent__texte" id={`${titreId}-texte`}>
              {t("consentement.texte")}{" "}
              <strong>{t("consentement.texteFort")}</strong>
            </p>
          </div>
        </div>

        {panneauOuvert && (
          <div className="consent__detail">
            <label className="consent__option">
              <input
                type="checkbox"
                checked={mapbox}
                onChange={(e) => setMapbox(e.target.checked)}
              />
              <span className="consent__option-icone" aria-hidden="true">
                <FiMap />
              </span>
              <span className="consent__option-corps">
                <span className="consent__option-titre">{t("consentement.carteTitre")}</span>
                <span className="consent__option-texte">
                  {t("consentement.carteTexte")}
                </span>
              </span>
            </label>

            <label className="consent__option">
              <input
                type="checkbox"
                checked={google}
                onChange={(e) => setGoogle(e.target.checked)}
              />
              <span className="consent__option-icone" aria-hidden="true">
                <FcGoogle />
              </span>
              <span className="consent__option-corps">
                <span className="consent__option-titre">
                  {t("consentement.googleTitre")}
                </span>
                <span className="consent__option-texte">
                  {t("consentement.googleTexte")}
                </span>
              </span>
            </label>
          </div>
        )}

        <div className="consent__actions">
          {/* Même variante, même taille, même rang : le refus ne coûte pas plus
              cher que l'acceptation. */}
          <Button variant="secondary" onClick={refuserTout}>
            {t("consentement.toutRefuser")}
          </Button>
          <Button variant="secondary" onClick={accepterTout}>
            {t("consentement.toutAccepter")}
          </Button>

          {panneauOuvert ? (
            <>
              <Button
                variant="primary"
                onClick={() => enregistrer({ google, mapbox })}
              >
                {t("consentement.enregistrerChoix")}
              </Button>
              <button
                type="button"
                className="consent__lien is-inline"
                onClick={fermerPanneau}
              >
                {t("consentement.replier")}
              </button>
            </>
          ) : (
            <button
              type="button"
              className="consent__lien is-inline"
              onClick={ouvrirPanneau}
              aria-expanded={false}
            >
              {t("consentement.serviceParService")}
            </button>
          )}
        </div>

        <p className="consent__pied">
          {t("consentement.pied")}{" "}
          <Link to="/cookies">{t("consentement.piedLien")}</Link>.
        </p>
      </div>
    </div>
  );
}
