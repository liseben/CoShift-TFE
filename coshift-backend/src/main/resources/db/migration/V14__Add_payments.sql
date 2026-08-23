-- =============================================================================
--  V14 — Le paiement d'une réservation
-- =============================================================================
--  Jusqu'ici, le prix par place était saisi, multiplié et rangé dans
--  `bookings.total_price`, et rien ne se passait ensuite : un montant dû que
--  personne ne devait, ne payait, ni ne remboursait.
--
--  CE QUE CETTE TABLE EST, ET CE QU'ELLE N'EST PAS
--  Elle tient la comptabilité d'une réservation : ce qui est dû, ce qui a été
--  réglé, ce qui a été rendu et pourquoi. Elle ne fait pas circuler d'argent —
--  cela relève d'un prestataire agréé, et le champ `provider` dit lequel a
--  traité l'opération.
--
--  Encaisser pour le compte d'un tiers relève de la DSP2 et du statut d'agent
--  de paiement. Tant que ce statut n'est pas obtenu, le seul prestataire
--  disponible est la simulation, et les conditions générales continuent de dire
--  vrai : aucun paiement n'est perçu par CoShift.
--
--  POURQUOI UNE TABLE ET NON DEUX COLONNES SUR `bookings`
--  Un paiement a son propre cycle de vie, ses propres dates et sa propre
--  référence chez le prestataire. Le loger dans la réservation mêlerait deux
--  histoires — celle du siège et celle de l'argent — qui ne changent pas
--  d'état aux mêmes moments.
--
--  POURQUOI LE MONTANT EST RECOPIÉ
--  `amount` duplique `bookings.total_price` au moment où le paiement naît.
--  C'est délibéré : le prix d'un trajet peut changer, et un paiement doit dire
--  ce qui a été réglé ce jour-là, pas ce que coûterait la même place
--  aujourd'hui. Une facture ne se recalcule pas.
-- =============================================================================

CREATE TABLE payments (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    uuid        VARCHAR(255) NOT NULL,

    -- Une réservation, un paiement. La clé est unique côté réservation : deux
    -- paiements pour un même siège seraient deux fois le même dû.
    booking_id  BIGINT       NOT NULL,

    -- Recopié à la naissance du paiement. Voir l'en-tête.
    amount      DECIMAL(38,2) NOT NULL,
    currency    VARCHAR(3)    NOT NULL DEFAULT 'EUR',

    status      ENUM('DUE','PAID','REFUNDED','PARTIALLY_REFUNDED','CANCELLED','FAILED')
                NOT NULL DEFAULT 'DUE',

    -- Prestataire ayant traité l'opération. `SIMULATION` tant qu'aucun
    -- prestataire agréé n'est branché : il vaut mieux nommer la simulation que
    -- laisser croire à un encaissement.
    provider            VARCHAR(20)   NOT NULL DEFAULT 'SIMULATION',
    -- Référence de l'opération chez le prestataire, seule façon de rapprocher
    -- une ligne d'ici d'une ligne de son relevé.
    provider_reference  VARCHAR(255)  DEFAULT NULL,

    paid_at         DATETIME(6)   DEFAULT NULL,
    refunded_at     DATETIME(6)   DEFAULT NULL,
    -- Montant réellement rendu. Distinct du montant total : le barème
    -- d'annulation prévoit des remboursements partiels.
    refunded_amount DECIMAL(38,2) NOT NULL DEFAULT 0.00,
    -- Pourquoi ce remboursement, et selon quelle règle du barème.
    refund_reason   VARCHAR(255)  DEFAULT NULL,

    created_at DATETIME(6) DEFAULT NULL,
    updated_at DATETIME(6) DEFAULT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_payments_uuid (uuid),
    UNIQUE KEY uk_payments_booking (booking_id),
    KEY idx_payments_status (status),
    CONSTRAINT fk_payments_booking
        FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ─────────────────────────────────────────────────────────────────────────────
--  Les réservations déjà en base
-- ─────────────────────────────────────────────────────────────────────────────
--  Chaque réservation existante reçoit son paiement, dans l'état qui
--  correspond à sa propre histoire :
--
--    CONFIRMED et COMPLETED  → réglé. Le trajet a eu lieu ou va avoir lieu
--                              avec l'accord du conducteur ; le montant est
--                              considéré comme acquitté.
--    PENDING                 → dû. Le conducteur n'a pas encore répondu.
--    REJECTED et CANCELLED   → annulé, montant nul. Rien n'a été prélevé, il
--                              n'y a donc rien à rendre.
--
--  Sans ce rattrapage, les 262 réservations du jeu de démonstration
--  apparaîtraient sans aucune trace comptable, et l'écran de paiement serait
--  vide sur tout ce qui précède cette version.
INSERT INTO payments (uuid, booking_id, amount, currency, status, provider,
                      provider_reference, paid_at, refunded_amount, created_at, updated_at)
SELECT
    CONCAT('0f6-', LPAD(b.id, 6, '0'), '-0000-4000-8000-', LPAD(b.id, 12, '0')),
    b.id,
    b.total_price,
    'EUR',
    CASE
        WHEN b.status IN ('CONFIRMED', 'COMPLETED') THEN 'PAID'
        WHEN b.status = 'PENDING'                   THEN 'DUE'
        ELSE 'CANCELLED'
    END,
    'SIMULATION',
    CASE WHEN b.status IN ('CONFIRMED', 'COMPLETED')
         THEN CONCAT('sim_', LPAD(b.id, 10, '0')) END,
    CASE WHEN b.status IN ('CONFIRMED', 'COMPLETED')
         THEN COALESCE(b.updated_at, b.created_at) END,
    0.00,
    b.created_at,
    b.updated_at
FROM bookings b;
