/**
 * Socle documentaire des pages légales.
 *
 * <h2>Pourquoi une source unique</h2>
 *
 * Les mentions légales, la politique de confidentialité, les conditions
 * générales et la politique de cookies répètent les mêmes informations :
 * l'identité de l'éditeur, l'adresse de contact, la liste des sous-traitants,
 * la date de dernière mise à jour. Recopiées dans quatre fichiers, elles
 * divergent au premier changement — et un site dont les mentions légales
 * contredisent la politique de confidentialité est un site dont aucune des
 * deux ne fait foi.
 *
 * <h2>Le registre est ici, pas dans un tableur</h2>
 *
 * L'article 30 du RGPD impose un registre des activités de traitement. Le
 * tenir dans le code plutôt qu'à côté a une conséquence utile : il se lit au
 * même endroit que les entités qu'il décrit, et un traitement ajouté sans
 * ligne de registre saute aux yeux à la relecture.
 *
 * <h2>Avertissement</h2>
 *
 * CoShift est un travail de fin d'études. Les données d'identification de
 * l'éditeur sont <strong>fictives</strong> et signalées comme telles sur
 * chaque page. Le numéro d'entreprise porte une somme de contrôle
 * volontairement invalide : il ne peut correspondre à aucune société réelle.
 */

/* ────────────────────────────────────────────────────────────────────────
   Identité de l'éditeur — article XII.6 du Code de droit économique
   ──────────────────────────────────────────────────────────────────────── */

export const EDITEUR = {
  denomination: "CoShift SRL",
  forme: "Société à responsabilité limitée de droit belge",
  siege: "Rue de la Mobilité 1, 1000 Bruxelles, Belgique",
  bce: "0999.999.999",
  tva: "BE 0999.999.999",
  rpm: "Tribunal de l'entreprise francophone de Bruxelles",
  representant: "Élisabeth Benga",
  contact: "contact@coshift.be",
  viePrivee: "privacy@coshift.be",
  signalement: "signalement@coshift.be",
  domaine: "coshift.be",
} as const;

/**
 * Hébergement.
 *
 * <p>Le service n'est pas déployé : il s'exécute en environnement local. Écrire
 * le nom d'un hébergeur qui n'héberge rien serait une fausse mention, alors que
 * l'article XII.6 vise précisément la sincérité de l'identification. On déclare
 * donc l'état réel et les critères retenus pour le choix à venir.</p>
 */
export const HEBERGEMENT = {
  statut: "non déployé — exécution en environnement de développement local",
  criteres: [
    "Centres de données situés dans l'Espace économique européen",
    "Contrat de sous-traitance conforme à l'article 28 du RGPD",
    "Aucun transfert de données vers un pays tiers sans garantie appropriée",
  ],
} as const;

/* ────────────────────────────────────────────────────────────────────────
   Versions et dates
   ──────────────────────────────────────────────────────────────────────── */

/** Version des conditions générales acceptée à l'inscription et conservée en base. */
export const VERSION_CGU = "1.0";

/** Version de la politique de confidentialité. */
export const VERSION_CONFIDENTIALITE = "1.0";

/** Format ISO : sert la balise `dateModified` des données structurées. */
export const DATE_MAJ_ISO = "2026-08-20";

/** Même date, lisible. */
export const DATE_MAJ = "20 août 2026";

/* ────────────────────────────────────────────────────────────────────────
   Registre des activités de traitement — RGPD, article 30
   ──────────────────────────────────────────────────────────────────────── */

export interface Traitement {
  /** Ancre de la section, pour les renvois internes. */
  id: string;
  nom: string;
  /** Pourquoi les données sont traitées — RGPD, article 5.1.b. */
  finalite: string;
  /** Base légale invoquée — RGPD, article 6.1. */
  base: string;
  /** Ce qui justifie cette base plutôt qu'une autre. */
  justification: string;
  /** Catégories de données, telles qu'elles existent réellement en base. */
  donnees: string[];
  /** Durée de conservation — RGPD, article 5.1.e. */
  duree: string;
  /** Qui d'autre y accède. */
  destinataires: string;
}

