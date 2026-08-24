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

### Courriels

Sept messages partent automatiquement, chacun **dans la langue du destinataire**
et non dans celle de la personne qui agit : activation du compte, bienvenue une
fois l'adresse confirmée, demande de réservation reçue, réservation acceptée,
réservation refusée avec son motif, réservation annulée par le passager, trajet
annulé par le conducteur — et un **reçu de paiement** portant le trajet, le
nombre de places, le montant et la référence de l'opération, seul identifiant
commun entre ce courriel, la base et le relevé bancaire.

La bienvenue part après la vérification, pas à l'inscription : le courriel
d'inscription porte le code d'activation, et y ajouter un mot d'accueil noierait
la seule chose qu'on doit y trouver. C'est aussi le moment où le rattachement à
une organisation est acquis, donc où le message peut nommer le cercle rejoint.

> **Configuration requise.** `MAIL_USERNAME` et `MAIL_APP_PASSWORD` dans
> `coshift-backend/.env`. Sans mot de passe d'application valide, les envois
> échouent silencieusement — l'échec est journalisé et n'interrompt jamais
> l'opération en cours : perdre un reçu est regrettable, perdre l'enregistrement
> d'un règlement le serait davantage.

### Partage de frais

- Le montant devient **dû dès la demande**, mais rien n'est prélevé : le
  conducteur peut encore refuser, et faire payer une place qu'on n'aura
  peut-être pas obligerait à rembourser des gens qui n'ont jamais voyagé.
- **Barème d'annulation**, tenant en une idée : *on ne fait pas payer quelqu'un
  pour une décision qui n'est pas la sienne.*

  | Situation | Rendu au passager |
  |---|---|
  | Le conducteur annule le trajet, ou refuse la demande | 100 % |
  | Le passager annule plus de 24 h avant le départ | 100 % |
  | Le passager annule moins de 24 h avant | 50 % |
  | Le passager annule après le départ | 0 % |

  Le seuil est *une* décision défendable, pas la seule. Ce qui compte est qu'il
  soit écrit, appliqué au même endroit pour tout le monde, et **annoncé avant**
  que la personne confirme — découvrir après coup qu'on ne récupère que la
  moitié est le genre de surprise qui vaut une réclamation.

- **Stripe, en mode test.** Le mode test ne demande ni société ni agrément :
  les clés `sk_test_` et `pk_test_` s'obtiennent en quelques minutes et aucun
  euro ne circule. Le paiement passe par une intention créée côté serveur, un
  formulaire de carte servi par Stripe, et une confirmation qui vient d'une
  **notification signée** — jamais du navigateur.

  **Les coordonnées bancaires ne transitent jamais par CoShift.** Le champ de
  saisie appartient à Stripe et vit dans un cadre isolé : ni le numéro, ni la
  date, ni le cryptogramme n'atteignent l'application, ne sont journalisés, ni
  ne pourraient fuiter d'une base compromise.

  **Le navigateur n'est pas cru sur parole.** Après confirmation, la page
  annonce « c'est payé » ; cette page est entre les mains de la personne qui
  paie. Le serveur interroge donc Stripe lui-même — ou attend sa notification,
  dont la signature est vérifiée sur les octets bruts de la requête.

- **Sans clé configurée, l'application retombe sur une simulation** et démarre
  normalement. C'est ce qui permet de cloner le dépôt et de lancer le projet
  sans compte Stripe, et ce qui fait passer l'intégration continue. Le
  prestataire retenu est écrit au journal au démarrage, enregistré avec chaque
  opération et affiché à l'écran : tant qu'il vaut `SIMULATION`, l'interface
  annonce qu'aucun euro ne circule.

  Tout le reste — montants, états, barème, remboursements partiels — est du
  code métier qui se teste sans compte chez un prestataire. C'est ce qui a
  motivé la séparation entre les règles et le mouvement de fonds.

### Administration

