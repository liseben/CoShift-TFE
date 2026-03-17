package com.coshift.api;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;

@SpringBootApplication
@EnableScheduling
public class CoshiftBackendApplication {

    public static void main(String[] args) {
        
        // 1. Logique intelligente pour trouver le .env
        Dotenv dotenv;
        
        // Cas A : On lance depuis le dossier coshift-backend (ex: en prod)
        if (new File(".env").exists()) {
            System.out.println("✅ .env trouvé à la racine d'exécution.");
            dotenv = Dotenv.configure().load();
        } 
        // Cas B : On lance depuis la racine du projet (Ton cas actuel avec VS Code)
        else if (new File("./coshift-backend/.env").exists()) {
            System.out.println("✅ .env trouvé dans le sous-dossier coshift-backend.");
            dotenv = Dotenv.configure().directory("./coshift-backend").load();
        } 
        // Cas C : Pas de .env (Erreur probable)
        else {
            System.out.println("⚠️ AUCUN FICHIER .ENV TROUVÉ ! Vérifie l'emplacement.");
            dotenv = Dotenv.configure().ignoreIfMissing().load();
        }

        // 2. Injection dans les variables système Java
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });

        // 3. Lancement de Spring Boot
        SpringApplication.run(CoshiftBackendApplication.class, args);
    }
}