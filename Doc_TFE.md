# Journal de Bord TFE - Projet CoShift

## Introduction

Ce fichier documente au fur et à mesure les choix techniques, architecturaux et UX du projet CoShift. Il servira de base pour la rédaction du rapport final.

---

## 1. Architecture du Layout (Header, Footer, Navigation)

**Date :** [Date du jour]
**Objectif :** Créer une "coquille" (MainLayout) robuste pour accueillir l'utilisateur lambda.

**Choix UX / Design :**

- Utilisation du "Glassmorphism" (transparence et flou) pour le header afin de laisser transpirer la carte 3D Mapbox en dessous.
- Séparation claire entre "Le Blog" (communication interne CoShift) et "Actus Mobilité" (flux d'informations externe).
- Création de micro-interactions (effets de soulèvement, clics tactiles, cercles de remplissage) pour donner un aspect SaaS premium et moderne.

**Choix Techniques :**

- **CSS Grid** (`grid-template-columns: 1fr auto 1fr`) préféré à Flexbox pour le Header afin de garantir un centrage mathématique parfait des liens de navigation.
- **Click Outside :** Utilisation des hooks React (`useRef` et `useEffect`) pour fermer automatiquement les menus déroulants (Langue et Connexion) lorsque l'utilisateur clique en dehors, respectant les standards UX actuels.
- Utilisation de React Router (`<Outlet />`) pour ne recharger que le contenu central sans rafraîchir la page entière (Single Page Application).

**Problèmes résolus :**

- Conflit d'importation sur la route `/` (`HomePage`). Résolu en nettoyant les composants factices et en pointant vers le vrai fichier.

## 2. Système d'Actualités (Migration Front-to-Back)

**Date :** [Date du jour]
**Objectif :** Déporter l'intelligence du flux d'actualités du client (React) vers le serveur (Spring Boot) pour centraliser la donnée et optimiser les performances.

**Choix Architecturaux :**

- **Architecture Driven by Data :** Passage d'un mode "client-side fetch" (où chaque utilisateur appelait les APIs GNews/NewsData) à un mode "Server-side Ingestion". Le backend devient l'unique source de vérité (Single Source of Truth).
- **Persistance SQL :** Stockage des articles en base de données MySQL pour éviter les appels API redondants et coûteux, permettant également un historique plus long que ce que proposent les versions gratuites des APIs.
- **Automatisation :** Implémentation de `CommandLineRunner` (en attendant un Scheduler `@Scheduled`) pour rafraîchir le flux automatiquement au démarrage du serveur.

**Choix Techniques & Algorithmiques :**

- **Normalisation des Données :** Création d'un `ArticleService` capable d'unifier deux formats JSON différents (GNews et NewsData) en une seule entité `Article` standardisée.
- **Algorithme de Filtrage "Multi-niveaux" :**
  1. **Niveau API :** Utilisation de requêtes booléennes (`OR`, `AND`) complexes pour ne récupérer que le "bruit" pertinent.
  2. **Niveau Java (Dictionnaire) :** Analyse sémantique via des listes de mots-clés thématiques (Mobilité, Écologie, Entreprises, Technologie).
- **Gestion des cas d'erreur UX :** Mise en place de "Fallbacks" pour les descriptions manquantes ("Aucune description") et les images par défaut via Unsplash pour garantir une interface toujours propre.

**Problèmes résolus :**

- **Doublons :** Implémentation d'une vérification stricte en base de données via l'URL et le titre (ignorant la casse) pour éviter d'afficher deux fois la même information provenant de sources différentes.
- **Tri Chronologique :** Correction du bug de tri en sauvegardant les dates au format ISO (`YYYY-MM-DD`) pour permettre un tri SQL `DESC` fiable, tout en conservant un affichage localisé (`fr-FR`) côté React.
- **Sensibilité à la Casse :** Passage de l'algorithme de détection en "Full Lowercase" pour s'assurer que des mots comme "TEC" ou "E411" soient captés peu importe leur rédaction dans l'article original.

---

## 3. SEO et Performance (PWA)

**Date :** [Date du jour]
**Objectif :** Transformer le projet en une Progressive Web App (PWA) fluide.

**Choix Techniques :**

- **API Restful :** Séparation stricte entre le contrôleur PWA (`/api/pwa/articles`) et le reste du backend pour faciliter de futures intégrations mobiles.
- **CORS Policy :** Configuration fine du `@CrossOrigin` pour permettre une communication sécurisée entre le frontend (Vite) et le backend (Spring Boot).

## 4. Optimisation de l'affichage (Pagination Client-Side)
**Date :** 14 Mars 2026
**Objectif :** Améliorer la lisibilité du flux d'actualités en limitant le nombre d'éléments visibles simultanément.

**Choix UX :**
- Limitation de l'affichage à **5 articles par page** pour éviter l'effet "mur d'informations" (Information Overload).
- Implémentation d'une navigation intuitive (Précédent / Suivant) avec affichage conditionnel : le bouton "Précédent" n'apparaît que si l'utilisateur a quitté la première page.
- Système de "Reset" automatique : si l'utilisateur change de catégorie de news, la pagination revient automatiquement à la page 1 pour garantir la cohérence des données affichées.

**Choix Techniques :**
- Utilisation de la méthode JavaScript `.slice()` sur le tableau d'articles filtré. Cela permet de manipuler les données en mémoire sans refaire d'appel serveur, garantissant une navigation instantanée entre les pages.
- Calcul dynamique du nombre total de pages via `Math.ceil()` basé sur les résultats filtrés par catégorie.

**Amélioration Continue (Refactoring) :**
- Intégration d'un toggle "Afficher/Masquer le mot de passe" (icône SVG dynamique). Ce détail UX réduit considérablement les erreurs de frappe lors de la connexion, un aspect crucial pour la rétention utilisateur sur mobile.
- Refonte du design d'origine (Flat Design clair) vers le thème sombre Glassmorphism du projet, garantissant une cohérence d'interface de bout en bout.

**Module d'Inscription (Registration Flow) :**
- Développement d'un formulaire complet avec double validation des mots de passe côté client (React) pour minimiser les requêtes inutiles vers le serveur.
- Utilisation de `BCryptPasswordEncoder` côté Spring Boot pour garantir que les mots de passe ne sont jamais stockés en clair dans la base de données.
- Implémentation du pattern "Auto-Login" : dès la réussite de l'inscription, le backend génère et renvoie un JWT, permettant au frontend de connecter l'utilisateur immédiatement sans le forcer à repasser par la page de connexion.

**Intégration OAuth2 (Single Sign-On via Google) :**
- **Objectif :** Réduire la friction à l'inscription et augmenter le taux de conversion des utilisateurs en proposant le "Continuer avec Google".
- **Choix Technique (Frontend) :** Utilisation de la librairie officielle `@react-oauth/google` pour gérer le flux OAuth2 directement depuis le client React.
- **Sécurité et Flux (Architecture) :** 1. Le frontend React récupère le jeton d'identité Google (JWT sécurisé par Google) via la popup native.
  2. Ce jeton est ensuite envoyé au backend Spring Boot (`/api/auth/google`) qui se charge de le décoder et de vérifier sa signature cryptographique auprès des serveurs de Google (garantissant ainsi que l'email n'a pas été usurpé).
  3. Le backend crée ou connecte l'utilisateur "à la volée" et renvoie le token JWT natif de l'application CoShift.

  **Débogage et Résolution (Spring Security) :**
- **Problème :** Blocage des requêtes OAuth2 (Erreur 403) par le `SecurityFilterChain` malgré la configuration apparente.
- **Analyse :** Incohérence entre les chemins physiques des packages (`/api/controller/auth`) et les chemins des endpoints exposés (`@RequestMapping("/api/auth")`). Spring Security intercepte les requêtes basées sur l'URL d'appel (Endpoint), et non sur l'arborescence des fichiers.
- **Résolution :** Alignement strict des `requestMatchers` sur les routes de l'API REST (`/api/auth/**`), permettant au flux Google de traverser le filtre de sécurité avec succès et d'inscrire l'utilisateur en base de données.

**Optimisation des Performances (React & WebGL) :**
- **Problème :** Ralentissement extrême de la navigation (Single Page Application) et surcharge du thread principal du navigateur.
- **Diagnostic :** L'animation de la carte Mapbox (WebGL) à 60 FPS déclenchait un re-rendu complet du composant React à cause de l'utilisation de `useState` dans la boucle `requestAnimationFrame`. La file d'attente du Virtual DOM était saturée.
- **Résolution :** Refactorisation du composant `MapBackground`. Remplacement des états React par une référence mutative (`useRef<MapRef>`). L'animation interagit désormais directement avec l'instance native de Mapbox via `map.setBearing()` et `source.setData()`, contournant totalement le cycle de vie de React et libérant le thread principal pour une navigation instantanée.

**Finalisation du flux d'Authentification et Routage :**
- Configuration du routage global via `react-router-dom` avec un `MainLayout` persistant.
- Sécurisation des routes privées côté Frontend (protection du `/dashboard` via redirection conditionnelle).
- Optimisation de l'UX post-connexion : redirection asynchrone vers la page d'accueil (`/`) permettant l'initialisation complète de l'`AuthContext` avant le changement de vue, évitant ainsi les "flashs" d'interface non connectée.

---

## 5. Validation de Compte par Email (F7)

**Date :** 07 Juillet 2026
**Objectif :** Implémenter le flux de validation de compte (F7 du cahier des charges) : à l'inscription, un code à 6 chiffres est envoyé par email. L'utilisateur doit le saisir pour activer son compte.

**Choix Architecturaux :**

- **Découplage Inscription / Connexion :** L'inscription (`POST /api/auth/register`) ne renvoie plus de JWT. Elle génère un code, l'envoie par email et retourne un simple message. Le token est uniquement fourni après validation du code via `POST /api/auth/verify-email`. Ce pattern garantit qu'aucun compte non validé n'accède à l'application.
- **Google OAuth2 :** Les utilisateurs inscrits via Google sont marqués `emailVerified = true` d'emblée — Google ayant déjà vérifié l'adresse, une double vérification serait une friction inutile.
- **Spring Security `isEnabled()` :** L'entité `User` retourne désormais `emailVerified` dans `isEnabled()`, ce qui bloque automatiquement les comptes non activés au niveau du filtre Spring Security, sans logique métier supplémentaire.

**Choix Techniques :**

- **`SecureRandom`** : Génération du code 6 chiffres via `SecureRandom` (cryptographiquement sûr, contrairement à `Math.random()`).
- **`@Async` + Spring Mail** : L'envoi de l'email est asynchrone (`@Async` activé via `@EnableAsync`) pour ne pas bloquer la réponse HTTP. Utilisation de `JavaMailSender` avec Gmail SMTP (STARTTLS).
- **Email HTML Premium** : Le template d'email est construit en Java (`String.formatted()`) avec un design dark glassmorphism cohérent avec la charte CoShift — pas de dépendance externe (Thymeleaf, etc.) pour garder le backend léger.
- **Migration Flyway V3** : Ajout des colonnes `email_verified`, `verification_code`, `verification_code_expiry` avec `DEFAULT TRUE` pour les utilisateurs existants (rétrocompatibilité).
- **Minuteur de renvoi (cooldown 60s)** : Côté frontend, un minuteur `useEffect` décrémentiel empêche les spam de renvoi de code, une pratique standard dans les flows d'authentification modernes.

**UX & Design (VerificationPage) :**

- Saisie case par case (6 inputs séparés) avec navigation clavier automatique (focus suivant à chaque chiffre, retour arrière vers le précédent).
- **Copier-coller intelligent** : L'événement `onPaste` intercepte le code copié depuis l'email et le distribue automatiquement dans les 6 cases en une seule action.
- Séparateur visuel entre les groupes de 3 (style Stripe), animation `slideUp` à l'entrée, `pulse` sur l'icône email.
- Design dark glassmorphism : cohérence avec la charte graphique globale (fond #0f1a1c, accents #60a5fa, backdrop-filter blur).

**Problèmes résolus :**

- **Bug localStorage** : Incohérence entre `coshift_token` (AuthContext) et `token` (DashboardPage) — la requête PUT profil renvoyait une 401 systématique. Résolu par alignement de la clé.

---

## 6. Profil Utilisateur Complet (F8/F9)

**Date :** 07 Juillet 2026
**Objectif :** Finaliser les fonctionnalités F8 (consulter son profil) et F9 (modifier son profil) avec photo de profil uploadable, numéro de téléphone et statistiques utilisateur.

**Choix Architecturaux :**

- **Stockage local des photos** : Les avatars sont stockés sur le serveur dans `uploads/avatars/` avec un UUID comme nom de fichier (évite les collisions et les injections de chemin). En production, ce dossier serait remplacé par un service de stockage cloud (AWS S3, Cloudinary).
- **Exposition via `WebMvcConfigurer`** : Plutôt que de copier les fichiers dans `src/main/resources/static/`, on les sert depuis un dossier externe grâce à `addResourceHandlers()`. Cela permet de persister les uploads entre les redémarrages du serveur sans les inclure dans le JAR.
- **Nettoyage de l'ancienne photo** : Avant de sauvegarder la nouvelle photo, le serveur supprime l'ancienne (`Files.deleteIfExists`) si elle était hébergée localement — évite l'accumulation de fichiers orphelins.

**Choix Techniques :**

- **Validation côté serveur** : Vérification du `ContentType` (JPEG/PNG uniquement) et de la taille (max 2 Mo) avant traitement — la validation côté client n'est jamais suffisante.
- **Statistiques profil** : `averageRating` (note moyenne) et `tripsCount` (trajets effectués) sont calculés côté backend et exposés dans `/api/users/me`. Ces champs seront mis à jour automatiquement lors des futures fonctionnalités (notation F22/F31, marquage trajet terminé F21).

**UX & Design (Dashboard refonte) :**

- **Refonte complète en dark glassmorphism** : Le Dashboard précédent était en thème clair (`background: white`), incohérent avec le reste de l'application. Tout le CSS a été réécrit dans la charte sombre du projet.
- **En-tête profil enrichi** : Affichage de l'email, du téléphone, de la note moyenne (étoile), du nombre de trajets et d'un badge "Email non vérifié" si le compte n'est pas encore activé.
- **Animations** : `fadeSlideUp` sur les widgets avec `animation-delay` décalé (0ms, 50ms, 100ms) pour un effet de cascade visuel — technique courante dans les dashboards SaaS premium.
- **Modale dark premium** : La modale d'édition reprend le style glassmorphism du reste de l'app (fond `#131f22`, bordures `rgba(255,255,255,0.1)`), avec upload photo en temps réel et champ téléphone.