export const REGISTRE: Traitement[] = [
  {
    id: "compte",
    nom: "Gestion du compte utilisateur",
    finalite:
      "Créer et maintenir un compte, authentifier son titulaire, rattacher " +
      "la personne à son organisation et lui permettre de se faire reconnaître " +
      "des autres membres.",
    base: "Exécution du contrat — article 6.1.b",
    justification:
      "Sans compte, aucun service ne peut être rendu : la relation entre la " +
      "personne et CoShift est contractuelle, et ces données en sont la " +
      "condition d'exécution. Le consentement serait ici une base impropre, " +
      "puisqu'il ne pourrait pas être retiré sans mettre fin au service.",
    donnees: [
      "Adresse électronique professionnelle",
      "Nom et prénom",
      "Numéro de téléphone (facultatif)",
      "Photographie de profil (facultative)",
      "Mot de passe, conservé sous forme d'empreinte BCrypt",
      "Organisation de rattachement",
      "Dates de création et de dernière modification",
    ],
    duree:
      "Durée du compte. La suppression demandée par le titulaire est appliquée " +
      "immédiatement et sans délai de grâce : un délai supposerait de conserver " +
      "les données pendant ce temps, ce qui contredirait la demande.",
    destinataires:
      "Les autres membres de l'organisation voient le nom, le prénom et la " +
      "photographie. Le numéro de téléphone n'est révélé qu'au conducteur et " +
      "au passager d'une réservation confirmée.",
  },
  {
    id: "verification",
    nom: "Vérification de l'adresse et réinitialisation du mot de passe",
    finalite:
      "S'assurer que l'adresse déclarée appartient bien à la personne, et lui " +
      "permettre de reprendre la main sur son compte après un oubli.",
    base: "Exécution du contrat et obligation de sécurité — articles 6.1.b et 32",
    justification:
      "Un cercle fermé par organisation n'a de sens que si l'appartenance est " +
      "vérifiée. La vérification n'est donc pas un confort : elle fonde la " +
      "confiance que les membres se portent entre eux.",
    donnees: [
      "Code à six chiffres et son horodatage d'expiration",
      "État de vérification de l'adresse",
    ],
    duree:
      "Une heure pour le code, effacé dès son usage. L'état de vérification " +
      "suit la durée du compte.",
    destinataires: "Personne. Ces champs ne sont exposés par aucune API.",
  },
  {
    id: "trajets",
    nom: "Publication et recherche de trajets",
    finalite:
      "Mettre en relation les personnes qui parcourent le même itinéraire au " +
      "même moment, et permettre au conducteur de décider qui monte.",
    base: "Exécution du contrat — article 6.1.b",
    justification:
      "C'est l'objet même du service. Les données de déplacement méritent " +
      "toutefois une vigilance particulière : croisées dans le temps, elles " +
      "révèlent un domicile, un employeur et des habitudes. Elles ne sont " +
      "jamais recoupées à des fins de profilage, et aucune géolocalisation " +
      "continue n'est collectée.",
    donnees: [
      "Villes et adresses de départ et d'arrivée",
      "Date et heure de départ",
      "Nombre de places, participation aux frais",
      "Préférences déclarées (bagages, animaux, musique, conversation)",
      "Marque, modèle, motorisation et plaque d'immatriculation du véhicule",
    ],
    duree:
      "24 mois après la date du trajet, puis anonymisation : le trajet subsiste " +
      "sans lien vers une personne, pour les statistiques agrégées.",
    destinataires:
      "Les membres de l'organisation qui recherchent un trajet. La plaque " +
      "d'immatriculation n'est affichée à personne d'autre que son propriétaire.",
  },
  {
    id: "reservations",
    nom: "Réservations et échanges entre membres",
    finalite:
      "Enregistrer une demande de place, la décision du conducteur et le motif " +
      "d'un refus ou d'une annulation.",
    base: "Exécution du contrat — article 6.1.b",
    justification:
      "Le motif d'un refus est conservé parce qu'il est communiqué à la " +
      "personne concernée : une décision qu'on ne peut pas contester est une " +
      "décision qu'on ne peut pas corriger.",
    donnees: [
      "Identité du passager et du conducteur",
      "Nombre de places, montant du partage de frais",
      "Statut de la demande et motif de refus ou d'annulation",
    ],
    duree: "24 mois après la date du trajet concerné.",
    destinataires: "Le conducteur et le passager concernés, exclusivement.",
  },
  {
    id: "securite",
    nom: "Journal des événements de sécurité",
    finalite:
      "Détecter les tentatives d'intrusion, les accès refusés et l'acharnement " +
      "sur un compte, puis pouvoir en avertir la personne visée.",
    base: "Intérêt légitime — article 6.1.f",
    justification:
      "Une trace sans identifiant ne permet ni de constater une attaque ciblée " +
      "ni de prévenir sa victime. L'intérêt à sécuriser le service l'emporte " +
      "ici sur l'atteinte, tenue au minimum : aucun mot de passe, jeton ni code " +
      "n'est écrit, même tronqué, et la conservation est bornée à douze mois.",
    donnees: [
      "Adresse électronique du compte concerné",
      "Adresse IP de l'appelant",
      "Nature de l'événement et horodatage",
    ],
    duree: "12 mois, par rotation quotidienne des fichiers.",
    destinataires:
      "L'administrateur technique. Communication à une autorité judiciaire sur " +
      "réquisition régulière.",
  },
  {
    id: "opendata",
    nom: "Publication de données ouvertes de mobilité",
    finalite:
      "Publier des statistiques agrégées de mobilité, réutilisables librement " +
      "sous Licence Ouverte 2.0.",
    base: "Hors champ du RGPD après anonymisation — considérant 26",
    justification:
      "Aucune donnée publiée ne se rapporte à une personne identifiable : les " +
      "agrégats sont supprimés sous un seuil de cinq occurrences, de sorte " +
      "qu'un couple origine-destination rare ne puisse pas désigner une " +
      "personne. L'anonymisation étant irréversible, le règlement ne s'applique " +
      "pas au résultat — il s'applique en revanche à l'opération qui y mène.",
    donnees: [
      "Comptages agrégés par ville et par période",
      "Aucun identifiant, aucune date précise, aucun itinéraire individuel",
    ],
    duree: "Sans limite, les données publiées n'étant plus personnelles.",
    destinataires: "Le public.",
  },
  {
    id: "actualites",
    nom: "Agrégation d'actualités mobilité",
    finalite:
      "Alimenter la rubrique Actus à partir de deux agrégateurs de presse.",
    base: "Sans objet — aucune donnée personnelle d'utilisateur",
    justification:
      "Le traitement porte sur des contenus de presse, pas sur les personnes " +
      "inscrites. Il relève du droit d'auteur et du droit voisin des éditeurs " +
      "de presse, traités dans les conditions générales, et non du RGPD.",
    donnees: ["Titre, chapô, source, date et lien de l'article d'origine"],
    duree: "Sans limite fixée à ce jour.",
    destinataires: "Le public.",
  },
];

