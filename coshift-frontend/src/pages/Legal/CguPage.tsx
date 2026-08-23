import { Link } from "react-router-dom";
import LegalLayout, { LegalSection, LegalSource, type Section } from "./LegalLayout";
import { EDITEUR, VERSION_CGU } from "../../config/legal";

const SECTIONS: Section[] = [
  { id: "objet", titre: "Objet et acceptation" },
  { id: "acces", titre: "Qui peut s'inscrire" },
  { id: "compte", titre: "Votre compte" },
  { id: "frais", titre: "La règle du partage des frais" },
  { id: "conducteur", titre: "Engagements du conducteur" },
  { id: "passager", titre: "Engagements du passager" },
  { id: "contrat", titre: "Entre qui se noue le covoiturage" },
  { id: "assurance", titre: "Assurance" },
  { id: "interdits", titre: "Comportements interdits" },
  { id: "contenus", titre: "Vos contenus" },
  { id: "presse", titre: "La rubrique Actus et le droit de la presse" },
  { id: "propriete", titre: "Propriété intellectuelle de CoShift" },
  { id: "suspension", titre: "Suspension et résiliation" },
  { id: "paiement", titre: "Paiement et rétractation" },
  { id: "modification", titre: "Modification des conditions" },
];

export default function CguPage() {
  return (
    <LegalLayout
      titre="Conditions générales d'utilisation"
      chapeau="Ce à quoi chacun s'engage en utilisant CoShift. Le point central tient en une phrase : le prix d'une place couvre le partage des frais du trajet, jamais un bénéfice — c'est cette limite qui sépare le covoiturage du transport rémunéré."
      description="Conditions générales d'utilisation de CoShift : accès au service, règle du partage des frais, engagements du conducteur et du passager, assurance, contenus et propriété intellectuelle."
      chemin="/cgu"
      version={VERSION_CGU}
      sections={SECTIONS}
    >
      <LegalSection id="objet" titre="Objet et acceptation">
        <p>
          Les présentes conditions régissent l'accès à CoShift et son usage.
          Elles forment un contrat entre {EDITEUR.denomination} et chaque
          personne inscrite.
        </p>
        <p>
          L'acceptation intervient à l'inscription, par une case à cocher
          distincte, non pré-cochée. La date de l'acceptation et la version
          alors en vigueur sont conservées&nbsp;: sans cette trace, l'accord ne
          serait pas démontrable.
        </p>

        <LegalSource>
          Article VI.83, 21° du Code de droit économique&nbsp;: est abusive la
          clause qui constate de manière irréfragable l'adhésion du consommateur
          à des conditions dont il n'a pas eu connaissance avant la conclusion du
          contrat. Une acceptation par simple navigation ne vaudrait donc rien.
        </LegalSource>
      </LegalSection>

      <LegalSection id="acces" titre="Qui peut s'inscrire">
        <p>
          CoShift n'est pas un service ouvert. L'inscription suppose une adresse
          électronique professionnelle ou académique rattachée à une organisation
          partenaire, et sa vérification par un code à six chiffres.
        </p>
        <p>
          Ce cercle fermé n'est pas une contrainte administrative&nbsp;: c'est ce
          qui fonde la confiance. Monter dans la voiture d'un collègue identifié
          n'est pas monter dans celle d'un inconnu.
        </p>
        <p>
          Le service s'adresse aux personnes majeures, ou à tout le moins âgées
          de seize ans révolus. Le titulaire d'un compte garantit l'exactitude
          des informations qu'il déclare.
        </p>
      </LegalSection>

      <LegalSection id="compte" titre="Votre compte">
        <ul>
          <li>
            Un compte est personnel. Le prêter ou le céder est interdit.
          </li>
          <li>
            Le mot de passe est confidentiel. Toute opération effectuée avec lui
            est réputée émaner de son titulaire, jusqu'à signalement contraire à{" "}
            <a href={`mailto:${EDITEUR.contact}`}>{EDITEUR.contact}</a>.
          </li>
          <li>
            Modifier son adresse électronique replace le compte en attente de
            vérification&nbsp;: la nouvelle adresse doit être confirmée avant que
            le compte redevienne utilisable.
          </li>
          <li>
            Le compte peut être supprimé à tout moment depuis le tableau de bord.
            Les conséquences de la suppression sont détaillées dans la{" "}
            <Link to="/confidentialite#droits">politique de confidentialité</Link>.
          </li>
        </ul>
      </LegalSection>

      <LegalSection id="frais" titre="La règle du partage des frais">
        <p>
          C'est la clause la plus importante de ce document.
        </p>
        <p>
          Le montant demandé par un conducteur pour une place doit{" "}
          <strong>
            couvrir une part des frais réels du trajet, et rien de plus
          </strong>
          . Sont des frais réels&nbsp;: le carburant ou l'électricité, l'usure,
          les péages, le stationnement, une part raisonnable de l'assurance et de
          l'entretien.
        </p>
        <p>
          Le conducteur doit <strong>supporter sa propre part</strong>. Une place
          vendue à un tarif tel que le trajet ne lui coûte plus rien n'est plus
          un partage&nbsp;: c'est une prestation de transport.
        </p>

        <h3>Pourquoi cette limite n'est pas négociable</h3>
        <p>
          Franchir cette ligne fait basculer le conducteur dans le{" "}
          <strong>transport rémunéré de personnes</strong>, avec quatre
          conséquences immédiates&nbsp;:
        </p>
        <ol>
          <li>
            <strong>Autorisation.</strong> Le transport rémunéré est soumis à
            une licence délivrée par la Région compétente. L'exercer sans titre
            est une infraction.
          </li>
          <li>
            <strong>Assurance.</strong> Une police d'assurance automobile à usage
            privé ne couvre pas une activité de transport professionnel. Le
            refus de garantie se découvre après l'accident.
          </li>
          <li>
            <strong>Fiscalité.</strong> Le bénéfice réalisé devient un revenu
            imposable, comme profit d'une occupation lucrative ou comme revenu
            divers selon la régularité de l'activité.
          </li>
          <li>
            <strong>Statut.</strong> Une activité régulière et lucrative appelle
            une inscription à la Banque-Carrefour des Entreprises et un statut
            social.
          </li>
        </ol>

        <LegalSource>
          Le partage de frais sans bénéfice ne génère aucun revenu imposable, le
          conducteur ne s'enrichissant pas. À titre de repère, l'administration
          publie chaque année une indemnité kilométrique forfaitaire censée
          couvrir l'ensemble des frais d'un véhicule personnel — de l'ordre de
          0,42&nbsp;€ par kilomètre ces dernières années. Une participation
          totale, tous passagers confondus, qui dépasserait ce repère appliqué à
          la distance parcourue signale un bénéfice. Le montant exact doit être
          vérifié auprès de la publication en vigueur, révisée annuellement.
        </LegalSource>

        <p>
          CoShift ne perçoit aucune commission sur ces montants et n'intervient
          pas dans leur règlement, qui se fait directement entre le conducteur et
          le passager.
        </p>

        <div className="legal__limite">
          <p>
            <strong>Limite assumée.</strong> L'application n'impose aujourd'hui
            aucun plafond automatique au montant demandé par place. Un contrôle
            calculé sur la distance et le barème en vigueur est identifié comme
            l'évolution prioritaire de cette clause&nbsp;: une règle que rien ne
            vérifie repose entièrement sur la bonne foi.
          </p>
        </div>
      </LegalSection>

      <LegalSection id="conducteur" titre="Engagements du conducteur">
        <p>En publiant un trajet, le conducteur déclare et garantit&nbsp;:</p>
        <ul>
          <li>
            être titulaire d'un <strong>permis de conduire valide</strong> pour
            la catégorie du véhicule, non suspendu ni retiré&nbsp;;
          </li>
          <li>
            que le véhicule est <strong>couvert par une assurance</strong> en
            responsabilité civile automobile en cours de validité&nbsp;;
          </li>
          <li>
            que le véhicule est en <strong>état de circuler</strong> et satisfait
            au contrôle technique lorsque celui-ci est requis&nbsp;;
          </li>
          <li>
            être le propriétaire du véhicule ou disposer de l'autorisation de son
            détenteur pour l'usage envisagé&nbsp;;
          </li>
          <li>
            que le nombre de places proposées n'excède pas le nombre de places
            homologuées et effectivement équipées d'une ceinture&nbsp;;
          </li>
          <li>
            qu'il conduira en état de le faire, à jeun, et dans le respect du
            code de la route.
          </li>
        </ul>
        <p>
          Le conducteur prévient sans délai les passagers de toute annulation ou
          de tout retard significatif.
        </p>
        <div className="legal__limite">
          <p>
            <strong>Limite assumée.</strong> CoShift ne vérifie aucun de ces
            points. Aucun document n'est demandé, aucune plaque n'est confrontée
            à un registre. Ces déclarations engagent leur auteur&nbsp;; elles ne
            constituent pas une garantie de la plateforme, et le prétendre serait
            trompeur.
          </p>
        </div>
      </LegalSection>

      <LegalSection id="passager" titre="Engagements du passager">
        <ul>
          <li>
            Se présenter au point et à l'heure convenus, ou prévenir en cas
            d'empêchement.
          </li>
          <li>
            Verser la participation convenue, dans les conditions convenues.
          </li>
          <li>
            Respecter le véhicule, ses occupants et les préférences déclarées par
            le conducteur — bagages, animaux, musique, conversation.
          </li>
          <li>
            Porter la ceinture de sécurité et respecter les consignes du
            conducteur relatives à la sécurité.
          </li>
        </ul>
        <p>
          Une demande de place n'est pas une réservation ferme&nbsp;: elle ne
          vaut que lorsque le conducteur l'a acceptée. Un refus est motivé, et
          le motif est communiqué au demandeur.
        </p>
      </LegalSection>

      <LegalSection id="contrat" titre="Entre qui se noue le covoiturage">
        <p>
          Lorsqu'un conducteur accepte une demande, un accord se forme{" "}
          <strong>entre lui et le passager</strong>. CoShift n'y est pas partie.
        </p>
        <p>
          La plateforme fournit l'outil de mise en relation&nbsp;; elle ne
          transporte personne, ne garantit ni la réalisation du trajet, ni la
          ponctualité, ni le comportement des participants. Les litiges nés d'un
          covoiturage se règlent entre ses participants — ce qui n'exclut pas
          l'intervention de CoShift au titre de la modération lorsqu'un
          comportement est signalé.
        </p>

        <LegalSource>
          Cette qualification découle de celle du service lui-même, exposée dans
          les <Link to="/mentions-legales#qualification">mentions légales</Link>{" "}
          à la lumière des arrêts <em>Elite Taxi</em> (C-434/15) et{" "}
          <em>Airbnb Ireland</em> (C-390/18) de la Cour de justice de l'Union
          européenne.
        </LegalSource>
      </LegalSection>

      <LegalSection id="assurance" titre="Assurance">
        <p>
          En Belgique, l'assurance en responsabilité civile automobile est
          obligatoire et couvre les personnes transportées, y compris à titre
          gratuit. Un covoiturage limité au partage des frais reste dans le cadre
          de l'usage privé du véhicule&nbsp;: les passagers sont couverts.
        </p>
        <p>
          La loi va plus loin pour les victimes autres que le conducteur&nbsp;:
          elles bénéficient d'une indemnisation automatique des dommages
          corporels, indépendamment de toute faute. Un passager blessé n'a donc
          pas à établir la responsabilité de qui que ce soit pour être indemnisé.
        </p>

        <LegalSource>
          Loi du 21 novembre 1989 relative à l'assurance obligatoire de la
          responsabilité en matière de véhicules automoteurs, et son article
          29<em>bis</em> sur l'indemnisation automatique des victimes autres que
          le conducteur.
        </LegalSource>

        <p>
          Il appartient à chaque conducteur de vérifier auprès de son assureur
          que l'usage qu'il fait de son véhicule reste couvert. Une pratique
          dépassant le partage des frais peut entraîner un refus de garantie.
        </p>
      </LegalSection>

      <LegalSection id="interdits" titre="Comportements interdits">
        <p>Sont notamment interdits&nbsp;:</p>
        <ul>
          <li>
            proposer un trajet dans un but lucratif, sous quelque présentation
            que ce soit&nbsp;;
          </li>
          <li>
            usurper l'identité d'un tiers, ou déclarer un véhicule dont on ne
            dispose pas&nbsp;;
          </li>
          <li>
            publier des propos discriminatoires, haineux, menaçants ou
            harcelants&nbsp;;
          </li>
          <li>
            utiliser le service à des fins de démarchage, de publicité ou de
            collecte d'adresses&nbsp;;
          </li>
          <li>
            extraire systématiquement le contenu du site, par quelque procédé
            automatisé que ce soit&nbsp;;
          </li>
          <li>
            tenter d'accéder à des données ou fonctions non destinées à son
            compte, ou de contourner les mesures de sécurité.
          </li>
        </ul>
        <p>
          Ces comportements peuvent être signalés selon la procédure décrite dans
          les <Link to="/mentions-legales#signalement">mentions légales</Link>.
        </p>
      </LegalSection>

      <LegalSection id="contenus" titre="Vos contenus">
        <p>
          Les descriptions de trajets, photographies de profil et de véhicule et
          messages restent la propriété de leur auteur.
        </p>
        <p>
          En les publiant, l'auteur concède à CoShift une licence{" "}
          <strong>
            non exclusive, gratuite, limitée à l'exploitation du service et à sa
            durée
          </strong>
          , pour les reproduire et les afficher aux autres membres de son
          organisation. Cette licence s'éteint au retrait du contenu ou à la
          suppression du compte.
        </p>
        <p>
          Elle ne couvre ni la cession à des tiers, ni la publicité, ni l'usage
          promotionnel hors du service. Une licence perpétuelle, irrévocable et
          mondiale, telle qu'on en rencontre couramment, serait sans rapport avec
          ce que CoShift a besoin de faire des contenus.
        </p>
        <p>
          L'auteur garantit détenir les droits sur ce qu'il publie —
          particulièrement pour une photographie où figurent d'autres personnes,
          dont l'image est protégée indépendamment du droit d'auteur.
        </p>
      </LegalSection>

      <LegalSection id="presse" titre="La rubrique Actus et le droit de la presse">
        <p>
          La rubrique Actus est alimentée automatiquement à partir de deux
          agrégateurs de presse. Pour chaque article, CoShift conserve et affiche
          le <strong>titre</strong>, un <strong>chapô de quelques lignes</strong>,
          le <strong>nom de la source</strong>, la <strong>date</strong> et un{" "}
          <strong>lien vers l'article d'origine</strong>. Le texte intégral n'est
          jamais repris.
        </p>

        <h3>Le régime applicable</h3>
        <p>
          Cette pratique relève du droit voisin reconnu aux éditeurs de presse
          pour l'utilisation en ligne de leurs publications. Ce droit comporte
          une exception expresse pour les{" "}
          <strong>
            actes d'hyperlien et l'utilisation de mots isolés ou de très courts
            extraits
          </strong>
          .
        </p>
        <p>
          La position de CoShift est que le titre accompagné d'un chapô entre
          dans cette exception. Cette position mérite d'être exposée plutôt
          qu'affirmée&nbsp;: elle n'est pas certaine.
        </p>
        <p>
          La Cour de justice a jugé, à propos d'extraits de onze mots, qu'un
          fragment aussi court peut être protégé s'il traduit la création
          intellectuelle propre de son auteur. Un titre de presse travaillé peut
          donc être une œuvre à part entière. La frontière du « très court
          extrait » n'a jamais été chiffrée par le législateur, et la prudence
          commande de la traiter comme incertaine plutôt que comme acquise.
        </p>

        <LegalSource>
          Article 15 de la directive (UE) 2019/790 sur le droit d'auteur dans le
          marché unique numérique, transposé en droit belge par la loi du 19 juin
          2022 aux articles XI.216/1 et suivants du Code de droit économique.
          Sur la protection des courts extraits&nbsp;: Cour de justice de l'Union
          européenne, <em>Infopaq International</em>, C-5/08, 16 juillet 2009.
        </LegalSource>

        <h3>Les illustrations</h3>
        <p>
          Les vignettes proviennent des serveurs des éditeurs et ne sont pas
          copiées sur ceux de CoShift. La jurisprudence européenne distingue
          nettement les deux situations&nbsp;: republier une photographie sur son
          propre serveur constitue une communication à un public nouveau et exige
          une autorisation, tandis que le simple affichage depuis la source
          d'origine, librement accessible, n'en constitue pas une.
        </p>

        <LegalSource>
          Cour de justice de l'Union européenne&nbsp;: <em>Svensson</em>,
          C-466/12, 13 février 2014&nbsp;; <em>GS Media</em>, C-160/15,
          8 septembre 2016&nbsp;; <em>Land Nordrhein-Westfalen contre Renckhoff</em>,
          C-161/17, 7 août 2018&nbsp;; <em>VG Bild-Kunst</em>, C-392/19,
          9 mars 2021.
        </LegalSource>

        <h3>Retrait</h3>
        <p>
          Tout éditeur qui souhaite ne plus figurer dans cette rubrique obtient
          le retrait de ses contenus sur simple demande à{" "}
          <a href={`mailto:${EDITEUR.signalement}`}>{EDITEUR.signalement}</a>,
          sans avoir à motiver sa demande ni à établir un préjudice.
        </p>
      </LegalSection>

      <LegalSection id="propriete" titre="Propriété intellectuelle de CoShift">
        <p>
          Le nom CoShift, le logotype, la charte graphique, les textes
          rédactionnels, l'agencement des écrans et le code source sont protégés
          par le livre XI du Code de droit économique.
        </p>
        <p>
          L'inscription confère un droit d'usage personnel du service. Elle ne
          transfère aucun droit de propriété intellectuelle&nbsp;; toute
          reproduction hors des exceptions légales requiert une autorisation
          écrite.
        </p>

        <h3>Composants tiers</h3>
        <p>
          L'application repose sur des bibliothèques libres, publiées sous
          licences MIT, ISC, Apache 2.0, BSD et, pour les polices de caractères
          Inter et Outfit, sous licence SIL Open Font. Ces licences sont
          respectées et les mentions de droits d'auteur préservées.
        </p>
        <p>
          Deux composants échappent à ce régime et méritent d'être nommés&nbsp;:
        </p>
        <dl>
          <dt>La bibliothèque cartographique</dt>
          <dd>
            Elle n'est pas libre. Depuis sa version 2, elle est diffusée sous les
            conditions d'utilisation propriétaires de son éditeur, qui
            subordonnent son usage à un compte, à un jeton d'accès et à une
            facturation au chargement de carte. C'est l'unique dépendance non
            libre de l'interface.
          </dd>

          <dt>Le connecteur de base de données</dt>
          <dd>
            Il est publié sous licence GNU GPL version 2, assortie d'une exception
            pour les logiciels libres. L'obligation de publier le code source
            qu'emporte cette licence se déclenche à la{" "}
            <em>distribution</em> du logiciel. CoShift étant exploité comme
            service en ligne et n'étant pas distribué à ses utilisateurs, le
            déclencheur n'est pas actionné. Distribuer un jour l'application
            imposerait de revoir ce choix — ou de changer de connecteur.
          </dd>
        </dl>
      </LegalSection>

      <LegalSection id="suspension" titre="Suspension et résiliation">
        <p>
          CoShift peut suspendre ou clôturer un compte en cas de manquement aux
          présentes conditions, notamment de transport rémunéré déguisé,
          d'usurpation d'identité ou de comportement mettant en danger d'autres
          membres.
        </p>
        <p>
          Sauf urgence ou obligation légale, la mesure est précédée d'une
          information de la personne concernée, exposant les motifs et lui
          permettant de faire valoir ses observations. La décision motivée lui
          est notifiée, avec les voies de recours ouvertes.
        </p>
        <p>
          L'utilisateur peut résilier à tout moment en supprimant son compte,
          sans préavis ni justification.
        </p>

        <LegalSource>
          Article 17 du règlement (UE) 2022/2065&nbsp;: obligation d'exposer les
          motifs de toute restriction imposée à un destinataire du service,
          y compris la suspension d'un compte.
        </LegalSource>
      </LegalSection>

      <LegalSection id="paiement" titre="Paiement et rétractation">
        <p>
          <strong>
            Aucun paiement n'est perçu par CoShift à ce jour.
          </strong>{" "}
          L'accès au service est gratuit pour les personnes physiques, et la
          participation aux frais se règle directement entre le conducteur et le
          passager.
        </p>
        <p>
          L'application tient la <strong>comptabilité</strong> de cette
          participation — ce qui est dû, ce qui est déclaré réglé, ce qui est
          rendu en cas d'annulation — et applique un barème identique pour tous.
          Elle ne procède à <strong>aucun encaissement</strong> : cette activité
          suppose un statut d'agent de paiement dont CoShift ne dispose pas.
          Lorsque l'écran indique un montant réglé, il enregistre une déclaration
          des parties, non un mouvement de fonds.
        </p>
        <p>
          Le jour où un paiement en ligne sera proposé, les obligations relatives
          aux contrats à distance s'appliqueront intégralement&nbsp;: information
          précontractuelle, confirmation sur support durable et{" "}
          <strong>droit de rétractation de quatorze jours</strong>. Ce délai est
          d'ordre public&nbsp;; aucune condition générale ne peut le raccourcir.
        </p>

        <LegalSource>
          Livre VI du Code de droit économique, articles VI.45 et suivants,
          transposant la directive 2011/83/UE relative aux droits des
          consommateurs. Le délai de rétractation est de quatorze jours à compter
          de la conclusion du contrat de service.
        </LegalSource>
      </LegalSection>

      <LegalSection id="modification" titre="Modification des conditions">
        <p>
          CoShift peut modifier les présentes conditions. Toute modification
          substantielle est annoncée aux personnes inscrites avant son entrée en
          vigueur, avec un délai raisonnable pour en prendre connaissance et,
          le cas échéant, résilier.
        </p>
        <p>
          La version en vigueur et sa date figurent en tête de cette page. La
          version acceptée par chaque personne est conservée avec la date de son
          acceptation.
        </p>
        <p>
          Les présentes conditions sont régies par le droit belge. Les
          juridictions compétentes et les voies de recours sont indiquées dans
          les <Link to="/mentions-legales#droit">mentions légales</Link>.
        </p>
      </LegalSection>
    </LegalLayout>
  );
}
