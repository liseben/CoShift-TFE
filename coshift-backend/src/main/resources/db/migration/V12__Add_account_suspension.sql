-- =============================================================================
--  V12 — Suspension d'un compte, et un compte de supervision pour la démonstration
-- =============================================================================
--  Le rôle ADMIN existait depuis le schéma de référence sans ouvrir le moindre
--  écran, et SUPER_ADMIN n'était porté par personne. Les deux prennent un sens
--  ici : ADMIN supervise le cercle de ses organisations, SUPER_ADMIN répond de
--  toute la plateforme. Seul le second peut suspendre.
--
--  POURQUOI UNE DATE ET NON UN BOOLÉEN
--  `suspended_at` répond à « depuis quand », ce qu'un drapeau ne sait pas dire.
--  Une mesure de modération se conteste : savoir quand elle a été prise fait
--  partie de ce qu'on doit pouvoir répondre à la personne concernée. C'est le
--  même parti que `deleted_at`, posé par V6 pour l'effacement RGPD.
--
--  POURQUOI UN MOTIF OBLIGATOIRE EN PRATIQUE
--  La colonne est nullable — une contrainte NOT NULL empêcherait de réactiver
--  proprement — mais le service refuse une suspension sans motif. Suspendre
--  sans écrire pourquoi produit une décision que plus personne ne peut
--  expliquer trois mois plus tard, ni à la personne, ni à un juge.
--
--  CE QUE LA SUSPENSION N'EST PAS
--  Elle n'efface rien et n'anonymise rien : les trajets passés continuent
--  d'exister, parce qu'ils engagent aussi les autres participants.
--  L'effacement reste l'affaire de l'article 17, qui suit un tout autre
--  chemin.
-- =============================================================================

ALTER TABLE users
    ADD COLUMN suspended_at      DATETIME(6)  DEFAULT NULL AFTER deleted_at,
    ADD COLUMN suspension_reason VARCHAR(255) DEFAULT NULL AFTER suspended_at;

-- Un index sur la colonne : la console d'administration compte les comptes
-- suspendus à chaque affichage, et ils sont par nature très minoritaires.
CREATE INDEX idx_users_suspended ON users (suspended_at);


-- ─────────────────────────────────────────────────────────────────────────────
--  Un compte de supervision pour la démonstration
-- ─────────────────────────────────────────────────────────────────────────────
--  V3 avait posé deux comptes ADMIN et aucun SUPER_ADMIN, si bien qu'aucun
--  écran de supervision de plateforme n'était démontrable. Le premier des deux
--  passe SUPER_ADMIN ; le second reste ADMIN, ce qui permet de montrer côte à
--  côte les deux portées : la plateforme entière et le seul cercle de ses
--  organisations.
--
--  La borne sur l'identifiant vise exactement la ligne posée par V3. Promouvoir
--  « le premier ADMIN trouvé » dépendrait d'un ordre de tri non garanti, et
--  donnerait un résultat différent d'une base à l'autre.
UPDATE users
   SET role = 'SUPER_ADMIN'
 WHERE id = 1001
   AND role = 'ADMIN';
