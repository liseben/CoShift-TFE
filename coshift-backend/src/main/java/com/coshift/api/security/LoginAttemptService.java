package com.coshift.api.security;

import com.coshift.api.exception.TooManyRequestsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Freine les tentatives répétées sur les points d'entrée devinables.
 *
 * <p>Trois opérations se prêtent aux essais successifs : la connexion (mot de
 * passe), la vérification d'adresse et la réinitialisation (code à six chiffres,
 * soit un million de combinaisons — quelques heures suffisent à les parcourir
 * toutes si rien ne s'y oppose). Après {@value #MAX_ATTEMPTS} échecs, la clé est
 * bloquée {@value #LOCK_MINUTES} minutes.</p>
 *
 * <h2>Choix de la clé : adresse IP <em>et</em> compte visé</h2>
 *
 * <p>Compter par compte seul permettrait à n'importe qui de verrouiller le
 * compte d'un tiers en échouant volontairement cinq fois — un déni de service
 * trivial. Compter par IP seule bloquerait tout un site derrière une même
 * sortie NAT dès que cinq collègues se trompent, cas courant pour une
 * plateforme d'entreprise. La combinaison des deux arrête l'attaque réelle,
 * celle qui essaie de nombreux mots de passe sur un compte depuis une machine,
 * sans pénaliser les voisins de l'attaquant.</p>
 *
 * <p><strong>Limite assumée :</strong> une attaque répartie sur de nombreuses
 * adresses IP n'est pas couverte. La contrer demanderait un compteur partagé
 * (Redis) et une politique globale par compte, hors du périmètre d'un
 * déploiement mono-instance.</p>
 *
 * <p>Le compteur vit en mémoire : il repart à zéro au redémarrage, et ne se
 * partage pas entre plusieurs instances.</p>
 */
@Service
@Slf4j
public class LoginAttemptService {

    /** Nombre d'échecs tolérés avant blocage. */
    public static final int MAX_ATTEMPTS = 5;

    /** Durée du blocage, en minutes. */
    public static final int LOCK_MINUTES = 15;

    /** Au-delà de ce délai sans échec, le compteur repart de zéro. */
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private static final Duration LOCK = Duration.ofMinutes(LOCK_MINUTES);

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    private static final class Counter {
        int failures;
        Instant windowStart;
        Instant lockedUntil;

        Counter(Instant now) {
            this.windowStart = now;
        }

        boolean isLocked(Instant now) {
            return lockedUntil != null && lockedUntil.isAfter(now);
        }
    }

    /** Compose la clé de comptage à partir de l'adresse appelante et du compte visé. */
    public String key(String clientIp, String email) {
        String account = (email == null) ? "" : email.trim().toLowerCase(Locale.ROOT);
        return clientIp + "|" + account;
    }

    /**
     * Refuse la tentative si la clé est bloquée.
     *
     * @throws TooManyRequestsException tant que le blocage court
     */
    public void assertNotBlocked(String key) {
        Counter counter = counters.get(key);
        if (counter == null) return;

        Instant now = Instant.now();
        synchronized (counter) {
            if (!counter.isLocked(now)) return;

            // Arrondi à la minute supérieure : annoncer « 0 minute » alors que le
            // blocage court encore serait incompréhensible pour l'utilisateur.
            long minutes = Duration.between(now, counter.lockedUntil).toMinutes() + 1;
            throw new TooManyRequestsException(
                    "Trop de tentatives infructueuses. Réessayez dans " + minutes + " minute"
                            + (minutes > 1 ? "s" : "") + ".");
        }
    }

    /** Enregistre un échec, et déclenche le blocage au seuil atteint. */
    public void recordFailure(String key) {
        Instant now = Instant.now();
        counters.compute(key, (k, counter) -> {
            // Fenêtre écoulée sans blocage en cours : on repart d'un compteur neuf,
            // sans quoi cinq erreurs étalées sur plusieurs mois finiraient par bloquer.
            if (counter == null
                    || (!counter.isLocked(now) && counter.windowStart.plus(WINDOW).isBefore(now))) {
                counter = new Counter(now);
            }
            counter.failures++;
            if (counter.failures >= MAX_ATTEMPTS) {
                counter.lockedUntil = now.plus(LOCK);
                counter.failures = 0;
                counter.windowStart = now;
                log.warn("Blocage de {} minutes après {} tentatives infructueuses (clé {})",
                        LOCK_MINUTES, MAX_ATTEMPTS, k);
            }
            return counter;
        });
    }

    /** Efface le compteur après une opération réussie. */
    public void reset(String key) {
        counters.remove(key);
    }

    /**
     * Purge les compteurs dormants.
     *
     * <p>Sans elle, la table grossirait indéfiniment au fil des adresses IP
     * rencontrées : chaque tentative isolée y laisserait une entrée définitive.</p>
     */
    @Scheduled(cron = "${app.security.attempts-purge-cron:0 0 * * * *}")
    public void purgeExpired() {
        Instant now = Instant.now();
        int before = counters.size();
        counters.values().removeIf(counter -> {
            synchronized (counter) {
                return !counter.isLocked(now) && counter.windowStart.plus(WINDOW).isBefore(now);
            }
        });
        int removed = before - counters.size();
        if (removed > 0) {
            log.debug("{} compteur(s) de tentatives purgé(s).", removed);
        }
    }
}