/* ────────────────────────────────────────────────────────────────────────
   Sous-traitants et destinataires tiers — RGPD, articles 28 et 44 et suivants
   ──────────────────────────────────────────────────────────────────────── */

export interface Tiers {
  nom: string;
  role: string;
  /** Ce qui lui parvient réellement, vérifié dans le code. */
  donnees: string;
  pays: string;
  /** Encadrement du transfert hors Espace économique européen, le cas échéant. */
  encadrement: string;
  /** Le service fonctionne-t-il sans ? Détermine s'il peut être soumis au consentement. */
  necessaire: boolean;
}

export const TIERS: Tiers[] = [
  {
    nom: "Google LLC — Identity Services",
    role: "Connexion par compte Google, proposée en complément du mot de passe",
    donnees:
      "Adresse IP, agent utilisateur, et — si la personne va au bout — son " +
      "adresse électronique, son nom et sa photographie Google",
    pays: "États-Unis",
    encadrement:
      "Décision d'adéquation « EU-US Data Privacy Framework » du 10 juillet 2023",
    necessaire: false,
  },
  {
    nom: "Mapbox Inc.",
    role: "Fond cartographique animé de la page d'accueil",
    donnees: "Adresse IP, agent utilisateur, coordonnées des tuiles demandées",
    pays: "États-Unis",
    encadrement:
      "Clauses contractuelles types de la décision d'exécution (UE) 2021/914",
    necessaire: false,
  },
  {
    nom: "GNews et NewsData.io",
    role: "Agrégateurs de presse interrogés par le serveur toutes les six heures",
    donnees:
      "Aucune donnée d'utilisateur. Les appels partent du serveur, jamais du " +
      "navigateur : ces services ne voient donc aucun visiteur.",
    pays: "Union européenne et États-Unis",
    encadrement: "Sans objet, aucune donnée personnelle n'étant transmise",
    necessaire: true,
  },
  {
    nom: "Fournisseur de messagerie sortante",
    role: "Envoi des codes de vérification et de réinitialisation",
    donnees: "Adresse électronique, prénom, code à six chiffres",
    pays: "Union européenne",
    encadrement: "Contrat de sous-traitance à conclure avant la mise en ligne",
    necessaire: true,
  },
];

