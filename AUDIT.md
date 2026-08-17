# Audit CoShift — backlog de finalisation

Audit réalisé le **17 août 2026** sur le commit `67d5bf1` (branche `main`).
Référentiel : *Cahier de charges fonctionnel V2*.
Rapport complet et lisible : voir l'artefact « Audit CoShift ».

État de compilation :

| Module | Commande | À l'audit | Aujourd'hui |
|---|---|---|---|
| `coshift-backend` | `mvn compile` | ✅ OK | ✅ OK |
| `coshift-frontend` | `npm run build` | ❌ 3 erreurs | ✅ OK (517 modules) |
| `coshift-backend` | `mvn spring-boot:run` | ❌ ne démarrait pas | ✅ **démarre en 7,7 s** sur `:8081` |
| `coshift-backend` | `mvn test` | ❌ ne démarre pas | ✅ **BUILD SUCCESS** |

Environnement local : WAMP / MySQL 5.7.23 sur `:3306`, backend sur `:8081`
(le `:8080` est occupé par PEMHTTPD, la console web de PostgreSQL).

---

## Phase 1 — Remettre le projet en état de marche ✅

Branche `fix/phase-1-deblocage`, 4 commits.

- [x] **A1** — `npm run build` échouait (3 erreurs TS). — commit `6af1c3a`
      `VehiclePage.tsx:10` et `SearchTripsPage.tsx:45` : `JSX.Element` → `React.ReactElement`
      (React 19 a retiré le namespace global `JSX`).
      `AuthContext.tsx:1` : import `React` inutilisé (`noUnusedLocals`).
- [x] **A2** — `GlobalExceptionHandler` : `@ExceptionHandler(Exception.class)` interceptait
      tout et renvoyait 500 « Une erreur inattendue est survenue ». — commit `8a5da59`
      → 3 exceptions métier créées (`ResourceNotFoundException`, `ConflictException`,
      `BadRequestException`), handlers typés 400 / 401 / 403 / 404 / 409 / 413,
      `Exception` en dernier recours **avec log de la trace** (elle disparaissait sans trace).
- [x] **A3** — `WebMvcConfig` pointait sur `uploads/uploads/` alors que `UserController`
      écrit dans `uploads/avatars/`. — commit `b166f6e`
- [x] **A4** — `authenticateWithGoogle()` émettait un JWT sans vérifier `emailVerified`
      (contournement de F7) ; `JwtAuthenticationFilter` n'appelait jamais `isEnabled()`
      et remontait en 500 brut sur un jeton malformé. — commit `3efa19d`

### A21 — Le seul test du projet ne démarrait pas ✅ **Corrigé**

H2 en portée test + `application-test.properties` + `@ActiveProfiles("test")`.
`mvn test` → `Tests run: 1, Failures: 0, Errors: 0` / `BUILD SUCCESS`.
La planification du flux d'actualités a été externalisée (`app.news.initial-delay`)
pour qu'elle ne se déclenche pas au milieu de la suite.

Diagnostic d'origine :

`mvn test` échoue sur `Access denied for user '${DB_USERNAME}'@'localhost'` : le placeholder
n'est **pas résolu**. `CoshiftBackendApplication.main()` charge le `.env` via `java-dotenv`,
mais `@SpringBootTest` ne passe pas par `main()` — et `spring.config.import=optional:file:.env`
est ignoré silencieusement, Spring ne reconnaissant pas l'extension `.env` comme format de
configuration.

Conséquence : `contextLoads` n'a jamais tourné, et **aucun test ne peut être écrit** tant que
ce point n'est pas réglé. Bloque toute la phase 3 (CT6).

→ Ajouter H2 en portée `test` + `src/test/resources/application-test.properties`,
et annoter la classe de test avec `@ActiveProfiles("test")`.

## Phase 2 — Fermer la boucle métier ✅

- [x] **A5** — ✅ **Corrigé.** `BookingService` + `BookingController` créés.
      F27 (réserver), F19 (demandes reçues), F20 (accepter / refuser avec motif),
      F29 (annuler), F30 (mes réservations). Écrans frontend livrés.
