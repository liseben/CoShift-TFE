import type { Traductions } from "./index";

/**
 * English catalogue.
 *
 * <p>Typed against `Traductions`, which is derived from the French catalogue:
 * a missing or extra key fails the build. What the type cannot catch is a
 * string left in French — that only a reading catches.</p>
 *
 * <p>Register note: CoShift addresses colleagues and students, not customers.
 * The English wording stays plain and direct rather than corporate, to match
 * the French, which deliberately avoids marketing language.</p>
 */
export const en: Traductions = {
  commun: {
    chargement: "Loading…",
    chargementEnCours: "Loading",
    rechercher: "Search",
    envoyer: "Send",
    annuler: "Cancel",
    enregistrer: "Save",
    fermer: "Close",
    retour: "Back",
    precedent: "Previous",
    suivant: "Next",
    voirPlus: "Learn more",
    seConnecter: "Sign in",
    creerCompte: "Create an account",
    seDeconnecter: "Sign out",
    erreurReseau: "Could not reach the server. Please try again.",
    erreurGenerique: "Something went wrong. Please try again in a moment.",
    places_une: "{n} seat",
    places_plusieurs: "{n} seats",
    parPlace: "per seat",
  },

  champ: {
    obligatoire: "required",
  },

  message: {
    info: "Information",
    success: "Success",
    warning: "Warning",
    danger: "Error",
    fermer: "Dismiss this message",
  },

  langue: {
    choisir: "Choose a language",
    actuelle: "Current language: {nom}",
  },

  banniere: {
    defilante: "BE PART OF THE CHANGE WITH COSHIFT",
    accroche: "Mobility that is more",
    mot1: "simple",
    mot2: "green",
    mot3: "intelligent",
    mot4: "fast",
    mot5: "collaborative",
  },

  theme: {
    sombre: "Dark mode",
    passerEnClair: "Switch to light mode",
    passerEnSombre: "Switch to dark mode",
  },

  verification: {
    titre: "Verify your email address",
    accroche: "We sent a {n}-digit code to",
    votreAdresse: "your address",
    legende: "{n}-digit verification code",
    chiffre: "Digit {i} of {n}",
    activer: "Activate my account",
    pasRecu: "Didn't receive a code?",
    renvoyer: "Send a new code",
    renvoyerDans: "Resend in {s}s",
    codeIncomplet: "Enter all {n} digits of your verification code.",
    codeIncorrect: "Incorrect or expired code. Please try again.",
    codeRenvoye: "A new code has just been sent to your address.",
    envoiImpossible: "The code could not be sent. Please try again.",
  },

  actus: {
    titre: "Mobility news — carpooling, transport and sustainable travel",
    description:
      "CoShift's mobility news feed: carpooling, public transport, active travel and mobility policy in Belgium and across Europe.",
    entete: "Mobility briefing",
    accroche:
      "The essentials of transport and environment news, filtered for commuters.",
    filtrer: "Filter by topic",
    toutes: "All news",
    mobilite: "Mobility & transport",
    ecologie: "Environment & climate",
    entreprises: "Business & HR",
    technologie: "Tech & innovation",
    chargement: "Loading the news feed",
    aucunArticle: "No article in this topic",
    aucunArticleTexte: "Pick another filter to see the full feed.",
    voirTout: "See all",
    paginationLabel: "Article pages",
    retourAccueil: "Back to home",
    lire: "Read",
    serveurInjoignable: "Could not reach the server. Please try again shortly.",
  },

  article: {
    titreDefaut: "Mobility news",
    descriptionDefaut: "An article from CoShift's mobility news feed.",
    chargement: "Loading the article",
    introuvable: "This article could not be found.",
    retourActus: "Back to the news feed",
    toutesActus: "All news",
    source: "Source:",
    avertissement:
      "CoShift aggregates and summarises mobility news. The full article stays with its publisher.",
    lireSur: "Read the article on {source}",
    memeRubrique: "More in this topic",
  },

  trajets: {
    trouverTitre: "Find a ride",
    trouverAccroche: "Search the available rides and book your seat.",
    chargementRecherche: "Searching for rides",
    lancezRecherche: "Start a search",
    lancezRechercheTexte:
      "Enter at least a departure or arrival city to see the available rides.",
    aucunResultat: "No ride matches these criteria",
    aucunResultatTexte:
      "Widen the departure date or time, or remove the filter on the number of seats.",
    disponible_un: "{n} ride available",
    disponible_plusieurs: "{n} rides available",
    erreurRecherche: "Something went wrong during the search.",
  },

  photos: {
    habitacle:
      "Four smiling people seated in a car, seatbelts fastened.",
    depart: "A driver at the wheel and three passengers, about to set off.",
    rendezVous:
      "A passenger leans towards a car door to meet the driver.",
    application:
      "A passenger checks the CoShift app on their phone during the ride.",
  },

  apropos: {
    titre: "About — why CoShift exists",
    description:
      "CoShift started from one observation: most cars driving to work each morning carry a single person. Here is the reasoning behind the project and the choices it makes.",
    heroTitre: "Carpooling that starts at your organisation's door",
    heroAccroche:
      "CoShift connects the people making the same journey, at the same time, to the same place — their workplace, their campus, an event. Nothing more, and that is already a great deal.",

    constatTitre: "Where it started",
    constatP1:
      "Every morning, cars follow the same route, at the same hour, towards the same destination — with one person on board. That is not a choice: it is the absence of a practical alternative. The train does not serve the business park, the bus means two changes, and nobody knows which colleague sets off from the same neighbourhood.",
    constatP2:
      "Consumer carpooling platforms answer this need poorly. They are built for the exceptional journey — a Brussels to Paris run on a Friday evening — not for the twenty kilometres repeated twice a day, five days a week, with people you meet again at the coffee machine.",
    constatFort:
      "CoShift starts from the organisation, not from the journey. Moving that starting point is what changes everything else.",

    fonctionnementTitre: "How it works",
    fonctionnementLegende:
      "The meeting point is set by the driver when they post their ride.",
    etape1Titre: "You join your organisation",
    etape1Texte:
      "You sign up with your work email address. That is what links you to your company, your school or the event, and what assures your colleagues they are sharing the road with someone identified.",
    etape2Titre: "You post or you search",
    etape2Texte:
      "A driver registers their vehicle and states their route, their schedule and the number of seats. A passenger searches by city, by date and by departure time.",
    etape3Titre: "The driver accepts",
    etape3Texte:
      "A request is not a firm booking: the driver accepts or declines it, giving a reason for a refusal. They decide who gets into their car.",
    etape4Titre: "You share the road and the costs",
    etape4Texte:
      "The driver's phone number is only passed on once the booking is confirmed. The price covers a share of the costs, never a profit.",

    pourQui: "Who it is for",
    public1Titre: "Companies",
    public1Texte:
      "Car parks fill up, schedules look alike and journeys are duplicated. Grouping the commuters of a single site is the simplest lever available.",
    public2Titre: "Universities and colleges",
    public2Texte:
      "Thousands of students converge at the same hours on a campus rarely served like a city centre. Carpooling fills what the train does not.",
    public3Titre: "Festivals and trade fairs",
    public3Texte:
      "An event creates a traffic peak over a few hours. Organising sharing beforehand avoids an improvised car park in a field.",

    partisPris: "The choices we made",
    principe1Titre: "A closed circle, not a public marketplace",
    principe1Texte:
      "CoShift is not open to everyone. You carpool with members of your own organisation, which changes the relationship of trust entirely.",
    principe2Titre: "Sharing rather than travelling",
    principe2Texte:
      "A journey made by two people does not become greener: what counts is the car left in the garage. The whole interface highlights the seats actually shared.",
    principe3Titre: "The minimum of data",
    principe3Texte:
      "Work email address, name, possibly a phone number. No continuous location tracking, no advertising profiling, no resale.",

    origineTitre: "Where CoShift comes from",
    origineP1:
      "CoShift began as a final-year project, built end to end — interface, API, database. It is not a company with years behind it, and the page you are reading will not pretend otherwise.",
    origineP2:
      "What the project does claim is to work: posting a ride, searching, requesting a seat, the driver's acceptance and the tracking of bookings all genuinely function, with a real database behind them.",
    fait1: "Posting and searching for rides",
    fait2: "Booking, acceptance, reasoned refusal, cancellation",
    fait3: "Vehicle and profile management",
    fait4: "Address verification by six-digit code",
    fait5: "Mobility news feed",
    origineNote:
      "The dedicated space for organisations, ratings between members and internal messaging are the next pieces of work. Announcing them as available would sell better, but it would be untrue.",

    ctaTitre: "Ready to share the road?",
    ctaTexte:
      "Create your account with your work email address and see who around you already makes the same journey.",
    ctaVoirTrajets: "See the rides",
    ctaContact: "A question about rolling this out in your organisation?",
    ctaContactLien: "For organisations",
  },

  publier: {
    titre: "Offer a ride",
    accroche: "Share your journey and cut your carbon footprint.",
    publie: "Ride published",
    redirection: "Taking you to your dashboard…",
    chargementVehicules: "Loading your vehicles",
    vehiculesIndisponibles: "Your vehicles could not be loaded.",
    aucunVehicule: "No vehicle registered",
    aucunVehiculeTexte:
      "A ride is published with a vehicle. Register one first.",
    ajouterVehicule: "Add a vehicle",
    selectionnezVehicule: "Select a vehicle.",

    itineraire: "Route",
    villeDepart: "Departure city",
    villeDepartExemple: "Liège",
    villeArrivee: "Arrival city",
    villeArriveeExemple: "Brussels",
    pointDepart: "Exact pick-up point",
    pointDepartExemple: "Liège-Guillemins station",
    pointDepartAide: "Helps your passengers find you.",
    pointArrivee: "Exact drop-off point",
    pointArriveeExemple: "Brussels-Midi station",

    datePlaces: "Date and seats",
    dateHeure: "Departure date and time",
    dateHeureAide: "Two hours from now at the earliest.",
    placesProposees: "Seats offered",
    placesProposeesAide: "Up to {max}, your own seat excluded.",
    prixParPlace: "Price per seat (€)",
    prixParPlaceAide: "A share of the costs, not a profit.",

    vehicule: "Vehicle",
    choisirVehicule: "Choose the vehicle for this ride",
    placesVehicule: "{n} seats",

    details: "Details and preferences",
    description: "Description",
    descriptionAide: "Pick-up point, stops, particular constraints.",
    preferences: "Ride preferences",
    bagagesAcceptes: "Luggage allowed",
    animauxAcceptes: "Pets allowed",
    musiqueAutorisee: "Music allowed",
    discussionBienvenue: "Happy to chat",

    publierLeTrajet: "Publish the ride",
  },

  detail: {
    chargement: "Loading the ride",
    introuvable: "This ride could not be found.",
    retourRecherche: "Back to search",

    demandeEnvoyee: "Request sent",
    demandeEnvoyeeTexte:
      "The driver now has to accept it. Taking you to your bookings…",
    trajetAnnule: "Ride cancelled",
    trajetAnnuleTexte:
      "It no longer appears in searches. The bookings attached to it have been cancelled.",
    reservationImpossible: "The booking could not be recorded.",
    annulationImpossible: "The ride could not be cancelled.",

    bagagesAcceptes: "Luggage allowed",
    bagagesRefuses: "No luggage",
    animauxAcceptes: "Pets allowed",
    animauxRefuses: "No pets",
    musiqueAutorisee: "Music allowed",
    sansMusique: "No music",
    discussionBienvenue: "Happy to chat",
    trajetSilencieux: "Quiet ride",

    conducteur: "Driver",
    nouveauConducteur: "New driver",
    trajet_un: "{n} ride",
    trajet_plusieurs: "{n} rides",
    vehicule: "Vehicle",
    placesVehicule: "{n} seats",

    parPlace: " / seat",
    placesRestantes_une: "{n} seat left",
    placesRestantes_plusieurs: "{n} seats left",
    complet: "Full",
    nombreDePlaces: "Number of seats",
    retirerPlace: "Remove a seat",
    ajouterPlace: "Add a seat",
    total: "Total",
    reserver: "Book",
    indicationDemande:
      "Your request goes to the driver. It is only confirmed once they agree.",
    vousEtesConducteur: "You are the driver of this ride.",
    plusDeReservation: "This ride no longer accepts bookings.",
    plusDePlace: "There are no seats left.",

    annulerCeTrajet: "Cancel this ride",
    annulerIndication: "Pending requests and current bookings will be cancelled.",
    annulerTitre: "Cancel this ride?",
    annulerBouton: "Cancel the ride",
    annulerP1: "The ride {trajet} on {quand} will be removed from searches.",
    annulerP2:
      "All pending requests and already confirmed bookings will be cancelled with the reason “Ride cancelled by the driver”. This action is final.",
  },

  reservations: {
    titre: "My bookings",
    accroche: "Your seat requests and how they stand.",
    trouverTrajet: "Find a ride",
    chargement: "Loading your bookings",
    indisponibles: "Your bookings could not be loaded.",
    aucune: "No booking",
    aucuneTexte:
      "You have not booked a seat yet. Search for a ride to get started.",
    motif: "Reason:",
    nouveauConducteur: "New driver",
    reservee_une: "{n} seat booked",
    reservee_plusieurs: "{n} seats booked",
    voirLeTrajet: "View ride",
    annuler: "Cancel",
    annulationEchouee: "The cancellation failed.",
    annulerTitre: "Cancel this booking?",
    confirmerAnnulation: "Confirm cancellation",
    annulerTexte:
      "Your seat on the {trajet} ride will be made available again, and the driver will be notified. This action is final.",
  },

  demandes: {
    titre: "Requests received",
    accroche: "Passengers who have asked for a seat in your rides.",
    enAttente_une: "{n} request awaiting your answer.",
    enAttente_plusieurs: "{n} requests awaiting your answer.",
    chargement: "Loading requests",
    indisponibles: "The requests received could not be loaded.",
    aucune: "No request for now",
    aucuneTexte: "Booking requests on your rides will appear here.",
    nouveauPassager: "New passenger",
    trajet_un: "{n} ride",
    trajet_plusieurs: "{n} rides",
    place_une: "{n} seat",
    place_plusieurs: "{n} seats",
    accepter: "Accept",
    refuser: "Decline",
    operationEchouee: "The operation failed.",
    refuserTitre: "Decline this request?",
    motifDuRefus: "Reason for declining",
    motifAide:
      "Optional, but the passenger will see it. A short reason avoids misunderstandings.",
    motifExemple: "The car is already full for this ride.",
  },

  tableau: {
    membre: "CoShift member",
    administrateur: "Administrator",
    emailNonVerifie: "Email not verified",
    trajet_un: "{n} ride",
    trajet_plusieurs: "{n} rides",
    modifierProfil: "Edit profile",
    adresseNonVerifieeTitre: "Your address is not verified",
    adresseNonVerifieeTexte:
      "You will not be able to book a ride until your email address is confirmed.",

    sections: "Dashboard sections",
    vueEnsemble: "Overview",
    demandesRecues: "Requests received",
    mesVehicules: "My vehicles",
    mesDonnees: "My data",
    demandesEnAttente: "pending requests",

    chargement: "Loading your data",
    mesTrajets: "Rides I offer",
    nouveau: "New",
    aucunTrajetPropose: "You have not offered a ride to your colleagues yet.",
    mesReservations: "My bookings",
    toutVoir: "See all",
    aucuneReservationEnCours: "You have no booking in progress.",

    activite: "My sharing activity",
    publie_un: "ride published",
    publie_plusieurs: "rides published",
    reservation_une: "booking",
    reservation_plusieurs: "bookings",
    partagee_une: "seat actually shared",
    partagee_plusieurs: "seats actually shared",
    noteCo2:
      "Avoided CO₂ is not shown: computing it requires the distance of each ride, which the API does not provide yet.",

    modifierMonProfil: "Edit my profile",
    envoi: "Uploading…",
    changerPhoto: "Change photo",
    photoAide: "JPG or PNG, 2 MB maximum.",
    prenom: "First name",
    nom: "Last name",
    email: "Email address",
    emailAide: "Changing it will require a new verification.",
    telephone: "Phone number",
    telephoneAide: "Shared with your passengers once a booking is confirmed.",
  },

  vehicules: {
    titre: "My vehicles",
    accroche: "Register your vehicles so you can offer rides.",
    ajouter: "Add a vehicle",
    chargement: "Loading your vehicles",
    indisponibles: "Your vehicles could not be loaded.",
    aucun: "No vehicle registered",
    aucunTexte: "A ride is published with a vehicle. Start by declaring one.",
    places: "{n} seats",
    modifier: "Edit",
    supprimer: "Delete",
    suppressionImpossible: "This vehicle could not be deleted.",
    modifierTitre: "Edit vehicle",
    ajouterAction: "Add",
    marque: "Make",
    marqueExemple: "Renault",
    modele: "Model",
    modeleExemple: "Clio",
    plaque: "Number plate",
    plaqueExemple: "1-ABC-123",
    plaqueAide: "It identifies your vehicle uniquely.",
    nombreDePlaces: "Number of seats",
    nombreDePlacesAide: "Total seats, including your own.",
    motorisation: "Engine type",
    supprimerTitre: "Delete this vehicle?",
    supprimerTexte:
      "{vehicule} will be removed from your account. Rides already published with this vehicle are not deleted.",
  },

  donnees: {
    recuperer: "Get my data",
    recupererRef: "Articles 15 and 20 GDPR",
    recupererTexte:
      "Download everything CoShift holds about you: your account, your organisations, your vehicles, the rides you have offered and the bookings you have requested. The file is in JSON, readable by a machine as well as by a human.",
    recupererNote:
      "Other members' data is excluded. A ride booked with someone appears with its route and its schedule, never with their phone number: that is their data, not yours.",
    exporter: "Export my data",
    exportReussi: "Your export has been downloaded in JSON format.",
    exportEchoue: "The export could not be produced. Please try again shortly.",

    tiers: "My third-party services",
    tiersRef: "Article 7.3 GDPR",
    carteMapbox: "Animated map — Mapbox:",
    connexionGoogle: "Google sign-in:",
    autorisee: "allowed",
    refusee: "refused",
    choixExprime:
      "Choice made on {date}, on version {version} of the privacy policy.",
    pasEncoreRepondu:
      "You have not answered the banner yet. No third-party service is loaded.",
    revoirChoix: "Review my choice",

    documents: "What you signed up to",
    documentsRef: "Article 13 GDPR",
    docConfidentialite: "what is collected, why, and for how long",
    docCgu: "what each side commits to",
    docCookies: "what is stored in your browser",
    docMentions: "who publishes the service",

    supprimer: "Delete my account",
    supprimerRef: "Article 17 GDPR",
    supprimerIrreversible: "This action cannot be undone.",
    supprimerP1:
      "Your name, address, phone number, photograph and number plates are erased immediately, with no backup copy.",
    supprimerP2:
      "Your past rides and bookings are anonymised rather than deleted: they involve other members, whose history cannot be destroyed by your request. Once detached from you, they no longer identify anyone.",
    supprimerP3:
      "Your upcoming rides and current bookings are cancelled, with an explicit reason, so that nobody turns up for a meeting that will not happen.",
    supprimerBouton: "Permanently delete my account",
    confirmerTitre: "Confirm deletion",
    aucunRetour: "No way back",
    aucunRetourTexte:
      "Once started, neither you nor CoShift can recover your account. Consider exporting your data first if you want to keep it.",
    retapezAdresse: "Retype your email address to confirm",
    retapezAide:
      "The server requires the same confirmation: without it, the request is refused.",
    supprimerConfirmation: "Delete my account",
    suppressionEchouee: "The deletion did not go through. Please try again shortly.",
  },

  pages: {
    tableauDeBordTitre: "Dashboard",
    tableauDeBordDescription:
      "The rides you offer, your bookings and your vehicles.",
    reservationsTitre: "My bookings",
    reservationsDescription: "How your seat requests are progressing.",
    publierTitre: "Offer a ride",
    publierDescription:
      "Post a ride and share the costs with your colleagues.",
    detailTitre: "Ride details",
    detailDescription: "Schedule, driver, vehicle and seats available.",
    styleguideTitre: "Design system",
    styleguideDescription:
      "Reference board for CoShift's components and colours.",
  },

  nav: {
    accueil: "CoShift, home",
    entreprises: "For organisations",
    actus: "Mobility news",
    apropos: "About",
    blog: "Blog",
    telecharger: "Get the app",
    telechargerBientot: "Mobile app installation will be offered here.",
    monProfil: "My profile",
    connexion: "Sign in",
    ouvrirMenu: "Open menu",
    fermerMenu: "Close menu",
    bonjour: "Hello, {prenom}",
    tableauDeBord: "Dashboard",
    bonRetour: "Welcome back",
    bonRetourTexte: "Sign in to your CoShift space.",
    nouveau: "New here?",
    nouveauTexte: "Join tomorrow's commute.",
  },

  pied: {
    baseline: "Carpooling that starts at your organisation's door.",
    service: "The service",
    chercherTrajet: "Find a ride",
    proposerTrajet: "Offer a ride",
    espaceEntreprises: "For organisations",
    ressources: "Resources",
    actualites: "Mobility news",
    donneesOuvertes: "Open data",
    documentationApi: "API documentation",
    charteGraphique: "Design system",
    legal: "Legal information",
    mentions: "Legal notice",
    confidentialite: "Privacy policy",
    cgu: "Terms of use",
    cookies: "Cookies and trackers",
    revoirChoix: "Review my tracker choices",
    signaler: "Report content",
    projetAcademique:
      "Final-year project — the publisher's identification details are fictitious, as explained in the",
    projetAcademiqueLien: "legal notice",
  },

  consentement: {
    titre: "Two third-party services, your call",
    texte:
      "CoShift works without tracking you. Two optional features do rely on services based in the United States, which then receive your IP address: the animated map on the home page and the Google sign-in button.",
    texteFort: "Nothing loads until you answer.",
    toutRefuser: "Reject all",
    toutAccepter: "Accept all",
    serviceParService: "Choose service by service",
    replier: "Collapse",
    enregistrerChoix: "Save my choices",
    carteTitre: "Animated map — Mapbox",
    carteTexte:
      "Displays the map backdrop on the home page. Without it the page stays complete: only the animation is replaced by a flat colour.",
    googleTitre: "Google sign-in — Google Identity Services",
    googleTexte:
      "Enables the “Continue with Google” button on the sign-in screen. Signing in with an email address and password works without it.",
    pied: "Your choice is kept for six months and can be changed from the footer at any time. Details of what is stored:",
    piedLien: "cookies and trackers",
    googleDesactive:
      "Google sign-in is disabled: you have not allowed this third-party service.",
    googleNonCharge:
      "Google sign-in is not loaded until you answer the consent banner.",
    googleRepli: "The form below works without Google.",
    revoirChoix: "Review my choice",
    enSavoirPlus: "Learn more",
  },

  statuts: {
    PENDING: "Pending",
    CONFIRMED: "Confirmed",
    REJECTED: "Declined",
    CANCELLED: "Cancelled",
    COMPLETED: "Completed",
    PLANNED: "Upcoming",
    FULL: "Full",
  },

  energie: {
    ELECTRIC: "Electric",
    HYBRID: "Hybrid",
    GASOLINE: "Petrol",
    DIESEL: "Diesel",
    LPG: "LPG",
  },

  recherche: {
    depart: "From",
    departExemple: "Namur",
    arrivee: "To",
    arriveeExemple: "Brussels",
    date: "Date",
    aPartirDe: "From (time)",
    places: "Seats",
    peuImporte: "Any",
  },

  carte: {
    nouveauConducteur: "New driver",
    aHeure: " at ",
    bagages: "Luggage",
    animaux: "Pets",
    musique: "Music",
    voirLeTrajet: "View ride",
    placesRestantes_une: "{n} seat left",
    placesRestantes_plusieurs: "{n} seats left",
  },

  accueil: {
    titre: "Carpooling for companies, universities and events",
    description:
      "CoShift organises carpooling between colleagues and students: post your rides, book the ones offered inside your organisation, take cars off the road.",
    accroche:
      "Carpooling built for companies, universities and events. Fewer cars on the road, lower costs, and rides shared between people who already know each other.",
    heroPastille: "B2B & Campus",
    heroTitre: "Share your daily commute with",
    paginationLabel: "Ride pages",
    pagination: "Page {page} of {total}",
    paginationTrajets: " · {n} rides",
    argumentP1:
      "That is the whole idea. Every morning, along the same road, four cars carry four people. CoShift does not invent a new route: it fills the ones already running.",
    argumentP2:
      "For the driver, shared costs. For the passengers, a direct trip with no connection. For the organisation, three parking spaces freed up.",
    trajetsDisponibles: "Available rides",
    prochainsDeparts: "The next departures offered by members.",
    connectezVous: "Sign in to see the rides offered near you.",
    rechercheDetaillee: "Detailed search",
    invite:
      "Rides show the driver's name and departure time. They are only visible once your account has been created.",
    chargementTrajets: "Loading rides",
    aucunTrajet: "No ride posted yet",
    aucunTrajetTexte:
      "Be the first to offer one — your colleagues will see it here.",
    argumentTitre: "Four people, one car, a single journey",
    voirTrajets: "See the rides on offer",
    enSavoirPlus: "Learn more about CoShift",
    entreCollegues: "Among colleagues",
    entreColleguesTexte:
      "You share the road with people from your own organisation, not with strangers met online.",
    voitureDeMoins: "One car fewer",
    voitureDeMoinsTexte:
      "Every shared seat takes a car out of the morning traffic and splits the cost by as much.",
    adresseVerifiee: "Verified address",
    adresseVerifieeTexte:
      "You sign up with your work email address: that is what links you to your organisation.",
  },

  atouts: {
    titre: "Why CoShift",
  },

  connexion: {
    titre: "Sign in",
    accroche: "Sign in to offer or find a ride.",
    email: "Email address",
    emailExemple: "first.last@company.be",
    motDePasse: "Password",
    afficherMotDePasse: "Show password",
    masquerMotDePasse: "Hide password",
    oublie: "Forgot your password?",
    ou: "or",
    pasDeCompte: "No account yet?",
    erreurGoogle: "The Google window closed, or something went wrong.",
    seSouvenir: "Remember me",
    nouveauSurCoShift: "New to CoShift?",
    oublieTitre: "Forgotten password",
    oublieAccroche: "Enter your address to receive a reset code.",
    envoyerCode: "Send the code",
    retourConnexion: "← Back to sign-in",
    codeEnvoye:
      "If an account exists for this address, a code has just been sent to it.",
    resetTitre: "New password",
    resetAccroche:
      "Enter the code you received by email, then choose a new password.",
    codeRecu: "Code received by email",
    codeAide: "Six digits, sent to {email}. Valid for one hour.",
    nouveauMotDePasse: "New password",
    nouveauMotDePasseAide: "At least six characters.",
    confirmerMotDePasse: "Confirm password",
    afficherLesMotsDePasse: "Show passwords",
    changerMotDePasse: "Change password",
    demanderNouveauCode: "← Request a new code",
    motsDePasseDifferents: "The two passwords are not identical.",
    motDePasseModifie: "Password changed. You can now sign in.",
    googleInconnu: "This user does not exist. Please create an account.",
    googleEchec: "Google sign-in failed. Please try again.",
    identifiantsRefuses: "Incorrect email address or password.",
  },

  inscription: {
    titre: "Join CoShift",
    accroche: "Create your account to start carpooling.",
    prenom: "First name",
    prenomExemple: "Jane",
    nom: "Last name",
    nomExemple: "Doe",
    emailPro: "Work email address",
    emailProExemple: "jane.doe@company.be",
    emailProAide: "This address is what links you to your organisation.",
    motDePasse: "Password",
    motDePasseAide: "At least 6 characters.",
    motDePasseCourt: "At least 6 characters.",
    confirmer: "Confirm password",
    confirmerDifferent: "The two passwords do not match.",
    corrigerChamps: "Please fix the highlighted fields before continuing.",
    accepterCgu: "I have read and accept the",
    accepterCguLien: "terms of use",
    accepterEt: "and the",
    accepterConfidentialiteLien: "privacy policy",
    accepterObligatoire:
      "Accept the terms of use and the privacy policy to continue.",
    creerMonCompte: "Create my account",
    dejaInscrit: "Already registered?",
    compteExiste: "An account may already exist with this address.",
  },
};
