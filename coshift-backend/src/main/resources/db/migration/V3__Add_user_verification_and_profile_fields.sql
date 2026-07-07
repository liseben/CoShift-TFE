-- F7 : Validation de compte par email
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN verification_code VARCHAR(6);
ALTER TABLE users ADD COLUMN verification_code_expiry DATETIME;

-- F8/F9 : Profil enrichi
ALTER TABLE users ADD COLUMN phone_number VARCHAR(20);
ALTER TABLE users ADD COLUMN average_rating DOUBLE NOT NULL DEFAULT 0.0;
ALTER TABLE users ADD COLUMN trips_count INT NOT NULL DEFAULT 0;

-- Les utilisateurs existants sont déjà vérifiés (email_verified = TRUE par défaut ci-dessus)
-- Les futurs nouveaux utilisateurs démarreront avec email_verified = FALSE (géré côté Java)
