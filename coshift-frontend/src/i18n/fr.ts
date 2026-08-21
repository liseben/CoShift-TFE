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
