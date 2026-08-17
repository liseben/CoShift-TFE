-- =============================================================================
--  CoShift — Schéma de référence (baseline)
-- =============================================================================
--  Cette migration crée l'intégralité du schéma à partir d'une base vide.
--  Elle remplace les cinq migrations précédentes, qui n'enchaînaient que des
--  ALTER TABLE sur des tables jamais créées par aucune migration : le schéma
--  n'existait en réalité que grâce à `spring.jpa.hibernate.ddl-auto=update`.
--
--  Les types reproduisent fidèlement ceux générés par Hibernate (bit(1) pour
--  les booléens, datetime(6) pour les horodatages, enum() pour les énumérations
--  annotées @Enumerated(EnumType.STRING)), afin que `ddl-auto=validate` accepte
--  le schéma sans écart. Seuls les noms de contraintes ont été rendus lisibles :
--  Hibernate générait des identifiants du type UK6dotkott2kjsp8vw4d0m25fb7.
--
--  Ordre de création imposé par les clés étrangères :
--  users → organizations → organization_members → vehicules → trips → bookings
-- =============================================================================


-- ─────────────────────────────────────────────────────────────────────────────
--  users — comptes de la plateforme (F4 à F9)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id                       BIGINT       NOT NULL AUTO_INCREMENT,
    uuid                     VARCHAR(255) NOT NULL,
    email                    VARCHAR(255) NOT NULL,
    password                 VARCHAR(255) NOT NULL,
    firstname                VARCHAR(255) DEFAULT NULL,
    lastname                 VARCHAR(255) DEFAULT NULL,
    picture_url              VARCHAR(255) DEFAULT NULL,
    phone_number             VARCHAR(255) DEFAULT NULL,
    role                     ENUM('ADMIN','SUPER_ADMIN','USER') DEFAULT NULL,

    -- F7 : validation du compte par code envoyé par email.
    -- Un compte naît non vérifié ; isEnabled() s'appuie sur cette colonne pour
    -- bloquer la connexion tant que le code n'a pas été saisi.
    email_verified           BIT(1)       NOT NULL DEFAULT b'0',
    verification_code        VARCHAR(6)   DEFAULT NULL,
    verification_code_expiry DATETIME(6)  DEFAULT NULL,

    -- F8 : statistiques affichées sur le profil, alimentées par F21/F22/F31.
    average_rating           DOUBLE       NOT NULL DEFAULT 0,
    trips_count              INT          NOT NULL DEFAULT 0,

    created_at               DATETIME(6)  DEFAULT NULL,
    updated_at               DATETIME(6)  DEFAULT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_uuid  (uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ─────────────────────────────────────────────────────────────────────────────
--  organizations — clients B2B : festivals, entreprises, universités, salons
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE organizations (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    uuid       VARCHAR(255) NOT NULL,
    name       VARCHAR(100) NOT NULL,
    -- Identifiant lisible utilisé pour le multi-tenant (URL, sous-domaine).
    slug       VARCHAR(255) NOT NULL,
    logo_url   VARCHAR(255) DEFAULT NULL,
    active     BIT(1)       NOT NULL DEFAULT b'1',
    created_at DATETIME(6)  DEFAULT NULL,
    updated_at DATETIME(6)  DEFAULT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_organizations_slug (slug),
    UNIQUE KEY uk_organizations_uuid (uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ─────────────────────────────────────────────────────────────────────────────
--  organization_members — table de liaison du ManyToMany User ↔ Organization
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE organization_members (
    user_id         BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,

    PRIMARY KEY (user_id, organization_id),
    KEY idx_org_members_organization (organization_id),
    CONSTRAINT fk_org_members_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_org_members_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ─────────────────────────────────────────────────────────────────────────────
--  vehicules — véhicules déclarés par les conducteurs (F14bis)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE vehicules (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    uuid          VARCHAR(255) NOT NULL,
    brand         VARCHAR(255) NOT NULL,
    model         VARCHAR(255) NOT NULL,
    -- Une plaque d'immatriculation identifie un seul véhicule.
    license_plate VARCHAR(255) NOT NULL,
    seats         INT          NOT NULL,
    energy        ENUM('DIESEL','ELECTRIC','GASOLINE','HYBRID','LPG') DEFAULT NULL,
    photo_url     VARCHAR(255) DEFAULT NULL,
    owner_id      BIGINT       NOT NULL,
    created_at    DATETIME(6)  DEFAULT NULL,
    updated_at    DATETIME(6)  DEFAULT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_vehicules_license_plate (license_plate),
    UNIQUE KEY uk_vehicules_uuid          (uuid),
    KEY idx_vehicules_owner (owner_id),
    CONSTRAINT fk_vehicules_owner
        FOREIGN KEY (owner_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ─────────────────────────────────────────────────────────────────────────────
--  trips — trajets publiés par les conducteurs (F16 à F18)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE trips (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    uuid              VARCHAR(255) NOT NULL,

    departure_city    VARCHAR(255) NOT NULL,
    departure_address VARCHAR(255) DEFAULT NULL,
    arrival_city      VARCHAR(255) NOT NULL,
    arrival_address   VARCHAR(255) DEFAULT NULL,
    departure_time    DATETIME(6)  NOT NULL,

    available_seats   INT            NOT NULL,
    price_per_seat    DECIMAL(38,2)  NOT NULL,
    description       TEXT,

    -- Préférences du conducteur affichées sur la fiche du trajet (F26).
    accepts_luggage   BIT(1) DEFAULT b'1',
    accepts_pets      BIT(1) DEFAULT b'0',
    music_allowed     BIT(1) DEFAULT b'1',
    talking_allowed   BIT(1) DEFAULT b'1',

    status            ENUM('CANCELLED','COMPLETED','FULL','PLANNED') NOT NULL,

    driver_id         BIGINT NOT NULL,
    vehicule_id       BIGINT NOT NULL,
    -- Nullable : un trajet peut exister hors de toute organisation tant que le
    -- module multi-tenant n'est pas branché. Hibernate avait créé cette colonne
    -- en NOT NULL, ce qui rendait toute publication de trajet impossible.
    organization_id   BIGINT DEFAULT NULL,

    created_at        DATETIME(6) DEFAULT NULL,
    updated_at        DATETIME(6) DEFAULT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_trips_uuid (uuid),
    KEY idx_trips_driver       (driver_id),
    KEY idx_trips_vehicule     (vehicule_id),
    KEY idx_trips_organization (organization_id),

    -- Exigence N1 (performance de recherche, importance 5/5) : la requête F25
    -- filtre sur le statut, les deux villes et la date de départ.
    KEY idx_trips_search (status, departure_time),
    KEY idx_trips_departure_city (departure_city),
    KEY idx_trips_arrival_city   (arrival_city),

    CONSTRAINT fk_trips_driver
        FOREIGN KEY (driver_id) REFERENCES users (id),
    CONSTRAINT fk_trips_vehicule
        FOREIGN KEY (vehicule_id) REFERENCES vehicules (id),
    CONSTRAINT fk_trips_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ─────────────────────────────────────────────────────────────────────────────
--  bookings — réservations de places par les passagers (F27 à F30)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE bookings (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    uuid         VARCHAR(255) NOT NULL,
    trip_id      BIGINT       NOT NULL,
    passenger_id BIGINT       NOT NULL,
    seats_booked INT          NOT NULL,
    total_price  DECIMAL(38,2) NOT NULL,
    status       ENUM('CANCELLED','COMPLETED','CONFIRMED','PENDING','REJECTED') NOT NULL,
    created_at   DATETIME(6)  DEFAULT NULL,
    updated_at   DATETIME(6)  DEFAULT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_bookings_uuid (uuid),
    KEY idx_bookings_trip      (trip_id),
    KEY idx_bookings_passenger (passenger_id),
    CONSTRAINT fk_bookings_trip
        FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT fk_bookings_passenger
        FOREIGN KEY (passenger_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ─────────────────────────────────────────────────────────────────────────────
--  articles — flux d'actualités mobilité agrégé depuis GNews et NewsData
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE articles (
    id               VARCHAR(255) NOT NULL,
    category         VARCHAR(255) DEFAULT NULL,
    title            VARCHAR(500) DEFAULT NULL,
    -- Titre normalisé servant à la déduplication (comparaison de Levenshtein).
    normalized_title VARCHAR(500) DEFAULT NULL,
    summary          TEXT,
    source           VARCHAR(255) DEFAULT NULL,
    date             DATE         DEFAULT NULL,
    image_url        VARCHAR(1000) DEFAULT NULL,
    url              VARCHAR(1000) DEFAULT NULL,
    created_at       DATETIME(6)  DEFAULT NULL,

    PRIMARY KEY (id),
    -- Index sur préfixe : utf8mb4 limite une clé à 3072 octets, soit 768
    -- caractères, ce qui interdit d'indexer un VARCHAR(1000) en entier.
    -- Garantit au niveau base l'unicité sur laquelle repose existsByUrl().
    UNIQUE KEY uk_articles_url (url(255)),
    KEY idx_articles_date     (date),
    KEY idx_articles_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