/* ────────────────────────────────────────────────────────────────────────
   Stockage sur le terminal — article 129 de la loi du 13 juin 2005
   ──────────────────────────────────────────────────────────────────────── */

export interface Tracage {
  cle: string;
  type: "Stockage local" | "Cookie tiers";
  finalite: string;
  duree: string;
  /** Vrai si le service ne peut pas fonctionner sans : exempté de consentement. */
  exempte: boolean;
}

export const TRACAGES: Tracage[] = [
  {
    cle: "coshift_token",
    type: "Stockage local",
    finalite:
      "Conserver le jeton d'authentification entre deux pages. Sans lui, il " +
      "faudrait se reconnecter à chaque navigation.",
    duree: "24 heures, ou jusqu'à la déconnexion",
    exempte: true,
  },
  {
    cle: "coshift_theme",
    type: "Stockage local",
    finalite:
      "Retenir le choix entre thème clair et thème sombre. Ce choix est " +
      "explicite : le retenir exécute une demande de la personne.",
    duree: "Jusqu'à effacement par la personne",
    exempte: true,
  },
  {
    cle: "coshift_consentement",
    type: "Stockage local",
    finalite:
      "Mémoriser le choix exprimé dans le bandeau, sa date et la version des " +
      "documents alors en vigueur. Sans cette trace, le bandeau reparaîtrait à " +
      "chaque visite et le consentement ne serait pas démontrable.",
    duree: "6 mois, puis le choix est redemandé",
    exempte: true,
  },
  {
    cle: "Cookies déposés par Google Identity Services",
    type: "Cookie tiers",
    finalite:
      "Fonctionnement du bouton « Se connecter avec Google » et rattachement à " +
      "la session Google du navigateur.",
    duree: "Fixée par Google",
    exempte: false,
  },
  {
    cle: "Requêtes vers les serveurs de tuiles Mapbox",
    type: "Cookie tiers",
    finalite:
      "Affichage du fond cartographique. Chaque tuile demandée transmet " +
      "l'adresse IP du visiteur à Mapbox.",
    duree: "Le temps de la session",
    exempte: false,
  },
];

/* ────────────────────────────────────────────────────────────────────────
   Autorités de contrôle et voies de recours
   ──────────────────────────────────────────────────────────────────────── */

export const AUTORITES = [
  {
    nom: "Autorité de protection des données",
    objet: "Protection des données à caractère personnel",
    adresse: "Rue de la Presse 35, 1000 Bruxelles",
    lien: "https://www.autoriteprotectiondonnees.be",
  },
  {
    nom: "SPF Économie — Direction générale de l'Inspection économique",
    objet: "Pratiques du marché, information du consommateur, commerce électronique",
    adresse: "Boulevard du Roi Albert II 16, 1000 Bruxelles",
    lien: "https://economie.fgov.be",
  },
  {
    nom: "IBPT — coordinateur pour les services numériques",
    objet: "Application du règlement (UE) 2022/2065 sur les services numériques",
    adresse: "Boulevard du Roi Albert II 35, 1030 Bruxelles",
    lien: "https://www.ibpt.be",
  },
] as const;
