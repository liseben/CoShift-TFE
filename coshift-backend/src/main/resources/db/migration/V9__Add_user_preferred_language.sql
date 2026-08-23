-- =============================================================================
--  CoShift — langue de correspondance du membre
-- =============================================================================
--  Jusqu'ici, la langue d'un courriel venait de la requête en cours, lue dans
--  l'en-tête Accept-Language. C'était correct pour les deux seuls courriels
--  existants : on demande son propre code de vérification, sa propre
--  réinitialisation. Le destinataire était toujours celui qui agissait.
--
--  Les notifications rompent cette coïncidence. Quand un passager réserve, le
--  courriel part vers le CONDUCTEUR — et la langue de la requête est celle du
--  passager. Un membre anglophone recevrait « Nouvelle demande de réservation »
--  parce que quelqu'un d'autre a cliqué en français.
--
--  Cette colonne mémorise donc la langue choisie par la personne, relevée à
--  l'inscription puis à chaque connexion. Ces deux moments suffisent : ils sont
--  fréquents, ils sont toujours le fait de la personne elle-même, et ils ne
--  demandent aucun écran de préférences supplémentaire.
--
--  NULL est un état légitime, et non un défaut à corriger : il désigne les
--  comptes créés avant cette migration, dont personne n'a jamais relevé la
--  langue. Écrire « fr » à leur place serait une supposition présentée comme un
--  fait. Le code retombe explicitement sur le français dans ce cas, ce qui est
--  la même chose — sauf que la base, elle, ne ment pas.
--
--  VARCHAR(5) : assez pour une étiquette BCP 47 courte, « fr » comme « fr-BE ».
-- =============================================================================

ALTER TABLE users
    ADD COLUMN preferred_language VARCHAR(5) DEFAULT NULL AFTER cgu_version;
