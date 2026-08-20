import { Link } from "react-router-dom";
import { Button } from "../../components/ui";
import { useConsent } from "../../context/ConsentContext";
import LegalLayout, { LegalSection, LegalSource, type Section } from "./LegalLayout";
import { TRACAGES } from "../../config/legal";

const SECTIONS: Section[] = [
  { id: "vocabulaire", titre: "Pourquoi cette page ne parle pas que de cookies" },
  { id: "inventaire", titre: "Ce qui est réellement stocké" },
  { id: "exemptes", titre: "Ce qui ne demande pas votre accord" },
  { id: "soumis", titre: "Ce qui demande votre accord" },
  { id: "recueil", titre: "Comment votre accord est recueilli" },
  { id: "modifier", titre: "Modifier ou retirer votre choix" },
  { id: "absents", titre: "Ce que CoShift n'utilise pas" },
];

export default function CookiesPage() {
  const { choix, reinitialiser } = useConsent();

  const exemptes = TRACAGES.filter((t) => t.exempte);
  const soumis = TRACAGES.filter((t) => !t.exempte);

  return (
    <LegalLayout
      titre="Cookies et traceurs"
      chapeau="CoShift ne dépose aucun cookie publicitaire. Il stocke en revanche trois informations dans votre navigateur, et deux fonctions facultatives font appel à des services tiers. Voici lesquelles, pourquoi, et comment y couper court."
      description="Inventaire des informations stockées par CoShift dans le navigateur, régime de consentement applicable, et moyen de modifier son choix à tout moment."
      chemin="/cookies"
      version="1.0"
      sections={SECTIONS}
    >
      <LegalSection id="vocabulaire" titre="Pourquoi cette page ne parle pas que de cookies">
        <p>
          CoShift ne dépose aucun cookie de première partie. Il serait donc
          tentant d'écrire « ce site n'utilise pas de cookies » et de s'arrêter
          là. Ce serait une réponse à côté de la question.
        </p>
        <p>
          Le texte applicable ne vise pas les cookies&nbsp;: il vise{" "}
          <strong>
            le stockage d'informations, ou l'obtention de l'accès à des
            informations déjà stockées, dans l'équipement terminal
          </strong>{" "}
          de l'utilisateur. La formulation est délibérément neutre du point de
          vue technique. Un cookie, une clé dans le stockage local du navigateur
          ou une empreinte matérielle relèvent du même régime.
        </p>
        <p>
          CoShift utilise le stockage local. Cette page en rend donc compte
          exactement comme d'un cookie.
        </p>

        <LegalSource>
          Article 129 de la loi du 13 juin 2005 relative aux communications
          électroniques, transposant l'article 5.3 de la directive 2002/58/CE
          dite « vie privée et communications électroniques », telle que modifiée
          par la directive 2009/136/CE.
        </LegalSource>
      </LegalSection>

      <LegalSection id="inventaire" titre="Ce qui est réellement stocké">
        <div className="legal__table-wrap">
          <table className="legal__table">
            <thead>
              <tr>
                <th scope="col">Clé ou service</th>
                <th scope="col">Nature</th>
                <th scope="col">À quoi cela sert</th>
                <th scope="col">Durée</th>
                <th scope="col">Accord</th>
              </tr>
            </thead>
            <tbody>
              {TRACAGES.map((t) => (
                <tr key={t.cle}>
                  <th scope="row">
                    <code>{t.cle}</code>
                  </th>
                  <td>{t.type}</td>
                  <td>{t.finalite}</td>
                  <td>{t.duree}</td>
                  <td>{t.exempte ? "Non requis" : "Requis"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </LegalSection>

      <LegalSection id="exemptes" titre="Ce qui ne demande pas votre accord">
        <p>
          Le même article 129 exempte le stockage{" "}
          <strong>
            strictement nécessaire à la fourniture d'un service expressément
            demandé
          </strong>{" "}
          par l'utilisateur. Trois clés en relèvent&nbsp;:
        </p>
        <dl>
          {exemptes.map((t) => (
            <div key={t.cle}>
              <dt>
                <code>{t.cle}</code>
              </dt>
              <dd>{t.finalite}</dd>
            </div>
          ))}
        </dl>
        <p>
          Le cas de la trace de consentement mérite d'être explicité&nbsp;: elle
          n'est pas exemptée parce qu'elle serait anodine, mais parce que sans
          elle, la question reparaîtrait à chaque visite et un refus ne pourrait
          jamais être respecté. Mémoriser un refus est la condition même pour
          l'honorer.
        </p>
      </LegalSection>

      <LegalSection id="soumis" titre="Ce qui demande votre accord">
        <p>
          Deux fonctions font appel à des services tiers établis aux États-Unis.
          L'un et l'autre reçoivent votre adresse IP dès qu'ils sont
          chargés&nbsp;— avant même que vous n'ayez cliqué sur quoi que ce soit.
          Ni l'un ni l'autre n'est nécessaire au service&nbsp;:
        </p>
        <dl>
          {soumis.map((t) => (
            <div key={t.cle}>
              <dt>{t.cle}</dt>
              <dd>{t.finalite}</dd>
            </div>
          ))}
        </dl>

        <div className="legal__constat">
          <p>
            <strong>Vérifiable.</strong> Ouvrez l'onglet réseau de votre
            navigateur et rechargez la page d'accueil sans avoir répondu au
            bandeau&nbsp;: aucune requête ne part vers{" "}
            <code>accounts.google.com</code> ni vers <code>api.mapbox.com</code>.
            Le chargement de ces scripts est conditionné au consentement dans le
            code, non désactivé après coup.
          </p>
        </div>

        <p>
          Le détail de ce que ces tiers reçoivent, du pays où les données sont
          transférées et de l'instrument juridique qui encadre ce transfert
          figure dans la{" "}
          <Link to="/confidentialite#tiers">politique de confidentialité</Link>.
        </p>
      </LegalSection>

      <LegalSection id="recueil" titre="Comment votre accord est recueilli">
        <p>
          Le bandeau affiché à la première visite respecte quatre règles, qui se
          lisent dans sa forme autant que dans son texte&nbsp;:
        </p>
        <ol>
          <li>
            <strong>Refuser coûte un clic, comme accepter.</strong> Les deux
            boutons ont la même taille, le même style et le même rang. Un
            bouton d'acceptation coloré face à un lien de refus en petit gris
            rendrait le refus plus coûteux&nbsp;— et le consentement ne serait
            plus libre.
          </li>
          <li>
            <strong>Rien n'est pré-coché.</strong> Les interrupteurs du réglage
            détaillé sont fermés à l'ouverture, et le restent tant qu'on ne les
            actionne pas.
          </li>
          <li>
            <strong>Le silence ne vaut pas accord.</strong> Ni le défilement, ni
            la navigation vers une autre page, ni la fermeture du bandeau ne
            valent consentement. Le bandeau ne porte d'ailleurs pas de croix de
            fermeture.
          </li>
          <li>
            <strong>Rien n'est chargé avant la réponse.</strong> Le
            consentement est demandé avant le dépôt, non après.
          </li>
        </ol>

        <LegalSource>
          Article 4.11 du règlement (UE) 2016/679, qui définit le consentement
          comme une manifestation de volonté libre, spécifique, éclairée et
          univoque. Sur l'inefficacité d'une case pré-cochée&nbsp;: Cour de
          justice de l'Union européenne, <em>Planet49</em>, C-673/17, 1er octobre
          2019. Sur la nécessité d'un consentement distinct par finalité&nbsp;:
          même arrêt, points 58 et suivants.
        </LegalSource>
      </LegalSection>

      <LegalSection id="modifier" titre="Modifier ou retirer votre choix">
        <p>
          Le retrait doit être aussi simple que l'octroi. Un bouton suffit
          donc&nbsp;: il efface le choix enregistré et fait réapparaître le
          bandeau immédiatement.
        </p>

        <p>
          {choix ? (
            <>
              Votre choix actuel&nbsp;: carte animée{" "}
              <strong>{choix.mapbox ? "autorisée" : "refusée"}</strong>,
              connexion Google{" "}
              <strong>{choix.google ? "autorisée" : "refusée"}</strong>,
              exprimé le{" "}
              <time dateTime={choix.date}>
                {new Date(choix.date).toLocaleDateString("fr-BE", {
                  day: "numeric", month: "long", year: "numeric",
                })}
              </time>
              .
            </>
          ) : (
            <>
              Vous n'avez pas encore répondu au bandeau. Aucun service tiers
              n'est chargé.
            </>
          )}
        </p>

        <p>
          <Button variant="secondary" onClick={reinitialiser}>
            Revoir mon choix
          </Button>
        </p>

        <p>
          Le même lien figure en permanence dans le pied de page du site. Votre
          choix expire par ailleurs au bout de six mois, après quoi la question
          vous est reposée&nbsp;: un consentement perpétuel ne serait plus
          éclairé.
        </p>

        <LegalSource>
          Article 7.3 du règlement&nbsp;: la personne concernée a le droit de
          retirer son consentement à tout moment, et il doit être aussi simple de
          le retirer que de le donner.
        </LegalSource>

        <h3>Effacer les données déjà stockées</h3>
        <p>
          Le bouton ci-dessus efface le choix conservé par CoShift. Les cookies
          éventuellement déposés par Google lors d'une session antérieure
          relèvent de Google&nbsp;: ils s'effacent depuis les réglages de
          confidentialité de votre navigateur, rubrique cookies et données de
          sites.
        </p>
      </LegalSection>

      <LegalSection id="absents" titre="Ce que CoShift n'utilise pas">
        <p>
          L'inventaire d'une politique de cookies gagne à indiquer aussi ce qui
          n'y figure pas. CoShift n'emploie&nbsp;:
        </p>
        <ul>
          <li>
            aucun outil de mesure d'audience — ni Google Analytics, ni
            alternative&nbsp;;
          </li>
          <li>aucun pixel de réseau social ni bouton de partage traçant&nbsp;;</li>
          <li>aucune régie publicitaire, aucun identifiant partagé entre sites&nbsp;;</li>
          <li>aucune carte de chaleur ni enregistrement de session&nbsp;;</li>
          <li>
            aucune police de caractères servie par un tiers&nbsp;: Inter et
            Outfit sont hébergées par CoShift, ce qui évite de transmettre votre
            adresse IP à un serveur de polices à chaque page.
          </li>
        </ul>
        <p>
          Cette absence n'est pas un oubli à combler plus tard. Mesurer une
          audience se fait aujourd'hui sans traceur, par l'analyse des journaux
          du serveur&nbsp;; c'est la voie retenue si le besoin se présente.
        </p>
      </LegalSection>
    </LegalLayout>
  );
}
