/**
 * Catalogue français — référence.
 *
 * <p>Il définit la forme que les autres langues doivent respecter : une clé
 * absente d'un catalogue étranger est une erreur de compilation. L'ordre suit
 * la navigation, écran par écran, pour qu'une relecture porte sur des chaînes
 * qui s'affichent ensemble.</p>
 *
 * <p>Les marques `{nom}` sont remplacées à l'affichage. Les cas de pluriel
 * emploient deux clés distinctes plutôt qu'une règle : l'anglais et le
 * néerlandais n'accordent pas comme le français, et laisser le choix au
 * traducteur vaut mieux qu'imposer une mécanique française.</p>
 */
export const fr = {
  commun: {
    chargement: "Chargement…",
    chargementEnCours: "Chargement en cours",
    rechercher: "Rechercher",
    envoyer: "Envoyer",
    annuler: "Annuler",
    enregistrer: "Enregistrer",
    fermer: "Fermer",
    retour: "Retour",
    precedent: "Précédent",
    suivant: "Suivant",
    voirPlus: "En savoir plus",
    seConnecter: "Se connecter",
    creerCompte: "Créer un compte",
    seDeconnecter: "Se déconnecter",
    erreurReseau: "Impossible de joindre le serveur. Veuillez réessayer.",
    erreurGenerique: "Une erreur est survenue. Réessayez dans un instant.",
    places_une: "{n} place",
    places_plusieurs: "{n} places",
    parPlace: "par place",
  },

  champ: {
    obligatoire: "obligatoire",
  },

  message: {
    info: "Information",
    success: "Succès",
    warning: "Attention",
    danger: "Erreur",
    fermer: "Fermer ce message",
  },

  langue: {
    choisir: "Choisir la langue",
    actuelle: "Langue actuelle : {nom}",
  },

  banniere: {
    defilante: "PRENEZ PART AU CHANGEMENT AVEC COSHIFT",
    accroche: "Une mobilité plus",
    mot1: "simple",
    mot2: "verte",
    mot3: "intelligente",
    mot4: "rapide",
    mot5: "collaborative",
  },

  theme: {
    sombre: "Mode sombre",
    passerEnClair: "Passer en mode clair",
    passerEnSombre: "Passer en mode sombre",
  },

  verification: {
    titre: "Vérifiez votre e-mail",
    accroche: "Nous avons envoyé un code à {n} chiffres à",
    votreAdresse: "votre adresse",
    legende: "Code de vérification à {n} chiffres",
    chiffre: "Chiffre {i} sur {n}",
    activer: "Activer mon compte",
    pasRecu: "Vous n'avez pas reçu de code ?",
    renvoyer: "Renvoyer le code",
    renvoyerDans: "Renvoyer dans {s} s",
    codeIncomplet: "Entrez les {n} chiffres de votre code de vérification.",
    codeIncorrect: "Code incorrect ou expiré. Réessayez.",
    codeRenvoye: "Un nouveau code vient d'être envoyé à votre adresse.",
    envoiImpossible: "Impossible d'envoyer le code. Réessayez.",
  },

  actus: {
    titre: "Actus mobilité — covoiturage, transports et mobilité durable",
    description:
      "Le flux d'actualités CoShift sur la mobilité : covoiturage, transports en commun, mobilité douce et politiques de déplacement en Belgique et en Europe.",
    entete: "Info mobilité",
    accroche:
      "L'essentiel de l'actualité transport et écologie, filtré pour les navetteurs.",
    filtrer: "Filtrer par thème",
    toutes: "Toutes les actus",
    mobilite: "Mobilité & transport",
    ecologie: "Écologie & climat",
    entreprises: "Entreprises & RH",
    technologie: "Tech & innovation",
    chargement: "Chargement des actualités",
    aucunArticle: "Aucun article dans ce thème",
    aucunArticleTexte: "Choisissez un autre filtre pour retrouver le flux complet.",
    voirTout: "Voir tout",
    paginationLabel: "Pages d'articles",
    retourAccueil: "Retour à l'accueil",
    lire: "Lire",
    serveurInjoignable:
      "Impossible de contacter le serveur. Réessayez dans un moment.",
  },

  article: {
    titreDefaut: "Actus mobilité",
    descriptionDefaut: "Article du flux d'actualités CoShift sur la mobilité.",
    chargement: "Chargement de l'article",
    introuvable: "Cet article est introuvable.",
    retourActus: "Retour aux actualités",
    toutesActus: "Toutes les actualités",
    source: "Source :",
    avertissement:
      "CoShift agrège et résume l'actualité mobilité. L'article complet reste chez son éditeur.",
    lireSur: "Lire l'article sur {source}",
    memeRubrique: "Dans la même rubrique",
  },

  trajets: {
    trouverTitre: "Trouver un trajet",
    trouverAccroche:
      "Recherchez parmi les trajets disponibles et réservez votre place.",
    chargementRecherche: "Recherche des trajets",
    lancezRecherche: "Lancez une recherche",
    lancezRechercheTexte:
      "Indiquez au moins une ville de départ ou d'arrivée pour voir les trajets disponibles.",
    aucunResultat: "Aucun trajet pour ces critères",
    aucunResultatTexte:
      "Élargissez la date ou l'heure de départ, ou retirez le filtre sur le nombre de places.",
    disponible_un: "{n} trajet disponible",
    disponible_plusieurs: "{n} trajets disponibles",
    erreurRecherche: "Une erreur est survenue lors de la recherche.",
  },

  photos: {
    habitacle:
      "Quatre personnes souriantes installées dans une voiture, ceintures bouclées.",
    depart: "Un conducteur au volant et trois passagers, sur le point de partir.",
    rendezVous:
      "Une passagère se penche à la portière d'une voiture pour retrouver sa conductrice.",
    application:
      "Un passager consulte l'application CoShift sur son téléphone pendant le trajet.",
  },

  apropos: {
    titre: "À propos — pourquoi CoShift existe",
    description:
      "CoShift est né d'un constat : la plupart des voitures qui vont au travail chaque matin transportent une seule personne. Voici la démarche et les choix du projet.",
    heroTitre:
      "Le covoiturage qui commence à la porte de votre organisation",
    heroAccroche:
      "CoShift met en relation les personnes qui font le même trajet, au même moment, pour aller au même endroit — leur lieu de travail, leur campus, un événement. Rien de plus, et c'est déjà beaucoup.",

    constatTitre: "Le constat de départ",
    constatP1:
      "Chaque matin, des voitures parcourent le même itinéraire, à la même heure, vers la même destination — avec une seule personne à bord. Ce n'est pas un choix : c'est l'absence d'alternative pratique. Le train ne dessert pas le zoning, le bus impose deux correspondances, et personne ne sait qui, parmi ses collègues, part du même quartier.",
    constatP2:
      "Les plateformes de covoiturage grand public répondent mal à ce besoin. Elles sont conçues pour le trajet exceptionnel — un Bruxelles-Paris un vendredi soir — pas pour les vingt kilomètres répétés deux fois par jour, cinq jours par semaine, avec des gens qu'on retrouve à la machine à café.",
    constatFort:
      "CoShift part de l'organisation, pas du trajet. C'est ce déplacement du point de départ qui change tout le reste.",

    fonctionnementTitre: "Comment ça fonctionne",
    fonctionnementLegende:
      "Le point de rendez-vous est indiqué par le conducteur au moment où il publie son trajet.",
    etape1Titre: "Vous rejoignez votre organisation",
    etape1Texte:
      "L'inscription se fait avec votre adresse professionnelle. C'est elle qui vous rattache à votre entreprise, votre école ou l'événement, et qui garantit à vos collègues qu'ils partagent la route avec quelqu'un d'identifié.",
    etape2Titre: "Vous publiez ou vous cherchez",
    etape2Texte:
      "Un conducteur déclare son véhicule, indique son itinéraire, son horaire et le nombre de places. Un passager cherche par ville, par date et par heure de départ.",
    etape3Titre: "Le conducteur accepte",
    etape3Texte:
      "Une demande n'est pas une réservation ferme : le conducteur l'accepte ou la refuse, en motivant son refus. C'est lui qui décide qui monte dans sa voiture.",
    etape4Titre: "Vous partagez la route et les frais",
    etape4Texte:
      "Le numéro de téléphone du conducteur n'est transmis qu'une fois la réservation confirmée. Le prix couvre le partage des frais, jamais un bénéfice.",

    pourQui: "Pour qui",
    public1Titre: "Entreprises",
    public1Texte:
      "Le stationnement sature, les horaires se ressemblent et les trajets se doublent. Regrouper les navetteurs d'un même site est le levier le plus simple.",
    public2Titre: "Universités et hautes écoles",
    public2Texte:
      "Des milliers d'étudiants convergent aux mêmes heures vers un campus rarement desservi comme un centre-ville. Le covoiturage comble ce que le train ne fait pas.",
    public3Titre: "Festivals et salons",
    public3Texte:
      "Un événement crée un pic de circulation sur quelques heures. Organiser le partage en amont évite un parking improvisé dans un champ.",

    partisPris: "Nos partis pris",
    principe1Titre: "Un cercle fermé, pas une place publique",
    principe1Texte:
      "CoShift n'est pas un service ouvert à tous. On y covoiture avec les membres de son organisation, ce qui change entièrement le rapport de confiance.",
    principe2Titre: "Le partage plutôt que le trajet",
    principe2Texte:
      "Un trajet effectué à deux ne devient pas plus écologique : c'est la voiture restée au garage qui compte. Toute l'interface met en avant les places effectivement partagées.",
    principe3Titre: "Le minimum de données",
    principe3Texte:
      "Adresse professionnelle, nom, éventuellement un téléphone. Pas de géolocalisation continue, pas de suivi publicitaire, pas de revente.",

    origineTitre: "D'où vient CoShift",
    origineP1:
      "CoShift est né d'un travail de fin d'études, développé de bout en bout — interface, API, base de données. Ce n'est pas une entreprise avec des années d'existence derrière elle, et la page que vous lisez ne prétendra pas le contraire.",
    origineP2:
      "Ce que le projet revendique, en revanche, c'est d'être fonctionnel : la publication d'un trajet, la recherche, la demande de place, l'acceptation par le conducteur et le suivi des réservations fonctionnent réellement, avec une vraie base de données derrière.",
    fait1: "Publication et recherche de trajets",
    fait2: "Réservation, acceptation, refus motivé, annulation",
    fait3: "Gestion des véhicules et du profil",
    fait4: "Vérification de l'adresse par code à six chiffres",
    fait5: "Flux d'actualités mobilité",
    origineNote:
      "L'espace dédié aux organisations, la notation entre membres et la messagerie interne sont les chantiers suivants. Les annoncer comme disponibles serait plus vendeur, mais faux.",

    ctaTitre: "Prêt à partager la route ?",
    ctaTexte:
      "Créez votre compte avec votre adresse professionnelle et voyez qui, autour de vous, fait déjà le même trajet.",
    ctaVoirTrajets: "Voir les trajets",
    ctaContact: "Une question sur le déploiement dans votre organisation ?",
    ctaContactLien: "Espace entreprises",
  },

  publier: {
    titre: "Proposer un trajet",
    accroche: "Partagez votre trajet et réduisez votre empreinte carbone.",
    publie: "Trajet publié",
    redirection: "Redirection vers votre tableau de bord…",
    chargementVehicules: "Chargement de vos véhicules",
    vehiculesIndisponibles: "Impossible de charger vos véhicules.",
    aucunVehicule: "Aucun véhicule enregistré",
    aucunVehiculeTexte:
      "Un trajet se publie avec un véhicule. Enregistrez-en un d'abord.",
    ajouterVehicule: "Ajouter un véhicule",
    selectionnezVehicule: "Sélectionnez un véhicule.",

    itineraire: "Itinéraire",
    villeDepart: "Ville de départ",
    villeDepartExemple: "Liège",
    villeArrivee: "Ville d'arrivée",
    villeArriveeExemple: "Bruxelles",
    pointDepart: "Point de départ précis",
    pointDepartExemple: "Gare de Liège-Guillemins",
    pointDepartAide: "Aide vos passagers à vous retrouver.",
    pointArrivee: "Point d'arrivée précis",
    pointArriveeExemple: "Gare du Midi, Bruxelles",

    datePlaces: "Date et places",
    dateHeure: "Date et heure de départ",
    dateHeureAide: "Au plus tôt dans deux heures.",
    placesProposees: "Places proposées",
    placesProposeesAide: "Jusqu'à {max}, votre siège déduit.",
    prixParPlace: "Prix par place (€)",
    prixParPlaceAide: "Partage de frais, pas un bénéfice.",

    vehicule: "Véhicule",
    choisirVehicule: "Choisissez le véhicule du trajet",
    placesVehicule: "{n} places",

    details: "Détails et préférences",
    description: "Description",
    descriptionAide: "Point de ramassage, étapes, contraintes particulières.",
    preferences: "Préférences du trajet",
    bagagesAcceptes: "Bagages acceptés",
    animauxAcceptes: "Animaux acceptés",
    musiqueAutorisee: "Musique autorisée",
    discussionBienvenue: "Discussion bienvenue",

    publierLeTrajet: "Publier le trajet",
  },

  detail: {
    chargement: "Chargement du trajet",
    introuvable: "Ce trajet est introuvable.",
    retourRecherche: "Retour à la recherche",

    demandeEnvoyee: "Demande envoyée",
    demandeEnvoyeeTexte:
      "Le conducteur doit maintenant l'accepter. Redirection vers vos réservations…",
    trajetAnnule: "Trajet annulé",
    trajetAnnuleTexte:
      "Il n'apparaît plus dans les recherches. Les réservations qui le concernaient ont été annulées.",
    reservationImpossible: "La réservation n'a pas pu être enregistrée.",
    annulationImpossible: "Le trajet n'a pas pu être annulé.",

    bagagesAcceptes: "Bagages acceptés",
    bagagesRefuses: "Bagages refusés",
    animauxAcceptes: "Animaux acceptés",
    animauxRefuses: "Animaux refusés",
    musiqueAutorisee: "Musique autorisée",
    sansMusique: "Sans musique",
    discussionBienvenue: "Discussion bienvenue",
    trajetSilencieux: "Trajet silencieux",

    conducteur: "Conducteur",
    nouveauConducteur: "Nouveau conducteur",
    trajet_un: "{n} trajet",
    trajet_plusieurs: "{n} trajets",
    vehicule: "Véhicule",
    placesVehicule: "{n} places",

    parPlace: " / place",
    placesRestantes_une: "{n} place restante",
    placesRestantes_plusieurs: "{n} places restantes",
    complet: "Complet",
    nombreDePlaces: "Nombre de places",
    retirerPlace: "Retirer une place",
    ajouterPlace: "Ajouter une place",
    total: "Total",
    reserver: "Réserver",
    indicationDemande:
      "Votre demande part au conducteur. Elle n'est confirmée qu'après son accord.",
    vousEtesConducteur: "Vous êtes le conducteur de ce trajet.",
    plusDeReservation: "Ce trajet n'accepte plus de réservation.",
    plusDePlace: "Il ne reste plus de place disponible.",

    annulerCeTrajet: "Annuler ce trajet",
    annulerIndication: "Les demandes et réservations en cours seront annulées.",
    annulerTitre: "Annuler ce trajet ?",
    annulerBouton: "Annuler le trajet",
    annulerP1: "Le trajet {trajet} du {quand} sera retiré des recherches.",
    annulerP2:
      "Toutes les demandes en attente et les réservations déjà confirmées seront annulées avec le motif « Trajet annulé par le conducteur ». Cette action est définitive.",
  },

  reservations: {
    titre: "Mes réservations",
    accroche: "Vos demandes de place et leur suivi.",
    trouverTrajet: "Trouver un trajet",
    chargement: "Chargement de vos réservations",
    indisponibles: "Impossible de charger vos réservations.",
    aucune: "Aucune réservation",
    aucuneTexte:
      "Vous n'avez pas encore réservé de place. Cherchez un trajet pour commencer.",
    motif: "Motif :",
    nouveauConducteur: "Nouveau conducteur",
    reservee_une: "{n} place réservée",
    reservee_plusieurs: "{n} places réservées",
    voirLeTrajet: "Voir le trajet",
    annuler: "Annuler",
    annulationEchouee: "L'annulation a échoué.",
    /* F21 — confirmation de prestation par le passager. */
    confirmerTrajet: "J'ai fait ce trajet",
    confirmationEchouee: "La confirmation a échoué.",
    confirmeLe: "Trajet confirmé le {date}.",
    /* F22 / F31 — notation reciproque. */
    noter: "Noter",
    noterTitre: "Votre avis sur ce trajet",
    noterIntro: "Votre note aide les prochains membres à choisir. Elle n'est pas modifiable.",
    noteLabel: "Votre note",
    noteRequise: "Choisissez une note avant d'envoyer.",
    commentaireLabel: "Commentaire (facultatif)",
    commentairePlaceholder: "Ponctualité, conduite, ambiance…",
    envoyerAvis: "Envoyer mon avis",
    avisEnvoye: "Merci, votre avis a bien été enregistré.",
    avisEchoue: "L'envoi de l'avis a échoué.",
    dejaNote: "Vous avez noté ce trajet.",
    etoile_une: "1 étoile sur 5",
    etoile_deux: "2 étoiles sur 5",
    etoile_trois: "3 étoiles sur 5",
    etoile_quatre: "4 étoiles sur 5",
    etoile_cinq: "5 étoiles sur 5",
    annulerTitre: "Annuler cette réservation ?",
    confirmerAnnulation: "Confirmer l'annulation",
    annulerTexte:
      "Votre place sur le trajet {trajet} sera remise à disposition, et le conducteur sera prévenu. Cette action est définitive.",
  },

  demandes: {
    titre: "Demandes reçues",
    accroche: "Les passagers qui ont demandé une place dans vos trajets.",
    enAttente_une: "{n} demande en attente de votre réponse.",
    enAttente_plusieurs: "{n} demandes en attente de votre réponse.",
    chargement: "Chargement des demandes",
    indisponibles: "Impossible de charger les demandes reçues.",
    aucune: "Aucune demande pour l'instant",
    aucuneTexte:
      "Les demandes de réservation sur vos trajets apparaîtront ici.",
    nouveauPassager: "Nouveau passager",
    trajet_un: "{n} trajet",
    trajet_plusieurs: "{n} trajets",
    place_une: "{n} place",
    place_plusieurs: "{n} places",
    accepter: "Accepter",
    refuser: "Refuser",
    operationEchouee: "L'opération a échoué.",
    refuserTitre: "Refuser cette demande ?",
    motifDuRefus: "Motif du refus",
    motifAide:
      "Facultatif, mais le passager le verra. Une raison brève évite les malentendus.",
    motifExemple: "La voiture est déjà complète pour ce trajet.",
  },

  tableau: {
    membre: "Membre CoShift",
    administrateur: "Administrateur",
    emailNonVerifie: "E-mail non vérifié",
    trajet_un: "{n} trajet",
    trajet_plusieurs: "{n} trajets",
    modifierProfil: "Modifier le profil",
    adresseNonVerifieeTitre: "Votre adresse n'est pas vérifiée",
    adresseNonVerifieeTexte:
      "Vous ne pourrez pas réserver de trajet tant que votre e-mail n'est pas confirmé.",

    sections: "Sections du tableau de bord",
    vueEnsemble: "Vue d'ensemble",
    demandesRecues: "Demandes reçues",
    mesVehicules: "Mes véhicules",
    mesDonnees: "Mes données",
    demandesEnAttente: "demandes en attente",

    chargement: "Chargement de vos données",
    mesTrajets: "Mes trajets proposés",
    nouveau: "Nouveau",
    aucunTrajetPropose:
      "Vous n'avez pas encore proposé de trajet à vos collègues.",
    mesReservations: "Mes réservations",
    toutVoir: "Tout voir",
    aucuneReservationEnCours: "Vous n'avez aucune réservation en cours.",

    activite: "Mon activité de partage",
    publie_un: "trajet publié",
    publie_plusieurs: "trajets publiés",
    reservation_une: "réservation",
    reservation_plusieurs: "réservations",
    partagee_une: "place effectivement partagée",
    partagee_plusieurs: "places effectivement partagées",
    noteCo2:
      "Le CO₂ évité n'est pas affiché : son calcul demande la distance de chaque trajet, que l'API ne fournit pas encore.",

    modifierMonProfil: "Modifier mon profil",
    envoi: "Envoi…",
    changerPhoto: "Changer la photo",
    photoAide: "JPG ou PNG, 2 Mo maximum.",
    prenom: "Prénom",
    nom: "Nom",
    email: "Adresse e-mail",
    emailAide: "La changer demandera une nouvelle vérification.",
    telephone: "Téléphone",
    telephoneAide:
      "Communiqué à vos passagers une fois la réservation confirmée.",
  },

  vehicules: {
    titre: "Mes véhicules",
    accroche: "Enregistrez vos véhicules pour pouvoir proposer des trajets.",
    ajouter: "Ajouter un véhicule",
    chargement: "Chargement de vos véhicules",
    indisponibles: "Impossible de charger vos véhicules.",
    aucun: "Aucun véhicule enregistré",
    aucunTexte: "Un trajet se publie avec un véhicule. Commencez par en déclarer un.",
    places: "{n} places",
    modifier: "Modifier",
    supprimer: "Supprimer",
    suppressionImpossible: "Impossible de supprimer ce véhicule.",
    modifierTitre: "Modifier le véhicule",
    ajouterAction: "Ajouter",
    marque: "Marque",
    marqueExemple: "Renault",
    modele: "Modèle",
    modeleExemple: "Clio",
    plaque: "Plaque d'immatriculation",
    plaqueExemple: "1-ABC-123",
    plaqueAide: "Elle identifie votre véhicule de façon unique.",
    nombreDePlaces: "Nombre de places",
    nombreDePlacesAide: "Places totales, votre siège compris.",
    motorisation: "Motorisation",
    supprimerTitre: "Supprimer ce véhicule ?",
    supprimerTexte:
      "{vehicule} sera retiré de votre compte. Les trajets déjà publiés avec ce véhicule ne sont pas supprimés.",
  },

  donnees: {
    recuperer: "Récupérer mes données",
    recupererRef: "Articles 15 et 20 du RGPD",
    recupererTexte:
      "Téléchargez tout ce que CoShift détient sur vous : votre compte, vos organisations, vos véhicules, les trajets que vous avez proposés et les réservations que vous avez demandées. Le fichier est au format JSON, lisible par une machine comme par un humain.",
    recupererNote:
      "Les données des autres membres en sont exclues. Un trajet réservé chez quelqu'un apparaît avec son itinéraire et son horaire, jamais avec son téléphone : ce sont ses données, pas les vôtres.",
    exporter: "Exporter mes données",
    exportReussi: "Votre export a été téléchargé au format JSON.",
    exportEchoue: "L'export n'a pas pu être produit. Réessayez dans un instant.",

    tiers: "Mes services tiers",
    tiersRef: "Article 7.3 du RGPD",
    carteMapbox: "Carte animée — Mapbox :",
    connexionGoogle: "Connexion Google :",
    autorisee: "autorisée",
    refusee: "refusée",
    choixExprime:
      "Choix exprimé le {date}, sur la version {version} de la politique de confidentialité.",
    pasEncoreRepondu:
      "Vous n'avez pas encore répondu au bandeau. Aucun service tiers n'est chargé.",
    revoirChoix: "Revoir mon choix",

    documents: "Ce à quoi vous avez souscrit",
    documentsRef: "Article 13 du RGPD",
    docConfidentialite: "ce qui est collecté, pourquoi, et pour combien de temps",
    docCgu: "les engagements de chacun",
    docCookies: "ce qui est stocké dans votre navigateur",
    docMentions: "qui édite le service",

    supprimer: "Supprimer mon compte",
    supprimerRef: "Article 17 du RGPD",
    supprimerIrreversible: "Cette action est irréversible.",
    supprimerP1:
      "Votre nom, votre adresse, votre téléphone, votre photographie et vos plaques d'immatriculation sont effacés immédiatement, sans copie de sauvegarde.",
    supprimerP2:
      "Vos trajets et réservations passés sont anonymisés plutôt que supprimés : ils engagent d'autres membres, dont l'historique ne peut pas être détruit par votre demande. Une fois détachés de vous, ils ne désignent plus personne.",
    supprimerP3:
      "Vos trajets à venir et vos réservations en cours sont annulés, avec un motif explicite, pour que personne ne se présente à un rendez-vous qui n'aura pas lieu.",
    supprimerBouton: "Supprimer définitivement mon compte",
    confirmerTitre: "Confirmer la suppression",
    aucunRetour: "Aucun retour en arrière",
    aucunRetourTexte:
      "Une fois l'opération lancée, ni vous ni CoShift ne pourrez récupérer votre compte. Pensez à exporter vos données auparavant si vous souhaitez les conserver.",
    retapezAdresse: "Retapez votre adresse électronique pour confirmer",
    retapezAide:
      "Le serveur exige la même confirmation : sans elle, la requête est refusée.",
    supprimerConfirmation: "Supprimer mon compte",
    suppressionEchouee: "La suppression n'a pas abouti. Réessayez dans un instant.",
  },

  pages: {
    tableauDeBordTitre: "Tableau de bord",
    tableauDeBordDescription:
      "Vos trajets proposés, vos réservations et vos véhicules.",
    reservationsTitre: "Mes réservations",
    reservationsDescription: "Le suivi de vos demandes de place.",
    publierTitre: "Proposer un trajet",
    publierDescription:
      "Publiez un trajet et partagez les frais avec vos collègues.",
    detailTitre: "Détail du trajet",
    detailDescription: "Horaire, conducteur, véhicule et places disponibles.",
    styleguideTitre: "Charte graphique",
    styleguideDescription:
      "Planche de référence des composants et des couleurs de CoShift.",
  },

  nav: {
    accueil: "CoShift, accueil",
    entreprises: "Entreprises",
    actus: "Actus Mobilité",
    apropos: "À propos",
    blog: "Le Blog",
    telecharger: "Téléchargez l'App",
    telechargerBientot:
      "L'installation de l'application mobile sera proposée ici.",
    monProfil: "Mon profil",
    connexion: "Connexion",
    ouvrirMenu: "Ouvrir le menu",
    fermerMenu: "Fermer le menu",
    bonjour: "Bonjour, {prenom}",
    tableauDeBord: "Tableau de bord",
    bonRetour: "Bon retour",
    bonRetourTexte: "Accédez à votre espace CoShift.",
    nouveau: "Nouveau ici ?",
    nouveauTexte: "Rejoignez la mobilité de demain.",
  },

  pied: {
    baseline: "Le covoiturage qui commence à la porte de votre organisation.",
    service: "Le service",
    chercherTrajet: "Rechercher un trajet",
    proposerTrajet: "Proposer un trajet",
    espaceEntreprises: "Espace entreprises",
    ressources: "Ressources",
    actualites: "Actualités mobilité",
    donneesOuvertes: "Données ouvertes",
    documentationApi: "Documentation de l'API",
    charteGraphique: "Charte graphique",
    legal: "Informations légales",
    mentions: "Mentions légales",
    confidentialite: "Politique de confidentialité",
    cgu: "Conditions générales",
    cookies: "Cookies et traceurs",
    revoirChoix: "Revoir mon choix de traceurs",
    signaler: "Signaler un contenu",
    projetAcademique:
      "Projet de fin d'études — données d'identification fictives, détaillées dans les",
    projetAcademiqueLien: "mentions légales",
  },

  consentement: {
    titre: "Deux services tiers, votre choix",
    texte:
      "CoShift fonctionne sans vous pister. Deux fonctions facultatives font toutefois appel à des services établis aux États-Unis, qui reçoivent alors votre adresse IP : la carte animée de l'accueil et le bouton de connexion Google.",
    texteFort: "Rien n'est chargé tant que vous n'avez pas répondu.",
    toutRefuser: "Tout refuser",
    toutAccepter: "Tout accepter",
    serviceParService: "Choisir service par service",
    replier: "Replier",
    enregistrerChoix: "Enregistrer mes choix",
    carteTitre: "Carte animée — Mapbox",
    carteTexte:
      "Affiche le fond cartographique de la page d'accueil. Sans lui, la page reste complète : seule l'animation est remplacée par un aplat.",
    googleTitre: "Connexion Google — Google Identity Services",
    googleTexte:
      "Active le bouton « Continuer avec Google » sur l'écran de connexion. La connexion par adresse et mot de passe fonctionne sans lui.",
    pied: "Votre choix est conservé six mois et reste modifiable depuis le pied de page. Détail de ce qui est stocké :",
    piedLien: "cookies et traceurs",
    googleDesactive:
      "La connexion par compte Google est désactivée : vous n'avez pas autorisé ce service tiers.",
    googleNonCharge:
      "La connexion par compte Google n'est pas chargée tant que vous n'avez pas répondu au bandeau de consentement.",
    googleRepli: "Le formulaire ci-dessous fonctionne sans Google.",
    revoirChoix: "Revoir mon choix",
    enSavoirPlus: "En savoir plus",
  },

  statuts: {
    PENDING: "En attente",
    CONFIRMED: "Confirmée",
    REJECTED: "Refusée",
    CANCELLED: "Annulée",
    COMPLETED: "Terminé",
    PLANNED: "À venir",
    FULL: "Complet",
  },

  energie: {
    ELECTRIC: "Électrique",
    HYBRID: "Hybride",
    GASOLINE: "Essence",
    DIESEL: "Diesel",
    LPG: "GPL",
  },

  recherche: {
    depart: "Départ",
    departExemple: "Namur",
    arrivee: "Arrivée",
    arriveeExemple: "Bruxelles",
    date: "Date",
    aPartirDe: "À partir de",
    places: "Places",
    peuImporte: "Peu importe",
  },

  carte: {
    nouveauConducteur: "Nouveau conducteur",
    aHeure: " à ",
    bagages: "Bagages",
    animaux: "Animaux",
    musique: "Musique",
    voirLeTrajet: "Voir le trajet",
    placesRestantes_une: "{n} place restante",
    placesRestantes_plusieurs: "{n} places restantes",
  },

  accueil: {
    titre: "Covoiturage d'entreprise, d'université et d'événement",
    description:
      "CoShift organise le covoiturage entre collègues et étudiants : publiez vos trajets, réservez ceux de votre organisation, réduisez les voitures sur la route.",
    accroche:
      "Le covoiturage pensé pour les entreprises, les universités et les événements. Moins de voitures sur la route, moins de frais, et des trajets partagés entre gens qui se connaissent.",
    heroPastille: "B2B & Campus",
    heroTitre: "Partagez vos trajets quotidiens avec",
    paginationLabel: "Pages de trajets",
    pagination: "Page {page} sur {total}",
    paginationTrajets: " · {n} trajets",
    argumentP1:
      "C'est toute l'idée. Le matin, sur le même axe, quatre voitures transportent quatre personnes. CoShift n'invente pas de nouvelle route : il remplit celles qui roulent déjà.",
    argumentP2:
      "Pour le conducteur, ce sont des frais partagés. Pour les passagers, un trajet direct sans correspondance. Pour l'organisation, trois places de parking libérées.",
    trajetsDisponibles: "Trajets disponibles",
    prochainsDeparts: "Les prochains départs proposés par les membres.",
    connectezVous:
      "Connectez-vous pour voir les trajets proposés près de chez vous.",
    rechercheDetaillee: "Recherche détaillée",
    invite:
      "Les trajets affichent le nom du conducteur et son horaire. Ils ne sont visibles qu'une fois votre compte créé.",
    chargementTrajets: "Chargement des trajets",
    aucunTrajet: "Aucun trajet publié pour le moment",
    aucunTrajetTexte:
      "Soyez le premier à proposer le vôtre — vos collègues le verront ici.",
    argumentTitre: "Quatre personnes, une voiture, un seul trajet",
    voirTrajets: "Voir les trajets proposés",
    enSavoirPlus: "En savoir plus sur CoShift",
    entreCollegues: "Entre collègues",
    entreColleguesTexte:
      "Vous partagez la route avec des personnes de votre organisation, pas avec des inconnus croisés sur Internet.",
    voitureDeMoins: "Une voiture de moins",
    voitureDeMoinsTexte:
      "Chaque place partagée retire un véhicule des embouteillages du matin et divise les frais d'autant.",
    adresseVerifiee: "Adresse vérifiée",
    adresseVerifieeTexte:
      "L'inscription passe par votre e-mail professionnel : c'est lui qui vous rattache à votre organisation.",
  },

  atouts: {
    titre: "Pourquoi passer par CoShift",
  },

  connexion: {
    titre: "Connexion",
    accroche: "Connectez-vous pour proposer ou trouver un trajet.",
    email: "Adresse e-mail",
    emailExemple: "prenom.nom@entreprise.be",
    motDePasse: "Mot de passe",
    afficherMotDePasse: "Afficher le mot de passe",
    masquerMotDePasse: "Masquer le mot de passe",
    oublie: "Mot de passe oublié ?",
    ou: "ou",
    pasDeCompte: "Pas encore de compte ?",
    erreurGoogle:
      "La fenêtre Google s'est fermée ou une erreur est survenue.",
    seSouvenir: "Se souvenir de moi",
    nouveauSurCoShift: "Nouveau sur CoShift ?",
    oublieTitre: "Mot de passe oublié",
    oublieAccroche:
      "Indiquez votre adresse pour recevoir un code de réinitialisation.",
    envoyerCode: "Envoyer le code",
    retourConnexion: "← Retour à la connexion",
    codeEnvoye:
      "Si un compte existe pour cette adresse, un code vient d'y être envoyé.",
    resetTitre: "Nouveau mot de passe",
    resetAccroche:
      "Saisissez le code reçu par e-mail, puis choisissez un nouveau mot de passe.",
    codeRecu: "Code reçu par e-mail",
    codeAide: "Six chiffres, envoyés à {email}. Valables une heure.",
    nouveauMotDePasse: "Nouveau mot de passe",
    nouveauMotDePasseAide: "Six caractères au minimum.",
    confirmerMotDePasse: "Confirmer le mot de passe",
    afficherLesMotsDePasse: "Afficher les mots de passe",
    changerMotDePasse: "Changer le mot de passe",
    demanderNouveauCode: "← Demander un nouveau code",
    motsDePasseDifferents: "Les deux mots de passe ne sont pas identiques.",
    motDePasseModifie:
      "Mot de passe modifié. Vous pouvez maintenant vous connecter.",
    googleInconnu:
      "Cet utilisateur n'existe pas. Veuillez créer un compte.",
    googleEchec: "Échec de la connexion avec Google. Veuillez réessayer.",
    identifiantsRefuses: "E-mail ou mot de passe incorrect.",
  },

  inscription: {
    titre: "Rejoindre CoShift",
    accroche: "Créez votre compte pour commencer à covoiturer.",
    prenom: "Prénom",
    prenomExemple: "Jean",
    nom: "Nom",
    nomExemple: "Dupont",
    emailPro: "E-mail professionnel",
    emailProExemple: "jean.dupont@entreprise.be",
    emailProAide:
      "C'est cette adresse qui vous rattache à votre organisation.",
    motDePasse: "Mot de passe",
    motDePasseAide: "6 caractères minimum.",
    motDePasseCourt: "Au moins 6 caractères.",
    confirmer: "Confirmer le mot de passe",
    confirmerDifferent: "Les deux mots de passe diffèrent.",
    corrigerChamps: "Corrigez les champs signalés avant de continuer.",
    accepterCgu: "J'ai lu et j'accepte les",
    accepterCguLien: "conditions générales",
    accepterEt: "et la",
    accepterConfidentialiteLien: "politique de confidentialité",
    accepterObligatoire:
      "Acceptez les conditions générales et la politique de confidentialité pour continuer.",
    creerMonCompte: "Créer mon compte",
    dejaInscrit: "Déjà inscrit ?",
    compteExiste: "Un compte existe peut-être déjà avec cette adresse.",
  },
};
/* Pas de `as const` : il figerait chaque valeur sur sa chaîne littérale, et
   `Traductions` exigerait alors des autres catalogues qu'ils répètent le texte
   français mot pour mot. Sans lui, le type retenu est la *forme* — les mêmes
   clés, des valeurs de type `string` —, ce qui est exactement la contrainte
   qu'on veut faire porter aux traductions. */