- Deux rôles, deux portées. Un **`SUPER_ADMIN`** répond de la plateforme et voit
  tout ; un **`ADMIN`** répond de ses organisations et ne voit qu'elles. Sans
  cette borne, donner un rôle d'administrateur à une entreprise cliente lui
  ouvrirait les membres et les trajets de toutes les autres — le cercle fermé se
  contournerait par un rôle au lieu de se contourner par une requête. L'écran
  annonce sa portée plutôt que de laisser lire un chiffre borné comme un chiffre
  global.
- Supervision en lecture : membres, trajets et réservations du périmètre.
- **Attribution des rôles**, réservée au `SUPER_ADMIN`. Le premier a été posé par
  une migration — il faut bien que quelqu'un ouvre la porte de l'intérieur — mais
  s'en tenir là obligerait à redéployer chaque fois qu'un client change
  d'interlocuteur. Trois refus : on ne change pas son propre rôle, on ne
  rétrograde pas le dernier administrateur de plateforme, et un compte dont
  l'adresse n'est pas confirmée n'obtient aucun rôle — le donner à une adresse
  non prouvée, c'est le donner à qui la contrôle.
- **Suspension d'un compte**, réservée au `SUPER_ADMIN` : consulter n'engage
  rien, suspendre engage la plateforme vis-à-vis de la personne. Le motif est
  obligatoire. Suspendre n'efface rien — les trajets passés engagent aussi les
  autres participants et restent en place.
- La suspension prend effet **immédiatement**, y compris sur un jeton déjà émis :
  le filtre JWT relit l'état du compte à chaque requête. Une mesure qui ne
  prendrait effet qu'à la prochaine connexion n'en serait pas une.
- La console **n'expose pas le journal de sécurité**. Il reste un fichier : lui
  ouvrir un point d'entrée reviendrait à offrir, derrière une seule
  authentification, la liste des comptes et des adresses attaqués. Elle montre à
  la place les **freinages de connexion en cours**, qui répondent à la même
  question et sont actionnables tout de suite.

### Installation sur l'écran d'accueil

- L'application s'installe depuis le bouton de l'en-tête, sur Chrome, Edge et
  Android. Sur iOS, où le navigateur n'offre aucune invite, le bouton explique
  le geste manuel au lieu de faire semblant. Là où ni l'un ni l'autre n'est
  possible, il ne s'affiche pas : un bouton qui ne fait rien apprend qu'on ne
  peut pas se fier à l'interface.
- Une fois installée, l'application **s'ouvre sans réseau** : la coquille —
  code, styles, polices, images — est mise en cache. Elle ne **fonctionne** pas
  sans réseau pour autant : chercher un trajet ou réserver exige le serveur.
  Aucune réponse de l'API n'est conservée, délibérément — une liste de trajets
  servie depuis un cache montrerait des places déjà prises.
- Une nouvelle version est **annoncée**, jamais installée d'office. Remplacer
  l'application à la navigation suivante interromprait un formulaire à moitié
  rempli.

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

## Activer Stripe (facultatif)

Le projet démarre sans compte Stripe : les paiements passent alors par une
simulation, et l'écran le dit. Pour éprouver un vrai paiement par carte :

