-- =============================================================================
--  CoShift — réinitialisation du mot de passe (F6)
-- =============================================================================
--  La page de connexion proposait « Mot de passe oublié ? » et appelait
--  /api/auth/forgot-password, qui n'existait pas : l'appel finissait en 404 et
--  l'écran affichait malgré tout un message de succès. Ces deux colonnes
--  portent le code à six chiffres envoyé par courriel et sa date d'expiration.
--
--  Colonnes distinctes de verification_code / verification_code_expiry, et non
--  réutilisation de celles-ci : les deux flux peuvent se chevaucher — un compte
--  jamais vérifié peut demander une réinitialisation — et un code partagé
--  laisserait un code de réinitialisation activer un compte, ou l'inverse.
--
--  Aucune valeur par défaut : NULL signifie « aucune demande en cours », état
--  normal de la quasi-totalité des lignes.
-- =============================================================================

ALTER TABLE users
    ADD COLUMN password_reset_code VARCHAR(6) DEFAULT NULL AFTER verification_code_expiry,
    ADD COLUMN password_reset_expiry DATETIME DEFAULT NULL AFTER password_reset_code;
