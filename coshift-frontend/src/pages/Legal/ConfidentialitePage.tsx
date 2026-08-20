import { Link } from "react-router-dom";
import LegalLayout, { LegalSection, LegalSource, type Section } from "./LegalLayout";
import {
  AUTORITES, EDITEUR, REGISTRE, TIERS, VERSION_CONFIDENTIALITE,
} from "../../config/legal";

const SECTIONS: Section[] = [
  { id: "essentiel", titre: "L'essentiel en cinq points" },
  { id: "responsable", titre: "Qui traite vos données" },
  { id: "registre", titre: "Ce qui est collecté, et pourquoi" },
  { id: "deplacements", titre: "Le cas des données de déplacement" },
  { id: "tiers", titre: "Qui d'autre y accède" },
  { id: "transferts", titre: "Transferts hors d'Europe" },
  { id: "durees", titre: "Combien de temps" },
  { id: "droits", titre: "Vos droits, et comment les exercer" },
  { id: "securite", titre: "Comment vos données sont protégées" },
  { id: "violation", titre: "En cas de violation de données" },
  { id: "dpo", titre: "Délégué à la protection des données" },
  { id: "mineurs", titre: "Mineurs" },
  { id: "reclamation", titre: "Introduire une réclamation" },
];

export default function ConfidentialitePage() {
  return (
    <LegalLayout
      titre="Politique de confidentialité"
      chapeau="Ce que CoShift sait de vous, pourquoi, pour combien de temps, et ce que vous pouvez en faire. Ce document décrit le fonctionnement réel de l'application — les manques y sont nommés plutôt que passés sous silence."
      description="Traitement des données personnelles par CoShift : registre des traitements, bases légales, durées de conservation, sous-traitants, transferts hors UE et exercice des droits RGPD."
      chemin="/confidentialite"
      version={VERSION_CONFIDENTIALITE}
      sections={SECTIONS}
    >
      <LegalSection id="essentiel" titre="L'essentiel en cinq points">
        <p>
          Le règlement impose une information « concise, transparente,
          compréhensible et aisément accessible ». Un document complet ne peut
          pas être court&nbsp;; il peut en revanche commencer par l'essentiel.
        </p>
        <ol>
          <li>
            <strong>Aucune donnée n'est vendue, louée ni cédée</strong> à des
            fins publicitaires ou commerciales. Le modèle de CoShift repose sur
            l'abonnement des organisations, pas sur l'audience.
          </li>
          <li>
            <strong>Aucun suivi publicitaire, aucun profilage.</strong> Il n'y a
            ni régie, ni pixel de mesure, ni identifiant partagé entre sites.
          </li>
          <li>
            <strong>Aucune géolocalisation continue.</strong> CoShift connaît les
            trajets que vous déclarez, pas vos déplacements.
          </li>
          <li>
            <strong>Votre numéro de téléphone n'est révélé</strong> qu'à la
            personne avec qui vous partagez une réservation confirmée.
          </li>
          <li>
            <strong>Vous pouvez récupérer et supprimer vos données</strong>{" "}
            depuis votre tableau de bord, sans passer par un formulaire ni
            attendre une réponse.
          </li>
        </ol>
      </LegalSection>

      <LegalSection id="responsable" titre="Qui traite vos données">
        <p>
          Le responsable du traitement est{" "}
          <strong>{EDITEUR.denomination}</strong>, {EDITEUR.siege}, joignable à
          l'adresse <a href={`mailto:${EDITEUR.viePrivee}`}>{EDITEUR.viePrivee}</a>.
          Voir les <Link to="/mentions-legales#editeur">mentions légales</Link>{" "}
          pour l'identification complète.
        </p>

        <h3>Et votre employeur ou votre école&nbsp;?</h3>
        <p>
          CoShift fonctionne par organisation. L'organisation qui souscrit
          détermine qui peut rejoindre son cercle&nbsp;; elle ne décide en
          revanche ni des finalités du traitement, ni des moyens techniques
          employés. Elle est donc <strong>destinataire</strong> de données
          agrégées, non responsable conjoint au sens de l'article 26.
        </p>
        <p>
          Ce que votre organisation voit&nbsp;: le nombre de membres inscrits, le
          nombre de trajets partagés, les statistiques d'usage. Ce qu'elle ne
          voit pas&nbsp;: qui covoiture avec qui, à quelle heure vous partez, et
          quels trajets vous avez refusés.
        </p>
        <div className="legal__constat">
          <p>
            <strong>Vérifiable.</strong> Aucun point d'entrée de l'API ne permet
            à une organisation de lire les trajets ou les réservations
            individuelles de ses membres. Cette limite est structurelle&nbsp;:
            elle tient à ce que le code n'expose pas, pas à un engagement de
            bonne conduite.
          </p>
        </div>
      </LegalSection>

      <LegalSection id="registre" titre="Ce qui est collecté, et pourquoi">
        <p>
          Le tableau qui suit est le{" "}
          <strong>registre des activités de traitement</strong> de CoShift. Il
          n'est pas résumé pour la circonstance&nbsp;: c'est le document que
          l'article 30 impose de tenir, publié tel quel.
        </p>

        {REGISTRE.map((t) => (
          <article className="legal__fiche" id={`traitement-${t.id}`} key={t.id}>
            <h3>{t.nom}</h3>
            <p className="legal__fiche-base">{t.base}</p>
            <dl>
              <dt>Finalité</dt>
              <dd>{t.finalite}</dd>

              <dt>Pourquoi cette base</dt>
              <dd>{t.justification}</dd>

              <dt>Données</dt>
              <dd>
                <ul>
                  {t.donnees.map((d) => (
                    <li key={d}>{d}</li>
                  ))}
                </ul>
              </dd>

              <dt>Conservation</dt>
              <dd>{t.duree}</dd>

              <dt>Destinataires</dt>
              <dd>{t.destinataires}</dd>
            </dl>
          </article>
        ))}

        <LegalSource>
          Articles 5.1.b (finalité déterminée), 5.1.c (minimisation), 6.1 (bases
          légales) et 30 (registre) du règlement (UE) 2016/679. En droit belge,
          la loi du 30 juillet 2018 relative à la protection des personnes
          physiques à l'égard des traitements de données à caractère personnel
          complète le règlement et a abrogé la loi vie privée du 8 décembre 1992.
        </LegalSource>

        <h3>Une donnée souvent oubliée&nbsp;: la plaque d'immatriculation</h3>
        <p>
          Une plaque d'immatriculation est une{" "}
          <strong>donnée à caractère personnel</strong>. Elle ne contient aucun
          nom, mais elle permet d'identifier indirectement le titulaire du
          véhicule par consultation du répertoire de la Direction pour
          l'immatriculation des véhicules. Le considérant 26 du règlement vise
          exactement cette situation&nbsp;: est identifiable la personne qui peut
          l'être par des moyens raisonnablement susceptibles d'être utilisés.
        </p>
        <p>
          En conséquence, la plaque déclarée n'est affichée à personne d'autre
          qu'à son propriétaire. Elle ne figure ni dans les résultats de
          recherche, ni sur la fiche d'un trajet, ni dans les données ouvertes.
        </p>
      </LegalSection>

      <LegalSection id="deplacements" titre="Le cas des données de déplacement">
        <p>
          Les trajets méritent un traitement à part. Pris isolément, un trajet
          Namur–Bruxelles un mardi matin n'apprend rien. Répété quarante fois,
          il désigne un domicile, un employeur, des horaires de travail, des
          jours de télétravail et des absences.
        </p>
        <p>
          Cette accumulation est ce qui rend les données de mobilité sensibles —
          non pas au sens de l'article 9, qui vise des catégories limitativement
          énumérées, mais par ce qu'elles permettent d'inférer. Le nier serait
          confortable et faux.
        </p>

        <h3>Ce que CoShift en fait, et ne fait pas</h3>
        <ul>
          <li>
            Les trajets ne sont <strong>jamais recoupés dans le temps</strong>{" "}
            pour construire un profil de déplacement.
          </li>
          <li>
            Aucun trajet n'est visible en dehors du cercle de l'organisation.
          </li>
          <li>
            Les données ouvertes publiées par CoShift sont agrégées et supprimées
            sous un seuil de cinq occurrences&nbsp;: un couple origine-destination
            rare ne peut pas y désigner une personne.
          </li>
          <li>
            Aucune position n'est relevée pendant le trajet. L'application ne
            demande jamais l'accès à la géolocalisation du terminal.
          </li>
        </ul>

        <LegalSource>
          L'article 35 du règlement impose une analyse d'impact relative à la
          protection des données lorsqu'un traitement est susceptible d'engendrer
          un risque élevé, notamment en cas d'évaluation systématique ou de
          suivi systématique à grande échelle. La liste des traitements soumis à
          analyse adoptée par l'Autorité de protection des données le 16 janvier
          2019 y range les traitements de données de localisation collectées à
          grande échelle.
        </LegalSource>

        <div className="legal__limite">
          <p>
            <strong>Limite assumée.</strong> Aucune analyse d'impact formelle n'a
            été conduite à ce jour. À l'échelle actuelle du projet — un jeu de
            démonstration, aucun utilisateur réel — le seuil du « grand nombre de
            personnes » n'est pas atteint. Il le serait dès le premier
            déploiement en organisation&nbsp;: l'analyse est un préalable à la
            mise en production, pas une amélioration ultérieure.
          </p>
        </div>
      </LegalSection>

      <LegalSection id="tiers" titre="Qui d'autre y accède">
        <p>
          Quatre tiers interviennent. Le tableau distingue ceux qui sont
          nécessaires au service de ceux qui ne le sont pas — cette distinction
          commande le régime du consentement, détaillé dans la{" "}
          <Link to="/cookies">politique de cookies</Link>.
        </p>

        <div className="legal__table-wrap">
          <table className="legal__table">
            <thead>
              <tr>
                <th scope="col">Tiers</th>
                <th scope="col">Rôle</th>
                <th scope="col">Ce qui lui parvient</th>
                <th scope="col">Pays</th>
                <th scope="col">Consentement</th>
              </tr>
            </thead>
            <tbody>
              {TIERS.map((t) => (
                <tr key={t.nom}>
                  <th scope="row">{t.nom}</th>
                  <td>{t.role}</td>
                  <td>{t.donnees}</td>
                  <td>{t.pays}</td>
                  <td>{t.necessaire ? "Non requis" : "Requis avant chargement"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="legal__constat">
          <p>
            <strong>Vérifiable.</strong> Ni le script de connexion Google ni le
            fond cartographique Mapbox ne sont chargés tant que le bandeau n'a
            pas reçu de réponse favorable. L'onglet réseau du navigateur le
            montre&nbsp;: sur une première visite sans consentement, aucune
            requête ne part vers <code>accounts.google.com</code> ni vers{" "}
            <code>api.mapbox.com</code>.
          </p>
        </div>
      </LegalSection>

      <LegalSection id="transferts" titre="Transferts hors d'Europe">
        <p>
          Deux tiers sont établis aux États-Unis&nbsp;: Google LLC et Mapbox Inc.
          Un transfert hors de l'Espace économique européen n'est licite que s'il
          repose sur l'un des instruments du chapitre V du règlement.
        </p>

        <dl>
          <dt>Google LLC</dt>
          <dd>
            Décision d'adéquation de la Commission du 10 juillet 2023 relative au
            cadre de protection des données UE–États-Unis. Google LLC figure
            parmi les organisations certifiées au titre de ce cadre&nbsp;; le
            transfert ne requiert donc pas de garantie additionnelle.
          </dd>

          <dt>Mapbox Inc.</dt>
          <dd>
            Clauses contractuelles types de la décision d'exécution (UE) 2021/914.
            La certification de Mapbox au cadre UE–États-Unis n'a pas été
            vérifiée&nbsp;: les clauses types offrent une base autonome, valable
            indépendamment de la décision d'adéquation.
          </dd>
        </dl>

        <LegalSource>
          Dans l'arrêt <em>Schrems II</em> (C-311/18, 16 juillet 2020), la Cour
          de justice a invalidé le Privacy Shield et rappelé que les clauses
          contractuelles types n'exonèrent pas l'exportateur d'évaluer le droit
          du pays destinataire. La décision d'adéquation de 2023 fait l'objet de
          recours pendants&nbsp;: fonder une architecture sur sa seule pérennité
          serait imprudent.
        </LegalSource>

        <p>
          C'est la raison pour laquelle ces deux services sont{" "}
          <strong>facultatifs et remplaçables</strong>. La connexion par mot de
          passe fonctionne sans Google. L'invalidation éventuelle de la décision
          d'adéquation coûterait une carte animée et un bouton de connexion, pas
          le service.
        </p>

        <h3>Un choix qui n'a pas été fait</h3>
        <p>
          Les polices de caractères du site — Inter et Outfit — sont{" "}
          <strong>servies depuis le serveur de CoShift</strong>, non depuis Google
          Fonts. Charger une police depuis un serveur tiers transmet l'adresse IP
          de chaque visiteur à ce tiers, sans consentement et sans nécessité.
          Cette pratique a valu une condamnation à un exploitant de site devant
          le tribunal régional de Munich le 20 janvier 2022.
        </p>
      </LegalSection>

      <LegalSection id="durees" titre="Combien de temps">
        <p>
          Le principe de limitation de la conservation interdit de garder des
          données au-delà de ce qu'exige la finalité. Une durée « aussi longtemps
          que nécessaire » n'est pas une durée.
        </p>

        <div className="legal__table-wrap">
          <table className="legal__table">
            <thead>
              <tr>
                <th scope="col">Donnée</th>
                <th scope="col">Durée</th>
                <th scope="col">Ce qui la justifie</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th scope="row">Compte actif</th>
                <td>Durée d'utilisation du service</td>
                <td>Exécution du contrat</td>
              </tr>
              <tr>
                <th scope="row">Compte supprimé</th>
                <td>Immédiat, sans délai de grâce</td>
                <td>
                  Un délai de rétractation supposerait de conserver les données
                  pendant ce temps, ce qui contredirait la demande. La
                  confirmation par saisie de l'adresse tient lieu de garde-fou
                </td>
              </tr>
              <tr>
                <th scope="row">Compte jamais vérifié</th>
                <td>30 jours après l'inscription</td>
                <td>
                  Une inscription non confirmée n'est pas un compte&nbsp;: la
                  conserver reviendrait à garder l'adresse d'une personne qui n'a
                  rien demandé
                </td>
              </tr>
              <tr>
                <th scope="row">Trajets et réservations</th>
                <td>24 mois après la date du trajet, puis anonymisation</td>
                <td>
                  Historique utile au conducteur et au passager, délai de
                  prescription des actions contractuelles courantes
                </td>
              </tr>
              <tr>
                <th scope="row">Codes de vérification</th>
                <td>1 heure, effacés dès usage</td>
                <td>Un code qui survit à son usage est un mot de passe secondaire</td>
              </tr>
              <tr>
                <th scope="row">Journal de sécurité</th>
                <td>12 mois</td>
                <td>
                  Durée nécessaire pour établir une attaque étalée dans le temps
                </td>
              </tr>
              <tr>
                <th scope="row">Trace du consentement</th>
                <td>6 mois, puis le choix est redemandé</td>
                <td>
                  Preuve du consentement exigée par l'article 7.1, sans le rendre
                  perpétuel
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <p>
          L'anonymisation d'un trajet consiste à rompre définitivement le lien
          vers les personnes tout en conservant l'origine, la destination et le
          mois. Le résultat ne se rapporte plus à personne&nbsp;: il alimente les
          statistiques et les données ouvertes, hors du champ du règlement.
        </p>
      </LegalSection>

      <LegalSection id="droits" titre="Vos droits, et comment les exercer">
        <p>
          Les articles 15 à 22 ouvrent sept droits. Le tableau indique, pour
          chacun, <strong>ce qui fonctionne aujourd'hui</strong> — la distinction
          entre le droit reconnu et le moyen de l'exercer est ce qui sépare une
          politique sincère d'une déclaration d'intention.
        </p>

        <div className="legal__table-wrap">
          <table className="legal__table">
            <thead>
              <tr>
                <th scope="col">Droit</th>
                <th scope="col">Article</th>
                <th scope="col">Comment l'exercer</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th scope="row">Accès</th>
                <td>15</td>
                <td>
                  Tableau de bord, bouton <em>Exporter mes données</em>. Réponse
                  immédiate, sans intervention humaine.
                </td>
              </tr>
              <tr>
                <th scope="row">Rectification</th>
                <td>16</td>
                <td>
                  Tableau de bord&nbsp;: nom, prénom, téléphone, photographie et
                  adresse électronique sont modifiables directement.
                </td>
              </tr>
              <tr>
                <th scope="row">Effacement</th>
                <td>17</td>
                <td>
                  Tableau de bord, bouton <em>Supprimer mon compte</em>, avec
                  confirmation par saisie de l'adresse.
                </td>
              </tr>
              <tr>
                <th scope="row">Limitation</th>
                <td>18</td>
                <td>
                  Sur demande à{" "}
                  <a href={`mailto:${EDITEUR.viePrivee}`}>{EDITEUR.viePrivee}</a>.
                  Traitement manuel.
                </td>
              </tr>
              <tr>
                <th scope="row">Portabilité</th>
                <td>20</td>
                <td>
                  Le même export, au format JSON&nbsp;: structuré, couramment
                  utilisé et lisible par machine, comme l'exige le texte.
                </td>
              </tr>
              <tr>
                <th scope="row">Opposition</th>
                <td>21</td>
                <td>
                  Applicable au seul journal de sécurité, fondé sur l'intérêt
                  légitime. Sur demande, traitée au cas par cas.
                </td>
              </tr>
              <tr>
                <th scope="row">Décision automatisée</th>
                <td>22</td>
                <td>
                  Sans objet&nbsp;: aucune décision produisant des effets
                  juridiques n'est prise automatiquement. Un conducteur accepte
                  ou refuse lui-même chaque demande.
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <h3>Ce que l'effacement efface, et ce qu'il ne peut pas effacer</h3>
        <p>
          Supprimer un compte n'efface pas les trajets passés auxquels d'autres
          personnes ont participé. Une réservation confirmée engage deux
          personnes&nbsp;: effacer la trace d'un covoiturage priverait l'autre
          participant de son propre historique.
        </p>
        <p>La suppression procède donc ainsi&nbsp;:</p>
        <ul>
          <li>
            Nom, prénom, adresse électronique, téléphone, photographie, mot de
            passe et véhicules sont <strong>effacés</strong>&nbsp;;
          </li>
          <li>
            Les trajets et réservations passés sont{" "}
            <strong>anonymisés</strong>&nbsp;: ils subsistent, rattachés à un
            participant supprimé, sans aucune donnée identifiante&nbsp;;
          </li>
          <li>
            Les trajets futurs et les réservations en attente sont{" "}
            <strong>annulés</strong>, afin que personne ne se présente à un
            rendez-vous qui n'aura pas lieu.
          </li>
        </ul>

        <LegalSource>
          L'article 17.3 énumère les cas où le droit à l'effacement cède. Le
          maintien anonymisé d'un historique partagé ne relève d'aucune
          exception&nbsp;: il n'en a pas besoin, puisqu'une donnée anonymisée
          n'est plus une donnée personnelle et sort du champ du règlement
          (considérant 26).
        </LegalSource>

        <h3>Délai de réponse</h3>
        <p>
          Les demandes traitées par l'application sont satisfaites
          immédiatement. Les autres reçoivent une réponse dans le mois, délai
          prolongeable de deux mois en cas de complexité, avec information sur
          les motifs de la prolongation. La réponse est gratuite&nbsp;; une
          demande manifestement infondée ou excessive peut donner lieu à un
          refus motivé.
        </p>
      </LegalSection>

      <LegalSection id="securite" titre="Comment vos données sont protégées">
        <p>
          L'article 32 impose des mesures appropriées au risque. Voici celles qui
          sont en place, formulées de façon vérifiable plutôt que rassurante.
        </p>
        <ul>
          <li>
            Les mots de passe sont conservés sous forme d'empreinte{" "}
            <strong>BCrypt</strong>. Ils ne sont pas chiffrés, ils ne sont pas
            réversibles&nbsp;: même l'administrateur ne peut pas les lire.
          </li>
          <li>
            Cinq échecs d'authentification bloquent la combinaison adresse et
            adresse IP pendant quinze minutes.
          </li>
          <li>
            Le service ne révèle jamais si une adresse est inscrite&nbsp;: les
            réponses sont identiques pour un compte existant et un compte
            inconnu.
          </li>
          <li>
            Neuf natures d'événements de sécurité sont journalisées dans un
            fichier distinct du journal applicatif. Aucun mot de passe, jeton ni
            code n'y figure, même tronqué.
          </li>
          <li>
            Chaque opération modifiante vérifie la propriété de la ressource
            avant d'agir&nbsp;: un identifiant deviné ne suffit pas à modifier le
            trajet d'autrui.
          </li>
          <li>
            Les en-têtes <code>X-Content-Type-Options</code>,{" "}
            <code>X-Frame-Options</code>, <code>Content-Security-Policy</code> et{" "}
            <code>Referrer-Policy</code> sont émis sur toutes les réponses.
          </li>
        </ul>

        <div className="legal__limite">
          <p>
            <strong>Limites assumées.</strong> Le jeton d'authentification ne
            peut être ni rafraîchi ni révoqué avant son expiration&nbsp;;
            supprimer un compte n'invalide donc pas immédiatement un jeton déjà
            émis. Le mot de passe n'est soumis à aucune exigence de robustesse.
            Le blocage des tentatives est tenu en mémoire du serveur et ne
            survivrait pas à un redémarrage. Aucun second facteur
            d'authentification n'est proposé. Ces quatre points sont documentés
            comme faiblesses connues du projet.
          </p>
        </div>
      </LegalSection>

      <LegalSection id="violation" titre="En cas de violation de données">
        <p>
          Une violation de données est notifiée à l'Autorité de protection des
          données <strong>dans les 72 heures</strong> suivant sa découverte,
          sauf si elle est peu susceptible d'engendrer un risque pour les
          personnes.
        </p>
        <p>
          Lorsque le risque est élevé, les personnes concernées en sont averties
          directement, en des termes clairs, avec la nature de la violation, ses
          conséquences probables et les mesures à prendre — changer un mot de
          passe réutilisé ailleurs, par exemple.
        </p>

        <LegalSource>
          Articles 33 et 34 du règlement (UE) 2016/679. Le délai de 72 heures
          court à compter de la <em>prise de connaissance</em>, non de la
          survenance&nbsp;: c'est la capacité de détection qui conditionne le
          respect de l'obligation, ce qui donne au journal de sécurité une portée
          juridique et pas seulement technique.
        </LegalSource>
      </LegalSection>

      <LegalSection id="dpo" titre="Délégué à la protection des données">
        <p>
          CoShift n'a pas désigné de délégué à la protection des données. Cette
          désignation n'est obligatoire que dans trois cas&nbsp;: autorité
          publique, suivi régulier et systématique à grande échelle, ou
          traitement à grande échelle de données sensibles au sens des articles 9
          et 10. Aucun n'est rencontré à l'échelle actuelle.
        </p>
        <p>
          Cette analyse est datée. Un déploiement auprès de plusieurs
          organisations, avec le suivi de déplacements qu'il implique, appellerait
          un réexamen sérieux du deuxième cas. Les questions relatives aux données
          personnelles sont, en attendant, traitées à l'adresse{" "}
          <a href={`mailto:${EDITEUR.viePrivee}`}>{EDITEUR.viePrivee}</a>.
        </p>

        <LegalSource>
          Article 37 du règlement, éclairé par les lignes directrices du groupe
          de travail « Article 29 » WP243, adoptées le 13 décembre 2016 et
          révisées le 5 avril 2017, endossées par le Comité européen de la
          protection des données.
        </LegalSource>
      </LegalSection>

      <LegalSection id="mineurs" titre="Mineurs">
        <p>
          CoShift s'adresse aux membres d'organisations professionnelles ou
          d'établissements d'enseignement supérieur. Le service n'est pas destiné
          aux personnes de moins de seize ans, seuil retenu par la Belgique pour
          le consentement d'un enfant aux services de la société de
          l'information.
        </p>
        <p>
          Aucune vérification d'âge n'est opérée&nbsp;: elle supposerait de
          collecter une date de naissance ou une pièce d'identité, c'est-à-dire
          davantage de données pour en protéger moins.
        </p>

        <LegalSource>
          Article 8 du règlement, qui fixe le seuil à seize ans et permet aux
          États membres de descendre jusqu'à treize. L'article 7 de la loi belge
          du 30 juillet 2018 a maintenu le seuil de treize ans pour l'offre
          directe de services de la société de l'information&nbsp;; CoShift
          retient volontairement un seuil plus élevé, cohérent avec son public.
        </LegalSource>
      </LegalSection>

      <LegalSection id="reclamation" titre="Introduire une réclamation">
        <p>
          Toute personne qui estime ses droits méconnus peut saisir l'autorité de
          contrôle, sans préjudice d'un recours juridictionnel.
        </p>
        <div className="legal__fiche">
          <h3>{AUTORITES[0].nom}</h3>
          <dl>
            <dt>Adresse</dt>
            <dd>{AUTORITES[0].adresse}</dd>
            <dt>Site</dt>
            <dd>
              <a href={AUTORITES[0].lien} target="_blank" rel="noopener noreferrer">
                {AUTORITES[0].lien}
              </a>
            </dd>
          </dl>
        </div>
        <p>
          Nous vous serions reconnaissants de nous écrire d'abord à{" "}
          <a href={`mailto:${EDITEUR.viePrivee}`}>{EDITEUR.viePrivee}</a> — non
          pour retarder votre démarche, qui n'est soumise à aucune condition
          préalable, mais parce qu'une erreur se corrige plus vite qu'elle ne
          s'instruit.
        </p>

        <h3>Modifications de cette politique</h3>
        <p>
          Toute modification substantielle est portée à la connaissance des
          personnes inscrites avant son entrée en vigueur. La version et la date
          figurent en tête de cette page&nbsp;; les versions antérieures sont
          conservées et communiquées sur demande.
        </p>
      </LegalSection>
    </LegalLayout>
  );
}