1. Créer un compte sur [dashboard.stripe.com](https://dashboard.stripe.com) et
   relever les clés **de test** sur la page *Developers → API keys*.
2. Renseigner `coshift-backend/.env` :

   ```
   STRIPE_SECRET_KEY=sk_test_...
   STRIPE_PUBLISHABLE_KEY=pk_test_...
   ```

3. Renseigner `coshift-frontend/.env` :

   ```
   VITE_STRIPE_PUBLIC_KEY=pk_test_...
   ```

4. Redémarrer les deux serveurs. Le journal du backend indique quel prestataire
   a été retenu — c'est la première chose à vérifier si un paiement se comporte
   autrement qu'attendu.

Carte d'essai : **4242 4242 4242 4242**, une date future et un cryptogramme
quelconque. Aucun débit réel n'a lieu. Les autres numéros d'essai — carte
refusée, authentification requise — sont
[documentés par Stripe](https://docs.stripe.com/testing).

**Les notifications** (`POST /api/payments/webhook`) sont l'autorité en
production. Un poste de développement n'ayant pas d'adresse publique, le
serveur interroge Stripe lui-même après confirmation ; pour éprouver le chemin
des notifications en local, `stripe listen --forward-to
localhost:8081/api/payments/webhook` et le secret `whsec_...` dans
`STRIPE_WEBHOOK_SECRET`.

> **Ne jamais renseigner de clé `sk_live_`.** Le journal la signale, mais rien
> ne l'interdit techniquement : elle ferait circuler de l'argent réel.

---

## Comptes de démonstration

Les migrations chargent un jeu de données de développement : 12 organisations, 122 comptes, 114 véhicules, 208 trajets et 262 réservations. Les organisations sont fictives ; les villes, marques et modèles sont réels, pour que distances et capacités restent crédibles.

Chaque compte n'accède qu'aux trajets de ses organisations : c'est le cercle fermé, et il se voit dès la première recherche. La colonne « voit » ci-dessous donne le nombre de trajets à venir accessibles à chacun.

| Adresse | Mot de passe | Intérêt pour la démonstration | Voit |
|---|---|---|---|
| `julie.lecomte@salon-mobilite.be` | `1234` | 8 trajets publiés, 1 véhicule, 2 réservations | 5 |
| `charlotte.guerin@u-basse-meuse.be` | `1234` | 3 véhicules, 8 trajets | 6 |
| `margaux.gautier@val-vert.be` | `1234` | 6 trajets et 5 réservations — les deux rôles | 6 |
| `sarah.aubert@val-vert.be` | `1234` | **Deux organisations** : le sélecteur de cercle apparaît à la publication | 13 |
| `fanny.moreau@solvantis.be` | `1234` | **`SUPER_ADMIN`** : console d'administration, portée plateforme | 6 |
| `julien.martin@he-condroz.be` | `1234` | **`ADMIN`** : même console, bornée à son organisation | 7 |

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

Livré depuis la première rédaction de cette liste :

- [x] Couverture de tests automatisés et intégration continue
- [x] Espace Entreprises et Blog
- [x] Rattachement des trajets aux organisations, cercle de visibilité fermé et
      tableau de bord d'organisation
- [x] Application installable (PWA)
- [x] Espace d'administration : supervision et suspension de comptes
- [x] Blog rédigeable depuis l'administration, sans redéploiement
- [x] Partage de frais : montants, états, barème d'annulation et remboursements
- [x] Paiement par carte via Stripe, en mode test

Prévu, non réalisé dans cette version alpha :

- [ ] Messagerie entre conducteur et passager
- [ ] **Encaissement réel.** L'intégration Stripe fonctionne en mode test ;
      passer en production suppose un statut réglementaire (DSP2, agent de
      paiement), une adresse publique pour recevoir les notifications, et le
      reversement au conducteur, qui n'est pas modélisé
- [ ] Signalement d'une annonce ou d'un membre, et file de modération
- [ ] Néerlandais
- [ ] Distance des trajets, sans laquelle ni les kilomètres partagés ni les
      émissions évitées ne sont calculables
- [ ] Mode hors ligne réel : aujourd'hui l'application **s'ouvre** sans réseau,
      elle ne **fonctionne** pas sans lui

---

## Licence

[MIT](LICENSE).

Le choix est cohérent avec le jeu de données que l'application publie déjà sous [Licence Ouverte 2.0](https://www.etalab.gouv.fr/licence-ouverte-open-licence/) : ouvrir les données tout en réservant le code aurait été contradictoire. Un dépôt public sans fichier de licence reste, par défaut, sous droit d'auteur intégral — personne ne peut légalement le réutiliser, y compris pour l'étudier.
