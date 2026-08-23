-- =============================================================================
--  CoShift — confirmation de prestation (F21)
-- =============================================================================
--  BookingStatus.COMPLETED existait depuis le premier jour. Il est interrogé
--  par les requêtes de données ouvertes — « réservations abouties », « places
--  partagées » — et n'était attribué par aucune ligne de code. Une branche
--  entière des statistiques publiques était donc morte, en silence.
--
--  La conséquence se voyait ailleurs : sans réservation terminée, aucun trajet
--  ne comptait, users.trips_count restait à 0 pour tout le monde, et le tableau
--  de bord affichait « 0 trajet » à un conducteur qui en avait effectué dix.
--
--  Cette migration ouvre la voie à la confirmation par le passager.
--
--  completed_at plutôt qu'un simple changement de statut : la date de la
--  confirmation est ce qui ouvrira la fenêtre de notation (on ne note qu'après
--  coup), et ce qui permettra de distinguer une prestation confirmée le jour
--  même d'une confirmation tardive. Le statut seul ne porte pas cette
--  information.
--
--  La colonne est NULL par défaut : les réservations déjà en base n'ont jamais
--  été confirmées, et prétendre le contraire fausserait aussi bien les
--  statistiques que les compteurs de trajets.
-- =============================================================================

ALTER TABLE bookings
    ADD COLUMN completed_at DATETIME DEFAULT NULL AFTER status_reason;

-- La recherche des réservations confirmées à honorer parcourt les trajets
-- passés dont la réservation est encore CONFIRMED. Sans index sur le statut,
-- chaque passage impose un parcours complet de la table.
CREATE INDEX idx_bookings_status_completed ON bookings (status, completed_at);
