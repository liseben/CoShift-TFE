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