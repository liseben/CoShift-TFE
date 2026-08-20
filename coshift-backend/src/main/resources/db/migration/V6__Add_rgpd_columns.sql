-- =============================================================================
--  CoShift — droits des personnes et preuve de l'acceptation des conditions
-- =============================================================================
--  Trois colonnes, deux obligations distinctes.
--
--  deleted_at — article 17 du RGPD, droit à l'effacement.
--
--  Supprimer la ligne d'un membre est impossible : trips.driver_id et
--  bookings.passenger_id sont NOT NULL, et un covoiturage passé engage deux
--  personnes — effacer la trace du conducteur priverait le passager de son
--  propre historique. L'effacement procède donc par anonymisation : les champs
--  identifiants sont écrasés sur place, immédiatement et sans retour possible,
--  et cette colonne marque la date à laquelle l'opération a eu lieu.
--
--  Elle sert aussi de verrou d'authentification : un compte dont deleted_at
--  n'est pas NULL ne peut plus se connecter, quelle que soit la valeur écrite
--  dans password.
--
--  cgu_accepted_at et cgu_version — preuve de l'accord.
--
--  L'acceptation des conditions générales n'était consignée nulle part. Sans
--  date ni numéro de version, il est impossible d'établir à quoi une personne
--  a consenti, ni de savoir qui doit être informé lors d'une modification
--  substantielle. NULL désigne les comptes créés avant cette migration : leur
--  accord n'a pas été recueilli, et le prétendre serait faux.
-- =============================================================================

ALTER TABLE users
    ADD COLUMN deleted_at DATETIME DEFAULT NULL AFTER updated_at,
    ADD COLUMN cgu_accepted_at DATETIME DEFAULT NULL AFTER deleted_at,
    ADD COLUMN cgu_version VARCHAR(10) DEFAULT NULL AFTER cgu_accepted_at;

-- La purge des comptes jamais vérifiés et l'anonymisation des trajets anciens
-- balaient ces colonnes à intervalle régulier. Sans index, chaque passage
-- imposerait un parcours complet des tables.
CREATE INDEX idx_users_deleted_at ON users (deleted_at);
CREATE INDEX idx_users_verif_created ON users (email_verified, created_at);
CREATE INDEX idx_trips_departure_time ON trips (departure_time);
