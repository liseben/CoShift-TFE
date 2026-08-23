# CoShift — API

Backend Spring Boot de la plateforme CoShift. Il expose une API REST consommée par le module `coshift-frontend`.

**La procédure d'installation complète se trouve dans le [README à la racine](../README.md).**

## Commandes

| Commande | Effet |
|---|---|
| `./mvnw spring-boot:run` | Démarre l'API sur `http://localhost:8080` |
| `./mvnw test` | Suite de tests, sur base H2 en mémoire |
| `./mvnw package` | Produit le jar exécutable dans `target/` |

Profil de production : `./mvnw spring-boot:run -Dspring-boot.run.profiles=prod`. Il coupe la documentation d'API, restreint les origines autorisées et réduit la journalisation.

## Configuration

Copier `.env.example` en `.env` et le compléter. Le fichier est chargé au démarrage par `java-dotenv` ; il n'est pas versionné.

Les valeurs obligatoires sont `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET_KEY`, `JWT_EXPIRATION`, `MAIL_USERNAME` et `MAIL_APP_PASSWORD`.

## Base de données

Le schéma est piloté par Flyway et lui seul. `spring.jpa.hibernate.ddl-auto` est réglé sur `validate` : Hibernate vérifie que les entités correspondent au schéma produit par les migrations, sans jamais le modifier de lui-même. C'est ce qui garantit que le dépôt reste la source de vérité.

Les migrations vivent dans `src/main/resources/db/migration`. Une migration appliquée ne se modifie plus : Flyway en conserve une somme de contrôle et refuserait de démarrer. Toute correction passe par une migration supplémentaire.

`V3` et `V4` chargent un jeu de données de développement. Il n'a pas vocation à être déployé.

## Organisation

```
src/main/java/com/coshift/api/
  config/        Sécurité, CORS, OpenAPI, internationalisation, ressources statiques
  controller/    Points d'entrée REST
  dto/           Objets d'échange et contraintes de validation
  entity/        Modèle persistant
  exception/     Exceptions métier et traduction en réponses HTTP
  repository/    Accès aux données
  security/      JWT, freinage des tentatives, journal de sécurité
  service/       Règles métier
  util/
```

## Points d'entrée utiles

| Adresse | Contenu |
|---|---|
| `/swagger-ui.html` | Documentation interactive de l'API (hors profil `prod`) |
| `/v3/api-docs` | Spécification OpenAPI 3.1 |
| `/actuator/health` | Sonde de disponibilité |
| `/api/open-data` | Jeu de données ouvertes agrégées |
| `/sitemap.xml` | Plan du site |

## Journalisation

Deux destinations distinctes : la console pour le journal applicatif, et `logs/securite.log` pour les seuls événements de sécurité. Ce second fichier tourne quotidiennement et n'est conservé que douze mois — il contient des adresses électroniques et des adresses IP. Il n'est pas versionné.
