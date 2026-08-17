package com.coshift.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Vérifie que le contexte Spring se construit entièrement : toutes les
 * dépendances des beans se résolvent et la configuration est cohérente.
 *
 * <p>{@code @ActiveProfiles("test")} est indispensable : les tests ne passent
 * pas par {@code CoshiftBackendApplication.main()}, seul endroit où le fichier
 * {@code .env} est chargé. Sans le profil de test, les placeholders
 * {@code ${DB_USERNAME}}, {@code ${JWT_SECRET_KEY}} et consorts restent
 * littéraux et le démarrage échoue sur un « Access denied for user
 * '${DB_USERNAME}' ».</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class CoshiftBackendApplicationTests {

	@Test
	@DisplayName("Le contexte Spring démarre sans erreur")
	void contextLoads() {
	}

}
