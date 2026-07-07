-- L'organisation est optionnelle pour un trajet tant que le module
-- organisations n'est pas pleinement implémenté.
ALTER TABLE trips MODIFY COLUMN organization_id BIGINT NULL;

-- Ajout des champs d'adresse complète et préférences conducteur
ALTER TABLE trips ADD COLUMN departure_address VARCHAR(500);
ALTER TABLE trips ADD COLUMN arrival_address VARCHAR(500);
ALTER TABLE trips ADD COLUMN description TEXT;
ALTER TABLE trips ADD COLUMN accepts_luggage BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE trips ADD COLUMN accepts_pets BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE trips ADD COLUMN music_allowed BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE trips ADD COLUMN talking_allowed BOOLEAN NOT NULL DEFAULT TRUE;
