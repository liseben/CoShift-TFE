-- =============================================================================
--  V15 — La référence du remboursement, distincte de celle du paiement
-- =============================================================================
--  V14 n'avait qu'une colonne `provider_reference`, et le remboursement y
--  écrasait la référence du paiement. C'était sans conséquence avec la
--  simulation, qui ne se relit jamais ; cela ne l'est plus avec un vrai
--  prestataire.
--
--  Deux raisons de séparer les deux :
--
--  1. Chez Stripe, un remboursement se fait *sur* une intention de paiement.
--     Perdre l'identifiant de cette intention interdit tout second
--     remboursement, et rend impossible de retrouver l'opération d'origine.
--  2. Une notification du prestataire porte la référence du paiement. La
--     rapprocher de ce qui est enregistré ici suppose que cette référence n'ait
--     pas été remplacée entre-temps — sans quoi une confirmation arrivant en
--     retard ne retrouverait plus rien.
--
--  Un relevé comptable doit pouvoir montrer les deux mouvements : ce qui a été
--  encaissé, et ce qui a été rendu. Une seule colonne n'en montrait qu'un, et
--  toujours le dernier.
-- =============================================================================

ALTER TABLE payments
    ADD COLUMN refund_reference VARCHAR(255) DEFAULT NULL AFTER provider_reference;

-- Sert à retrouver une opération depuis une notification du prestataire.
CREATE INDEX idx_payments_provider_reference ON payments (provider_reference);
