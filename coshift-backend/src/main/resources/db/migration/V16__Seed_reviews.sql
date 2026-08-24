-- =============================================================================
--  CoShift — avis de démonstration (F22, F31)
-- =============================================================================
--  La table reviews existait depuis V8 et restait vide : le jeu de
--  démonstration s'arrêtait à la réservation, et aucun profil ne portait la
--  réputation qui est pourtant la raison d'être de la table. Un correcteur qui
--  ouvre l'application voyait des moyennes à zéro partout — c'est-à-dire un
--  indicateur affiché et faux, le défaut exact que V8 corrigeait côté schéma.
--
--  ─── Ce que cette semence s'interdit ───
--
--  Aucun avis n'est inventé hors sol : chaque ligne s'adosse à une réservation
--  COMPLETED dont le départ est passé, comme le service l'exige à l'exécution.
--  L'unicité (booking_id, author_id) de V8 est respectée par construction —
--  une passe pour les passagers, une pour les conducteurs.
--
--  ─── Pourquoi les choix sont arithmétiques et non aléatoires ───
--
--  Les notes, les commentaires et les dates dérivent de MOD() sur les
--  identifiants, pas de RAND() : la migration produit le même résultat à
--  chaque exécution, donc une base reconstruite depuis les migrations est
--  identique à celle qui a servi aux captures et au dump. Une semence
--  aléatoire rendrait chaque clone du dépôt différent des autres.
--
--  Tous les passagers ne notent pas (9 sur 10 ici), et les conducteurs notent
--  moins (3 sur 5) : une table où 100 % des participants auraient voté se
--  reconnaîtrait au premier regard comme artificielle.
-- =============================================================================

-- ─── 1. Le passager note le conducteur ──────────────────────────────────────
INSERT INTO reviews (uuid, booking_id, author_id, target_id, rating, comment,
                     created_at, updated_at)
SELECT
    UUID(),
    b.id,
    b.passenger_id,
    t.driver_id,
    CASE
        WHEN MOD(b.id * 7 + 3, 20) < 10 THEN 5
        WHEN MOD(b.id * 7 + 3, 20) < 16 THEN 4
        WHEN MOD(b.id * 7 + 3, 20) < 18 THEN 3
        WHEN MOD(b.id * 7 + 3, 20) < 19 THEN 2
        ELSE 1
    END,
    CASE MOD(b.id * 11 + t.id, 18)
        WHEN 0  THEN 'Conduite souple et départ à l''heure, trajet très agréable.'
        WHEN 1  THEN 'Très ponctuel, voiture propre, conversation sympathique.'
        WHEN 2  THEN 'Parfait, le point de rendez-vous était facile à trouver.'
        WHEN 3  THEN 'Conducteur prudent, je referai le trajet sans hésiter.'
        WHEN 4  THEN 'Bonne ambiance et musique discrète, merci pour le trajet !'
        WHEN 5  THEN 'Arrivé à l''heure malgré le trafic sur le ring, chapeau.'
        WHEN 6  THEN 'Accueil chaleureux, trajet sans histoire.'
        WHEN 7  THEN 'Quelques minutes de retard au départ, mais prévenu par message.'
        WHEN 8  THEN 'Conduite un peu nerveuse à mon goût, sinon rien à redire.'
        WHEN 9  THEN 'Très arrangeant sur le lieu de dépose, je recommande.'
        WHEN 10 THEN 'Ponctuel et discret, exactement ce que je cherchais.'
        WHEN 11 THEN 'Le coffre était plein, ma valise a voyagé sur mes genoux.'
        ELSE NULL
    END,
    t.departure_time + INTERVAL 3 + MOD(b.id, 46) HOUR,
    t.departure_time + INTERVAL 3 + MOD(b.id, 46) HOUR
FROM bookings b
JOIN trips t ON t.id = b.trip_id
WHERE b.status = 'COMPLETED'
  AND t.departure_time < NOW()
  AND MOD(b.id, 10) <> 0;

-- ─── 2. Le conducteur note le passager ──────────────────────────────────────
INSERT INTO reviews (uuid, booking_id, author_id, target_id, rating, comment,
                     created_at, updated_at)
SELECT
    UUID(),
    b.id,
    t.driver_id,
    b.passenger_id,
    CASE
        WHEN MOD(b.id * 3 + 1, 20) < 12 THEN 5
        WHEN MOD(b.id * 3 + 1, 20) < 17 THEN 4
        WHEN MOD(b.id * 3 + 1, 20) < 19 THEN 3
        ELSE 2
    END,
    CASE MOD(b.id * 13 + 5, 14)
        WHEN 0 THEN 'Passager ponctuel et agréable, bienvenue à bord quand il veut.'
        WHEN 1 THEN 'Au rendez-vous à l''heure dite, trajet très sympathique.'
        WHEN 2 THEN 'Discret et courtois, aucun souci.'
        WHEN 3 THEN 'Bonne compagnie de route, la conversation a raccourci le trajet.'
        WHEN 4 THEN 'Parfait, a prévenu de son retard de cinq minutes.'
        WHEN 5 THEN 'Passagère très respectueuse du véhicule, je recommande.'
        WHEN 6 THEN 'Un peu silencieux mais irréprochable sur la ponctualité.'
        WHEN 7 THEN 'Arrivé en avance au point de rendez-vous, exemplaire.'
        ELSE NULL
    END,
    t.departure_time + INTERVAL 6 + MOD(b.id * 3, 90) HOUR,
    t.departure_time + INTERVAL 6 + MOD(b.id * 3, 90) HOUR
FROM bookings b
JOIN trips t ON t.id = b.trip_id
WHERE b.status = 'COMPLETED'
  AND t.departure_time < NOW()
  AND MOD(b.id, 5) IN (0, 1, 2);

-- ─── 3. Recopier la moyenne sur les profils ─────────────────────────────────
--  Même règle que ReviewService : la moyenne est relue depuis la table, jamais
--  incrémentée, et arrondie au dixième. Semer les avis sans recopier la
--  moyenne laisserait les profils à zéro — exactement l'incohérence que cette
--  migration vient corriger.
UPDATE users u
JOIN (
    SELECT target_id, ROUND(AVG(rating), 1) AS moyenne
    FROM reviews
    GROUP BY target_id
) r ON r.target_id = u.id
SET u.average_rating = r.moyenne;
