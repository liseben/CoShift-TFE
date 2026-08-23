-- =============================================================================
--  CoShift — notation réciproque (F22, F31)
-- =============================================================================
--  users.average_rating existait depuis le baseline, en NOT NULL DEFAULT 0, et
--  n'était alimenté par rien. Le tableau de bord affichait donc une étoile à
--  zéro à tout le monde, y compris à un conducteur chevronné. Un indicateur
--  affiché et faux est pire qu'un indicateur absent : celui-ci se corrige ici.
--
--  Le covoiturage repose sur le fait de monter en voiture avec un inconnu. Le
--  seul substitut au lien social est la réputation accumulée : c'est la raison
--  d'être de cette table, bien plus que l'ornement d'un profil.
--
--  ─── Pourquoi l'avis est rattaché à la réservation ───
--
--  Et non pas directement à une paire de personnes. Une réservation est la
--  preuve qu'un trajet a été partagé : s'y adosser garantit qu'on ne note que
--  ce qu'on a vécu. La contrainte d'unicité (booking_id, author_id) découle du
--  même raisonnement — un trajet, un avis par participant. Sans elle, il
--  suffirait de noter en boucle pour couler quelqu'un.
--
--  ─── Pourquoi author_id ET target_id ───
--
--  La notation est réciproque : le passager note le conducteur, le conducteur
--  note le passager. Déduire la cible de la réservation obligerait à savoir de
--  quel côté se place l'auteur à chaque lecture. Deux colonnes rendent la
--  requête de moyenne triviale et l'intention explicite.
--
--  ─── Données personnelles ───
--
--  Le commentaire est un texte libre rédigé par une personne au sujet d'une
--  autre. C'est la donnée la plus sensible de la base après les adresses. Il
--  est effacé lorsque son auteur exerce son droit à l'effacement, et la note
--  chiffrée reste — détachée de tout nom, elle ne se rapporte plus à personne.
-- =============================================================================

CREATE TABLE reviews (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    uuid       VARCHAR(255) NOT NULL,
    booking_id BIGINT       NOT NULL,
    author_id  BIGINT       NOT NULL,
    target_id  BIGINT       NOT NULL,
    rating     TINYINT      NOT NULL,
    comment    VARCHAR(500) DEFAULT NULL,
    created_at DATETIME(6)  DEFAULT NULL,
    updated_at DATETIME(6)  DEFAULT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_reviews_uuid (uuid),

    -- Un trajet, un avis par participant. La règle est aussi appliquée dans le
    -- service, qui rend un message compréhensible ; celle-ci est le garde-fou
    -- de dernier recours, celui qui tient même si le code se trompe.
    UNIQUE KEY uk_reviews_booking_author (booking_id, author_id),

    -- La moyenne d'une personne se recalcule à chaque nouvel avis reçu : c'est
    -- la lecture la plus fréquente de la table.
    KEY idx_reviews_target (target_id),
    KEY idx_reviews_author (author_id),

    -- Une note hors barème n'a pas de sens et fausserait toutes les moyennes.
    --
    -- ATTENTION : cette contrainte n'est PAS active partout. MySQL analyse la
    -- clause CHECK depuis toujours mais ne l'applique qu'à partir de la 8.0.16 ;
    -- en 5.7, elle est acceptée puis ignorée en silence. Le poste de
    -- développement tourne sur 5.7.23 — vérifié — donc la garantie y est nulle.
    --
    -- Ce qui protège réellement le barème aujourd'hui, ce sont les annotations
    -- @Min(1) et @Max(5) de ReviewRequest, appliquées avant que la requête
    -- n'atteigne la base. La contrainte est écrite malgré tout : elle deviendra
    -- effective au passage en MySQL 8, sans migration supplémentaire, et une
    -- protection qui dort vaut mieux qu'une protection absente.
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5),

    CONSTRAINT fk_reviews_booking
        FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT fk_reviews_author
        FOREIGN KEY (author_id) REFERENCES users (id),
    CONSTRAINT fk_reviews_target
        FOREIGN KEY (target_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