- [x] **A6** — ✅ **Corrigé.** Places bornées par `vehicule.seats - 1` à la publication,
      décomptées à l'acceptation, restituées à l'annulation ; bascule `PLANNED ↔ FULL`.
- [x] **A7** — ✅ **Corrigé.** La recherche impose `departureTime >= now`, `availableSeats > 0`
      et exclut le conducteur courant. `TripService.closePastTrips()` clôture les trajets échus
      toutes les 15 min.
- [x] **A14** — ✅ **Corrigé.** Route `/trips/:uuid` + `TripDetailPage`, route `/bookings`,
      Dashboard branché sur `/api/trips/mine` et `/api/bookings/*`, paramètre `?tab=` pris en
      compte. *(Reste : la recherche rapide de `HomePage` n'est toujours pas branchée.)*
- [x] **F16** — ✅ **Corrigé.** Règle des 2 h appliquée côté serveur.
- [x] **F18** — ✅ **Corrigé.** L'annulation d'un trajet annule ses réservations en cascade.

### Vérification de bout en bout

Scénario rejoué contre le backend en fonctionnement, toutes les assertions passent :
publication bornée par la capacité du véhicule, règle des 2 h, exclusion de ses propres
trajets de la recherche, refus de réserver sa propre annonce, refus de sur-réserver, doublon
de demande en 409, décompte `3 → 1` à l'acceptation, restitution `1 → 3` à l'annulation,
double acceptation en 409, cloisonnement passager/conducteur en 403, cascade F18.

Le numéro de téléphone n'est exposé qu'après confirmation, conformément à F13bis.

## Phase 3 — Contraintes techniques du TFE

- [ ] **CT6** — Un seul test (`contextLoads`), mais il **tourne enfin** (A21 corrigé).
      Objectif 70 % : JUnit + MockMvc sur inscription, vérification email, publication de trajet,
      réservation. Ajouter JaCoCo. Le scénario de bout en bout de la phase 2 est déjà écrit
      et peut servir de base aux tests d'intégration.
- [ ] **CT4 / F28 / N2** — Stripe : clés présentes dans `.env`, zéro ligne de code.
      Payment intent, webhook, remboursement (barème F29), commission plateforme.
- [ ] **CT5 / F3** — i18n : le sélecteur FR/EN/NL de `MainLayout.tsx` ne change qu'un libellé.
      Intégrer `react-i18next` et externaliser les chaînes.
- [ ] **CT2 / A13** — Aucun `@PreAuthorize` ni `hasRole` dans le projet : `Role` est déclaré mais
      jamais appliqué. Back-office, 2FA, journal d'audit (A1–A11) à construire.
- [ ] **N5 / F14 / F15** — RGPD : ni export, ni suppression de compte.
- [ ] **A8** — Aucune limitation de tentatives sur `/login`, `/verify-email`,
      `/resend-verification`. Le cooldown 60 s est purement frontend
      (`VerificationPage.tsx:90`). Code à 6 chiffres valable 24 h.
      → Compteur de tentatives, délai serveur, validité ramenée à 15 min.
- [ ] **A11** — `UserService.java:28-33` : changement d'email sans re-vérification, alors que
      `DashboardPage.tsx:328` promet un email de confirmation. `emailVerified` reste à `true`.

## Phase 4 — Rendre le projet défendable

- [x] **A10** — ✅ **Corrigé.** `V1__Baseline_schema.sql` reconstruit les 7 tables depuis une
      base vide, `ddl-auto` est passé en `validate`, et Flyway est réellement branché.
      commits `7ded625`, `12f4760`, `05ac4e2`, `7b5b04e`.
- [x] **A23** — ✅ **Corrigé.** *(découvert en corrigeant A10)* **Flyway n'avait jamais tourné.**
      `flyway_schema_history` n'existait pas et les 5 migrations n'avaient jamais été exécutées :
      Spring Boot 4 a modularisé ses auto-configurations, et sans
      `org.springframework.boot:spring-boot-flyway` aucun bean Flyway n'est créé — **en silence**.
      Tout le schéma provenait en réalité de `ddl-auto=update`.
      → `flyway-core` remplacé par `spring-boot-starter-flyway`. commit `7ded625`.
- [ ] **A9** — `@CrossOrigin(origins = "${app.cors.allowed-origins:*}")` sur les 4 contrôleurs :
      la propriété n'existe pas → `*`, ce qui prime sur la liste blanche de
      `SecurityConfiguration.java:64-76`. → Supprimer les annotations.
- [ ] **A12** — `ArticleService.java:172-181` : `findAll()` + Levenshtein sur toute la table pour
      chaque article candidat, toutes les 6 h (O(n²)).
      → Fenêtre glissante 30 jours + projection sur `normalizedTitle`.
- [ ] **A16** — `application.properties` : `spring.security.user.password=admin123` en clair,
      `show-sql=true`, URLs `localhost` codées en dur, `devtools` en runtime, aucun profil `prod`.
- [ ] **A17** — jjwt 0.11.5 en fin de vie, API dépréciée. Migrer en 0.12.x.
- [ ] **A15** — Pas de PWA : ni manifeste, ni service worker.
      `index.html` est le gabarit Vite (`lang="en"`, titre `coshift-frontend`, favicon vite.svg).
      `MainLayout.tsx:127` : bouton « Téléchargez l'App » → `alert()`.
- [ ] **N6** — Ni Swagger/OpenAPI, ni versionnage `/api/v1`.
- [ ] **A22** — `LoginPage.tsx:108` appelle `POST /api/auth/forgot-password`, **endpoint
      inexistant** côté backend. La réinitialisation de mot de passe échoue silencieusement.
      → Soit implémenter le flux (réutiliser le mécanisme de code de F7), soit retirer le lien.
- [ ] **A24** — La base tourne sur **MySQL 5.7.23** (WAMP), alors qu'Hibernate 7 et
      Connector/J 9.5 visent MySQL 8. Fonctionne aujourd'hui, mais hors matrice supportée.
      WAMP embarque un MySQL 8.3.0 : basculer dessus sécuriserait la stack.
- [ ] **A18 / CT1** — Branche unique `main`, aucune PR, aucune CI.
      → `develop` + `feature/*`, pull requests, workflow GitHub Actions build + tests.
- [ ] **A19** — `Organization` modélisée mais jamais rattachée à un trajet (V5 a rendu la colonne
      nullable). Le positionnement B2B, qui distingue CoShift, n'est pas démontrable.
- [ ] **A20** — `V3__...sql:2` : `email_verified NOT NULL DEFAULT TRUE` — tout insert hors code
      Java crée un compte pré-validé. → Migration repassant la colonne en `DEFAULT FALSE`.

---

## Couverture fonctionnelle

| Bloc | Livré | Partiel | Total |
|---|---|---|---|
| Visiteur (F1–F4) | 1 | 1 | 4 |
| Utilisateur membre (F5–F15) | 4 | 1 | 12 |
| Conducteur (F14bis, F16–F24) | 4 | 1 | 9 |
| Passager (F25–F35) | 4 | 0 | 11 |
| Administration (A1–A11, A01–A06) | 0 | 0 | 17 |
| Non-fonctionnel et contraintes (N1–N6, CT1–CT6) | 2 | 4 | 12 |
| **Total** | **15** | **7** | **65** |

Pleinement livrées : F4, F5, F6, F7, F8, F16, F18, F19, F20, F25, F26, F27, F29, F30, CT3.
N1 est désormais satisfaite (index de recherche posés dans le baseline).

## Écarts à corriger dans `Doc_TFE.md`

- Section 3 annonce une PWA qui n'existe pas (aucun manifeste ni service worker).
- Section 5 affirme que les comptes Google sont marqués `emailVerified = true` à l'inscription.
  Le code actuel refuse purement et simplement l'inscription via Google
  (`AuthenticationService.java:66-70`).
