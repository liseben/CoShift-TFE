import { Link } from "react-router-dom";
import LegalLayout, { LegalSection, LegalSource, type Section } from "./LegalLayout";
import { AUTORITES, EDITEUR, HEBERGEMENT } from "../../config/legal";

const SECTIONS: Section[] = [
  { id: "editeur", titre: "Éditeur du site" },
  { id: "hebergement", titre: "Hébergement" },
  { id: "qualification", titre: "Qualification juridique du service" },
  { id: "responsabilite", titre: "Responsabilité" },
  { id: "signalement", titre: "Signaler un contenu illicite" },
  { id: "propriete", titre: "Propriété intellectuelle" },
  { id: "droit", titre: "Droit applicable et juridictions" },
  { id: "recours", titre: "Autorités de contrôle" },
];

export default function MentionsLegalesPage() {
  return (
    <LegalLayout
      titre="Mentions légales"
      chapeau="Qui édite CoShift, à quel titre, et devant qui répondre. L'identification du prestataire n'est pas une formalité : c'est ce qui permet à un utilisateur de savoir à qui il a affaire avant de confier quoi que ce soit."
      description="Identification de l'éditeur de CoShift, régime de responsabilité, signalement de contenus illicites, droit applicable et autorités de contrôle compétentes."
      chemin="/mentions-legales"
      version="1.0"
      sections={SECTIONS}
    >
      <LegalSection id="editeur" titre="Éditeur du site">
        <p>
          Le site <strong>{EDITEUR.domaine}</strong> et l'application CoShift
          sont édités par&nbsp;:
        </p>

        <div className="legal__fiche">
          <dl>
            <dt>Dénomination</dt>
            <dd>{EDITEUR.denomination}</dd>

            <dt>Forme juridique</dt>
            <dd>{EDITEUR.forme}</dd>

            <dt>Siège social</dt>
            <dd>{EDITEUR.siege}</dd>

            <dt>Numéro d'entreprise</dt>
            <dd>{EDITEUR.bce}</dd>

            <dt>Numéro de TVA</dt>
            <dd>{EDITEUR.tva}</dd>

            <dt>Registre</dt>
            <dd>{EDITEUR.rpm}</dd>

            <dt>Représentant</dt>
            <dd>{EDITEUR.representant}, éditeur responsable</dd>

            <dt>Contact général</dt>
            <dd>
              <a href={`mailto:${EDITEUR.contact}`}>{EDITEUR.contact}</a>
            </dd>

            <dt>Données personnelles</dt>
            <dd>
              <a href={`mailto:${EDITEUR.viePrivee}`}>{EDITEUR.viePrivee}</a>
            </dd>

            <dt>Signalements</dt>
            <dd>
              <a href={`mailto:${EDITEUR.signalement}`}>{EDITEUR.signalement}</a>
            </dd>
          </dl>
        </div>

        <LegalSource>
          Article XII.6 du Code de droit économique, qui transpose l'article 5
          de la directive 2000/31/CE sur le commerce électronique. Le prestataire
          d'un service de la société de l'information doit rendre accessibles, de
          manière permanente et facile, son nom, son adresse géographique, ses
          coordonnées électroniques et son numéro d'entreprise.
        </LegalSource>

        <p>
          Ces coordonnées constituent le <strong>point de contact unique</strong>{" "}
          prévu par les articles 11 et 12 du règlement (UE) 2022/2065 sur les
          services numériques, tant pour les autorités que pour les utilisateurs.
          La langue de communication est le français.
        </p>
      </LegalSection>

      <LegalSection id="hebergement" titre="Hébergement">
        <p>
          <strong>Statut actuel&nbsp;: {HEBERGEMENT.statut}.</strong>
        </p>
        <p>
          Désigner ici un hébergeur qui n'héberge rien serait une mention
          inexacte, alors que l'obligation d'identification vise précisément la
          sincérité. L'hébergeur sera nommé à cet endroit dès la mise en ligne,
          et sera retenu selon les critères suivants&nbsp;:
        </p>
        <ul>
          {HEBERGEMENT.criteres.map((c) => (
            <li key={c}>{c}</li>
          ))}
        </ul>
      </LegalSection>

      <LegalSection id="qualification" titre="Qualification juridique du service">
        <p>
          CoShift met en relation des personnes membres d'une même organisation
          qui effectuent un trajet comparable. La plateforme{" "}
          <strong>ne transporte personne</strong>, ne fixe pas les itinéraires,
          n'impose pas de tarif et ne rémunère aucun conducteur.
        </p>

        <h3>Un service de la société de l'information, pas un service de transport</h3>
        <p>
          La distinction n'est pas cosmétique&nbsp;: elle décide du régime
          applicable. Dans l'affaire{" "}
          <em>Asociación Profesional Elite Taxi</em> (C-434/15, 20 décembre
          2017), la Cour de justice de l'Union européenne a jugé qu'Uber
          fournissait un service relevant du domaine des transports, et non un
          service de la société de l'information, parce qu'elle{" "}
          <em>créait l'offre</em> de transport urbain et en{" "}
          <em>contrôlait les conditions déterminantes</em> — sélection des
          conducteurs, fixation du prix maximal, notation contraignante.
        </p>
        <p>
          Deux ans plus tard, dans <em>Airbnb Ireland</em> (C-390/18, 19 décembre
          2019), la même Cour a retenu la solution inverse&nbsp;: un
          intermédiaire qui se borne à rapprocher l'offre et la demande, sans
          être indispensable à l'existence de la prestation ni en maîtriser le
          prix, reste un service de la société de l'information.
        </p>
        <p>
          CoShift se range dans le second cas. Le conducteur existait avant la
          plateforme — il faisait déjà ce trajet pour son propre compte. Il
          choisit son itinéraire, son horaire, ses passagers, et il fixe
          lui-même la participation aux frais. La plateforme n'exerce aucune
          influence décisive sur la prestation.
        </p>

        <h3>Covoiturage et non transport rémunéré</h3>
        <p>
          Le montant demandé par un conducteur doit couvrir{" "}
          <strong>le partage des frais du trajet, jamais un bénéfice</strong>.
          Cette limite est la frontière entre le covoiturage et le transport
          rémunéré de personnes — lequel est soumis, en Belgique, à une
          autorisation délivrée par la Région compétente, et exige une assurance
          professionnelle spécifique.
        </p>
        <p>
          Un conducteur qui franchirait cette limite exercerait une activité de
          transport sans titre. Les{" "}
          <Link to="/cgu">conditions générales</Link> le rappellent et en tirent
          les conséquences.
        </p>

        <LegalSource>
          Depuis la sixième réforme de l'État (loi spéciale du 6 janvier 2014),
          le transport rémunéré de personnes est une compétence régionale&nbsp;:
          ordonnance bruxelloise du 9 juin 2022, décret flamand du 29 mars 2019
          sur le transport individuel rémunéré de personnes, décret wallon du
          18 octobre 2007 relatif aux services de taxis. Le covoiturage
          authentique, limité au partage des frais, échappe à ces régimes.
        </LegalSource>
      </LegalSection>

      <LegalSection id="responsabilite" titre="Responsabilité">
        <h3>Ce dont CoShift ne répond pas</h3>
        <p>
          Les trajets, les descriptions de véhicules, les messages et les
          photographies sont fournis par les utilisateurs. CoShift les stocke et
          les diffuse sans les avoir vérifiés au préalable, et bénéficie à ce
          titre de l'exonération prévue pour les services d'hébergement&nbsp;:
          la responsabilité n'est engagée qu'à défaut d'agir promptement après
          avoir eu connaissance effective d'un contenu illicite.
        </p>

        <LegalSource>
          Article 6 du règlement (UE) 2022/2065, qui a repris et remplacé
          l'article 14 de la directive 2000/31/CE. L'article 8 du même règlement
          exclut toute obligation générale de surveillance des contenus.
        </LegalSource>

        <h3>Pourquoi la modération ne fait pas perdre cette exonération</h3>
        <p>
          CoShift procède à des vérifications volontaires — contrôle de
          l'adresse électronique à l'inscription, détection des tentatives
          d'accès illégitimes, journalisation des événements de sécurité. Un
          raisonnement répandu voudrait que ces mesures, en démontrant un rôle
          actif, fassent tomber le régime protecteur.
        </p>
        <p>
          Ce raisonnement est écarté par l'<strong>article 7</strong> du
          règlement sur les services numériques, dit clause du bon
          samaritain&nbsp;: les enquêtes menées de sa propre initiative et de
          bonne foi par un fournisseur pour détecter et retirer des contenus
          illicites ne le privent pas des exonérations des articles 4 à 6.
          Contrôler n'est donc pas se dénoncer.
        </p>

        <h3>Ce dont CoShift ne peut pas répondre</h3>
        <p>
          La plateforme n'est partie à aucun trajet. Le déroulement du
          déplacement, la conduite, l'état du véhicule, la ponctualité et le
          comportement des personnes à bord relèvent des seuls participants.
          CoShift ne garantit ni la disponibilité continue du service, ni
          l'exactitude des informations déclarées par un utilisateur.
        </p>
        <div className="legal__limite">
          <p>
            <strong>Limite assumée.</strong> CoShift ne contrôle aujourd'hui ni
            la validité du permis de conduire, ni celle de l'assurance du
            véhicule, ni l'exactitude de la plaque déclarée. Rien dans
            l'application ne permet de le faire, et l'affirmer serait faux. Ce
            contrôle relève de la responsabilité du conducteur, à qui les
            conditions générales l'imposent expressément.
          </p>
        </div>
      </LegalSection>

      <LegalSection id="signalement" titre="Signaler un contenu illicite">
        <p>
          Toute personne peut signaler un contenu qu'elle estime illicite&nbsp;:
          annonce frauduleuse, usurpation d'identité, propos haineux, atteinte à
          un droit d'auteur, offre de transport rémunéré déguisée en
          covoiturage.
        </p>
        <p>
          Le signalement s'adresse à{" "}
          <a href={`mailto:${EDITEUR.signalement}`}>{EDITEUR.signalement}</a> et
          doit contenir&nbsp;:
        </p>
        <ul>
          <li>l'adresse exacte du contenu visé&nbsp;;</li>
          <li>
            l'explication des raisons pour lesquelles il est jugé illicite&nbsp;;
          </li>
          <li>le nom et l'adresse électronique de l'auteur du signalement&nbsp;;</li>
          <li>
            une déclaration de bonne foi quant à l'exactitude des informations
            fournies.
          </li>
        </ul>
        <p>
          Chaque signalement reçoit un accusé de réception. La décision prise est
          notifiée à son auteur ainsi qu'à la personne dont le contenu est visé,
          accompagnée de ses motifs et des voies de recours ouvertes.
        </p>

        <LegalSource>
          Articles 16 et 17 du règlement (UE) 2022/2065&nbsp;: mécanisme de
          notification et d'action, et obligation d'exposer les motifs de toute
          restriction. L'article 19 dispense les micro et petites entreprises des
          obligations des articles 20 à 28 — traitement interne des
          réclamations, règlement extrajudiciaire des litiges, signaleurs de
          confiance, rapports de transparence. Cette dispense ne s'étend pas aux
          articles 16 à 18, qui demeurent applicables quelle que soit la taille
          du fournisseur.
        </LegalSource>

        <div className="legal__limite">
          <p>
            <strong>Limite assumée.</strong> Le mécanisme décrit ci-dessus
            fonctionne aujourd'hui par courrier électronique. Un formulaire
            intégré à l'application, avec suivi de l'état de chaque signalement,
            reste à construire.
          </p>
        </div>
      </LegalSection>

      <LegalSection id="propriete" titre="Propriété intellectuelle">
        <p>
          La marque CoShift, le logotype, la charte graphique, les textes
          rédactionnels et le code source de l'application sont protégés par le
          livre XI du Code de droit économique. Leur reproduction, hors les
          exceptions légales, requiert une autorisation écrite.
        </p>
        <p>
          Les contenus déposés par les utilisateurs restent leur propriété. Les
          articles de presse repris dans la rubrique Actus demeurent la propriété
          de leurs éditeurs, et le régime applicable à leur reprise est détaillé
          dans les <Link to="/cgu">conditions générales</Link>.
        </p>
      </LegalSection>

      <LegalSection id="droit" titre="Droit applicable et juridictions">
        <p>
          Les présentes mentions et l'usage du site sont régis par le{" "}
          <strong>droit belge</strong>.
        </p>
        <p>
          Tout litige relève des juridictions de l'arrondissement judiciaire de
          Bruxelles. Cette attribution ne prive pas le consommateur du droit de
          saisir la juridiction de son domicile, garanti par l'article 18 du
          règlement (UE) 1215/2012 — une clause qui l'en priverait serait
          réputée non écrite.
        </p>
      </LegalSection>

      <LegalSection id="recours" titre="Autorités de contrôle">
        <div className="legal__table-wrap">
          <table className="legal__table">
            <thead>
              <tr>
                <th scope="col">Autorité</th>
                <th scope="col">Compétence</th>
                <th scope="col">Adresse</th>
              </tr>
            </thead>
            <tbody>
              {AUTORITES.map((a) => (
                <tr key={a.nom}>
                  <th scope="row">
                    <a href={a.lien} target="_blank" rel="noopener noreferrer">
                      {a.nom}
                    </a>
                  </th>
                  <td>{a.objet}</td>
                  <td>{a.adresse}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <p>
          Pour les réclamations relatives aux données personnelles, la{" "}
          <Link to="/confidentialite#droits">
            politique de confidentialité
          </Link>{" "}
          détaille la procédure et les droits ouverts.
        </p>

        <LegalSource>
          La plateforme européenne de règlement en ligne des litiges, instituée
          par le règlement (UE) 524/2013, a cessé ses activités le 20 juillet
          2025 en application du règlement (UE) 2024/3228. Les mentions légales
          qui y renvoient encore pointent vers un service fermé.
        </LegalSource>
      </LegalSection>
    </LegalLayout>
  );
}
