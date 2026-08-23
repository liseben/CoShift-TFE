# CoShift

Plateforme de covoiturage domicile-travail pour les entreprises, les universités et les événements.

Là où les services grand public mettent en relation des inconnus sur de longues distances, CoShift s'adresse à des personnes qui partagent déjà un lieu et des horaires : des collègues, des étudiants d'un même campus, des participants d'un même événement. Le trajet quotidien, court et répété, est celui qui pèse le plus dans les émissions liées à la mobilité, et c'est celui que le covoiturage classique couvre le moins bien.

> **Version alpha.** Produit Minimum Viable. Toutes les fonctionnalités prévues ne sont pas réalisées ; celles qui figurent ci-dessous fonctionnent. Voir [Périmètre](#périmètre) et [Feuille de route](#feuille-de-route).

Travail de fin d'études — développement d'application web, 2025-2026.

---

## Essayer l'application

Trois commandes, aucune clé d'API à obtenir au préalable.

```bash
# 1. Base vide — Flyway construit le schéma et charge le jeu de démonstration
mysql -u root -e "CREATE DATABASE coshift_db CHARACTER SET utf8mb4;"

# 2. API — le modèle .env fonctionne tel quel si MySQL tourne en root sans mot de passe
cd coshift-backend && cp .env.example .env && ./mvnw spring-boot:run

# 3. Interface, dans un second terminal
cd coshift-frontend && cp .env.example .env && npm install && npm run dev
```

Ouvrir `http://localhost:5173` et se connecter :

| Adresse | Mot de passe | Ce que ce compte permet de voir |
|---|---|---|
| `julie.lecomte@salon-mobilite.be` | `1234` | 8 trajets publiés, 1 véhicule, 2 réservations |
| `charlotte.guerin@u-basse-meuse.be` | `1234` | 3 véhicules, 7 trajets |
| `margaux.gautier@val-vert.be` | `1234` | 5 trajets et 5 réservations — les deux rôles à la fois |

Détail des variables d'environnement et des cas particuliers : [Installation](#installation).

---

## Sommaire

- [Essayer l'application](#essayer-lapplication)
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

### Notifications

- Courriel à chaque changement d'état : demande reçue, réservation acceptée, refusée avec son motif, désistement d'un passager, et annulation d'un trajet — la plus importante des cinq, sans laquelle quelqu'un attend à un point de rendez-vous où personne ne viendra.
- **Chacun est écrit dans la langue de son destinataire**, pas dans celle de la personne qui a déclenché l'envoi. Un conducteur francophone sollicité par un passager anglophone reçoit du français.

### Confiance

- **Confirmation de prestation.** Le passager reconnaît que le trajet a eu lieu — et lui seul. Le conducteur a un intérêt à déclarer la course effectuée ; le passager n'en a aucun. Confier la confirmation à la partie qui n'y gagne rien est ce qui en fait une information fiable. Le compteur de trajets des deux participants est alors incrémenté.
- **Notation réciproque.** Le passager note le conducteur, le conducteur note le passager. Trois conditions : avoir effectivement voyagé (réservation confirmée *puis* reconnue), avoir voyagé avec la personne notée, et une seule fois par trajet. Sans la première, réserver puis annuler donnerait le droit de noter ; sans la troisième, noter en boucle suffirait à couler quelqu'un.
- La moyenne du profil est **relue depuis la table** après chaque avis, jamais mise à jour par pondération : un calcul incrémental dérive sans que rien ne le signale.

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

| Variable | Obligatoire | Rôle |
|---|---|---|
| `DB_USERNAME`, `DB_PASSWORD` | **oui** | Compte MySQL local. Sous WAMP, `root` sans mot de passe |
| `JWT_SECRET_KEY` | **oui** | Clé de signature, Base64, 256 bits. **Une clé de démonstration est déjà fournie dans le modèle** — laissée vide, l'application démarre mais échoue à la première connexion |
| `JWT_EXPIRATION` | **oui** | Validité du jeton en millisecondes. Pré-rempli à `86400000` (24 h) |
| `MAIL_USERNAME`, `MAIL_APP_PASSWORD` | non | SMTP. Sur Gmail, un *mot de passe d'application*, pas celui du compte. Sans eux, tout fonctionne sauf l'envoi du code d'activation : utilisez alors les comptes de démonstration, déjà vérifiés |
| `GNEWS_API_KEY`, `NEWSDATA_API_KEY` | non | Flux d'actualités. Sans eux, la rubrique Actus reste vide |
| `SERVER_PORT` | non | Port d'écoute, `8080` par défaut |

Les variables non obligatoires doivent rester **présentes dans le fichier, même vides** : Spring échoue au démarrage sur un placeholder qu'il ne peut pas résoudre. Copier `.env.example` tel quel suffit.

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

[MIT](LICENSE).

Le choix est cohérent avec le jeu de données que l'application publie déjà sous [Licence Ouverte 2.0](https://www.etalab.gouv.fr/licence-ouverte-open-licence/) : ouvrir les données tout en réservant le code aurait été contradictoire. Un dépôt public sans fichier de licence reste, par défaut, sous droit d'auteur intégral — personne ne peut légalement le réutiliser, y compris pour l'étudier.
