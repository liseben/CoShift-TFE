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

  langue: {
    choisir: "Choose a language",
    actuelle: "Current language: {nom}",
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
