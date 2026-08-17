-- F20 / F29 : le conducteur doit motiver un refus de réservation, et le passager
-- peut expliquer son annulation. Une seule colonne suffit : une réservation ne
-- porte qu'un statut à la fois, donc qu'un seul motif à la fois.
ALTER TABLE bookings
    ADD COLUMN status_reason VARCHAR(500) DEFAULT NULL AFTER status;
