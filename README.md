# CoShift

Plateforme de covoiturage domicile-travail pour les entreprises, les universités et les événements.

Là où les services grand public mettent en relation des inconnus sur de longues distances, CoShift s'adresse à des personnes qui partagent déjà un lieu et des horaires : des collègues, des étudiants d'un même campus, des participants d'un même événement. Le trajet quotidien, court et répété, est celui qui pèse le plus dans les émissions liées à la mobilité, et c'est celui que le covoiturage classique couvre le moins bien.

> **Version alpha.** Produit Minimum Viable. Toutes les fonctionnalités prévues ne sont pas réalisées ; celles qui figurent ci-dessous fonctionnent. Voir [Périmètre](#périmètre) et [Feuille de route](#feuille-de-route).

Travail de fin d'études — développement d'application web, 2025-2026.

---

## Sommaire

- [Périmètre](#périmètre)
- [Pile technique](#pile-technique)
- [Installation](#installation)
- [Comptes de démonstration](#comptes-de-démonstration)
- [Documentation de l'API](#documentation-de-lapi)
- [Structure du projet](#structure-du-projet)
- [Choix d'architecture](#choix-darchitecture)
- [Feuille de route](#feuille-de-route)

---

## Périmètre

### Authentification

- Inscription avec acceptation explicite des conditions générales. La date et la version acceptées sont conservées : sans elles, il serait impossible d'établir à quoi une personne a consenti.
- Activation du compte par code à six chiffres reçu par courriel. Tant que l'adresse n'est pas prouvée, la connexion est refusée — y compris par le canal Google.
- Connexion classique et connexion par compte Google, dont le jeton d'identité est vérifié auprès de Google avant toute émission de session.
- Mot de passe oublié : code valable une heure, à usage unique. La réponse est identique que le compte existe ou non, pour ne pas transformer ce point d'entrée en annuaire.
- Freinage des tentatives : cinq échecs, puis quinze minutes de blocage, compté par couple *adresse IP × compte visé*.

### Profil

- Consultation et modification de l'identité, de l'adresse et du téléphone.
- Photographie de profil, dont le type et la taille sont contrôlés côté serveur.
- Changement d'adresse : le compte repasse en attente de vérification et un code part vers la nouvelle adresse.

### Droits des personnes (RGPD)

Ces trois droits sont exerçables depuis le compte, sans formulaire ni délai :

- **Article 15 et 20** — export de ses données au format JSON, accompagné de la liste de ce qui en est volontairement absent et pourquoi.
- **Article 17** — suppression du compte. L'opération annule d'abord les trajets et réservations à venir, pour que personne ne se présente à un rendez-vous qui n'aura pas lieu, puis anonymise les champs identifiants.
- **Article 7** — consentement aux services tiers, horodaté, versionné, révocable en un clic depuis le pied de page. Aucun fond cartographique ni script Google n'est chargé avant une réponse explicite.

### Trajets

- Recherche par villes, date et nombre de places. Seuls remontent les trajets à venir, disposant de places, et dont on n'est pas le conducteur.
- Publication d'un trajet, avec véhicule, places, participation aux frais et préférences de voyage. Le nombre de places est borné par la capacité réelle du véhicule, et le départ doit être à deux heures au moins.
- Demande de réservation, acceptation ou refus motivé, annulation par le passager. Les places sont décomptées à l'acceptation et restituées à l'annulation.
- L'annulation d'un trajet annule ses réservations en cascade, chacune portant son motif.
- Le téléphone du conducteur n'est révélé qu'une fois la réservation confirmée.

### Interface

- Tableau de bord à quatre onglets : vue d'ensemble, demandes reçues, véhicules, données personnelles.
- Rubrique Actualités, alimentée par un flux agrégé et classé côté serveur.
- Français et anglais, sur l'interface comme sur les messages d'erreur et les courriels du serveur.
- Thème clair et thème sombre, avec respect de la préférence du système.
- Planche de composants de référence sur `/styleguide`.

---

## Pile technique

| Couche | Technologie |
|---|---|
| Backend | Spring Boot 4.0.2 · Java 21 |
| Sécurité | Spring Security · JWT (jjwt) · OAuth 2.0 Google |
| Persistance | Spring Data JPA · Hibernate · MySQL · migrations Flyway |
| Documentation d'API | springdoc-openapi 3 (OpenAPI 3.1 / Swagger UI) |
| Supervision | Spring Boot Actuator |
| Frontend | React 19 · TypeScript 5.9 · Vite 7 · React Router 7 |
| Cartographie | Mapbox GL · Turf |
| Courriel | Spring Mail · SMTP |

---

## Installation

### Prérequis

- **Java 21** et Maven (le wrapper `mvnw` est fourni, aucune installation de Maven n'est nécessaire)
- **Node.js 20** ou supérieur
- **MySQL 8** (MySQL 5.7 fonctionne mais sort de la matrice supportée par Hibernate 7)
- Un compte SMTP pour l'envoi des courriels de vérification

### 1. Base de données

```sql
CREATE DATABASE coshift_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Le schéma est construit par Flyway au premier démarrage. Aucun script à jouer à la main.

### 2. Backend

```bash
cd coshift-backend
cp .env.example .env
```

Renseigner dans `.env` :

| Variable | Rôle |
|---|---|
| `DB_USERNAME`, `DB_PASSWORD` | Compte MySQL local |
| `JWT_SECRET_KEY` | Clé de signature, Base64, 256 bits — `openssl rand -base64 32` |
| `JWT_EXPIRATION` | Validité du jeton en millisecondes (`86400000` = 24 h) |
| `MAIL_USERNAME`, `MAIL_APP_PASSWORD` | SMTP. Sur Gmail, un *mot de passe d'application*, pas celui du compte |
| `GNEWS_API_KEY`, `NEWSDATA_API_KEY` | Flux d'actualités — formules gratuites suffisantes |
| `SERVER_PORT` | Port d'écoute, `8080` par défaut |

```bash
./mvnw spring-boot:run
```

L'API répond sur `http://localhost:8080`.

### 3. Frontend

```bash
cd coshift-frontend
cp .env.example .env
npm install
npm run dev
```

Renseigner dans `.env` :

| Variable | Rôle |
|---|---|
| `VITE_API_URL` | URL du backend — doit suivre `SERVER_PORT` |
| `VITE_MAPBOX_TOKEN` | Jeton public Mapbox pour le fond cartographique |

L'interface répond sur `http://localhost:5173`.

> Les variables `VITE_*` sont exposées au navigateur : n'y placez jamais de secret.

### Vérifier que tout tourne

```bash
curl http://localhost:8080/actuator/health     # doit renvoyer "status":"UP"
```

---

## Comptes de démonstration

Les migrations chargent un jeu de données de développement : 12 organisations, 120 comptes, 112 véhicules, 150 trajets et 258 réservations. Les organisations sont fictives ; les villes, marques et modèles sont réels, pour que distances et capacités restent crédibles.

| Adresse | Mot de passe | Intérêt pour la démonstration |
|---|---|---|
| `julie.lecomte@salon-mobilite.be` | `1234` | 8 trajets publiés, 1 véhicule, 2 réservations |
| `charlotte.guerin@u-basse-meuse.be` | `1234` | 3 véhicules, 7 trajets |
| `margaux.gautier@val-vert.be` | `1234` | 5 trajets et 5 réservations — les deux rôles |

98 des 122 comptes sont vérifiés. Les autres le sont volontairement restés, afin d'éprouver l'écran de saisie du code et le refus de connexion : `michael.leclercq@verhaegen.be` en fait partie et renvoie donc un `403`.

> **Réservé au développement.** Ce jeu de données et ces mots de passe n'ont pas vocation à être déployés.

---

## Documentation de l'API

Swagger UI, généré depuis les contrôleurs et leurs annotations de validation :

```
http://localhost:8080/swagger-ui.html
```

La documentation est produite par le code lui-même et ne peut donc pas diverger de lui. Elle est **désactivée sur le profil `prod`** : elle décrit l'intégralité de la surface d'attaque et dispenserait un attaquant de toute reconnaissance.

Autres points d'entrée publics :

| Adresse | Contenu |
|---|---|
| `/api/open-data` | Jeu de données ouvertes agrégées, sous Licence Ouverte 2.0 |
| `/api/open-data/villes.csv` | Même jeu, au format tabulaire |
| `/sitemap.xml` | Plan du site |
| `/actuator/health` | Sonde de disponibilité |

---

## Structure du projet

```
coshift-backend/
  src/main/java/com/coshift/api/
    config/        Sécurité, CORS, OpenAPI, internationalisation
    controller/    Points d'entrée REST
    dto/           Objets d'échange, avec leurs contraintes de validation
    entity/        Modèle persistant
    exception/     Exceptions métier et traduction en réponses HTTP
    repository/    Accès aux données
    security/      JWT, freinage des tentatives, journal de sécurité
    service/       Règles métier
  src/main/resources/
    db/migration/  Migrations Flyway — seule autorité sur le schéma
    messages_*.properties

coshift-frontend/
  src/
    components/    Composants réutilisables, dont une bibliothèque d'interface
    context/       Authentification, langue, thème, consentement
    i18n/          Catalogues français et anglais
    pages/         Un dossier par écran
    styles/        Jetons de conception, base, composants
```

---

## Choix d'architecture

**Le dépôt fait autorité sur le schéma.** Flyway reconstruit la base depuis zéro et Hibernate est en `ddl-auto=validate` : il vérifie que les entités correspondent au schéma, sans jamais le modifier de lui-même.

**L'internationalisation est typée, sans bibliothèque.** Le catalogue français définit la forme, l'anglais doit s'y conformer. Une clé oubliée est une erreur de compilation, pas un texte manquant découvert en production. Le besoin étant étroit, ajouter une dépendance pour une centaine de lignes demandait une meilleure raison.

**Le journal de sécurité est séparé du journal applicatif.** Mêlés au reste, les événements de sécurité se noient : personne ne les lit et aucune règle de détection ne s'y applique. Isolés, ils forment un fichier dont chaque ligne mérite d'être regardée. Conservation bornée à douze mois, parce qu'ils contiennent des données personnelles.

**Le freinage des tentatives compte par IP *et* par compte.** Compter par compte seul permettrait de verrouiller celui d'un tiers en échouant volontairement cinq fois. Compter par IP seule bloquerait tout un site derrière une même sortie NAT — cas courant en entreprise. La combinaison arrête l'attaque réelle sans pénaliser les voisins.

**L'effacement anonymise plutôt qu'il ne supprime.** Un covoiturage passé engage deux personnes : effacer la ligne du conducteur priverait le passager de son propre historique. Les champs identifiants sont donc écrasés sur place. Ce qui subsiste — un trajet rattaché à un participant sans nom ni coordonnées — ne se rapporte plus à une personne identifiable.

---

## Feuille de route

Fonctionnalités prévues, non réalisées dans cette version alpha :

- [ ] Notifications par courriel : demande reçue, acceptation, refus, annulation de trajet
- [ ] Notation réciproque après un trajet effectué
- [ ] Messagerie entre conducteur et passager
- [ ] Paiement en ligne et partage de frais
- [ ] Espace d'administration : modération, signalements, statistiques
- [ ] Rattachement effectif des trajets aux organisations, et tableau de bord employeur
- [ ] Néerlandais
- [ ] Couverture de tests automatisés et intégration continue
- [ ] Application installable (PWA) et mode hors ligne
- [ ] Espace Entreprises et Blog

---

## Licence

À définir.
