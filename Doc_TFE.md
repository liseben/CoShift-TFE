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