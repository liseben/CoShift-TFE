-- =============================================================================
--  CoShift — mot de passe de test unifié
-- =============================================================================
--  Tous les comptes de test partagent désormais le mot de passe « 1234 ».
--
--  Pourquoi une migration séparée plutôt qu'une correction de V3 : Flyway
--  enregistre une somme de contrôle pour chaque migration appliquée. Modifier
--  V3 après coup ferait échouer la validation au démarrage suivant, avec le
--  message « Migration checksum mismatch », et l'application refuserait de se
--  lancer. Une migration additionnelle est la seule façon propre de corriger
--  des données déjà en base.
--
--  Empreinte BCrypt, coût 12, vérifiée contre la valeur « 1234 ». Le préfixe
--  $2a$ comme le coût 12 sont acceptés par BCryptPasswordEncoder, qui lit ces
--  paramètres dans l'empreinte elle-même.
--
--  RÉSERVÉ AU DÉVELOPPEMENT. Un mot de passe de quatre chiffres, partagé et
--  publié dans un dépôt, n'a évidemment pas vocation à quitter le poste de
--  développement ni la démonstration.
-- =============================================================================

UPDATE users
SET password = '$2a$12$6yZj7kPWGvK3uvyFczEGQ.Vx10mUij/AWFaV5sjsRKkqRCAb1JGzG';


-- Les comptes créés à la main avant le jeu de test n'avaient jamais été
-- vérifiés : isEnabled() bloquait donc leur connexion, quel que soit le mot
-- de passe. On les active, sans quoi les corriger n'aurait servi à rien.
UPDATE users
SET email_verified = b'1',
    verification_code = NULL,
    verification_code_expiry = NULL
WHERE id < 1000;


-- Le jeu de test conserve volontairement une part de comptes non vérifiés :
-- ils servent à éprouver l'écran de saisie du code à six chiffres et le
-- blocage à la connexion. Les rendre tous vérifiés supprimerait ces cas.
--
-- Pour tout activer malgré tout, exécuter à la main :
--     UPDATE users SET email_verified = b'1';
