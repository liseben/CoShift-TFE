-- =============================================================================
--  V10 — Le domaine de courriel, clé du rattachement à une organisation
-- =============================================================================
--  CoShift ne met pas en relation des inconnus : il s'adresse à des gens qui
--  partagent déjà un employeur, un campus ou un événement. Encore faut-il que
--  le logiciel sache lequel. Jusqu'ici, seul le SQL de démonstration rattachait
--  les comptes à leur organisation ; toute inscription passée par l'application
--  produisait un compte sans rattachement, donc un trajet sans organisation.
--
--  Le rattachement se lit dans l'adresse professionnelle : quelqu'un qui écrit
--  depuis @solvantis.be travaille chez Solvantis. Cette colonne rend la règle
--  explicite au lieu de la déduire du slug.
--
--  Pourquoi pas simplement `CONCAT(slug, '.be')` dans le code
--  ---------------------------------------------------------
--  Le slug est un fragment d'URL, le domaine est une propriété de
--  l'organisation dans le monde réel. Les faire coïncider marche sur ce jeu de
--  données parce qu'il a été écrit ainsi, et cesse de marcher au premier client
--  dont le domaine ne ressemble pas à son slug — « Groupe Verhaegen » pourrait
--  parfaitement écrire depuis @verhaegen-group.com. Une règle qui n'est vraie
--  que sur les données d'essai est une règle fausse.
--
--  Le domaine est unique : deux organisations ne peuvent pas revendiquer la
--  même adresse professionnelle, sans quoi le rattachement serait ambigu et le
--  cercle de visibilité fuiterait de l'une vers l'autre. La colonne reste
--  nullable — une organisation peut exister avant que son domaine soit connu,
--  et MySQL admet plusieurs NULL dans un index unique.
-- =============================================================================

ALTER TABLE organizations
    ADD COLUMN email_domain VARCHAR(255) DEFAULT NULL AFTER slug;

-- Les douze organisations du jeu de démonstration (V3) ont toutes été écrites
-- avec un domaine de la forme <slug>.be — vérifié : 119 comptes sur 122 en
-- proviennent, les trois autres étant les comptes d'essai @coshift.be, qui
-- n'appartiennent volontairement à aucune organisation.
--
-- La borne sur l'identifiant est délibérée : elle vise exactement les lignes
-- posées par V3. Appliquer la formule à toute la table inventerait un domaine
-- pour chaque organisation créée plus tard, et cette invention serait ensuite
-- indiscernable d'une donnée saisie.
UPDATE organizations
   SET email_domain = CONCAT(slug, '.be')
 WHERE id BETWEEN 1001 AND 1012;

ALTER TABLE organizations
    ADD UNIQUE KEY uk_organizations_email_domain (email_domain);


-- ─────────────────────────────────────────────────────────────────────────────
--  Rattrapage des trajets publiés avant que le code ne pose l'organisation
-- ─────────────────────────────────────────────────────────────────────────────
--  Le rattachement devient une propriété du code à partir de cette version.
--  Restent les trajets déjà en base sans organisation : les laisser ainsi les
--  rendrait invisibles à tout le monde une fois le cercle appliqué, alors que
--  leur conducteur, lui, est bien rattaché.
--
--  La jointure sur organization_members n'est pas redondante avec celle sur le
--  domaine : elle interdit de rattacher un trajet à une organisation dont le
--  conducteur n'est pas membre. Le domaine dit d'où vient la personne,
--  l'appartenance dit ce qui lui est ouvert — c'est la seconde qui fait foi.
--
--  `updated_at` n'est pas touché à dessein. Cette écriture répare une donnée
--  manquante, elle ne traduit aucune décision du conducteur : lui faire porter
--  une date de modification laisserait croire qu'il a changé son trajet.
UPDATE trips t
  JOIN users u              ON u.id = t.driver_id
  JOIN organizations o      ON o.email_domain = SUBSTRING_INDEX(u.email, '@', -1)
  JOIN organization_members m ON m.user_id = u.id AND m.organization_id = o.id
   SET t.organization_id = o.id
 WHERE t.organization_id IS NULL;
