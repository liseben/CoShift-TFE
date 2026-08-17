package com.coshift.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads/avatars}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Le dossier d'upload est servi tel quel, sans reconstruire le chemin à la main.
        // L'ancienne version prenait le parent de "uploads/avatars" puis y recollait
        // "/uploads/", ce qui pointait vers "uploads/uploads/" : toutes les photos de
        // profil renvoyaient 404.
        Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();

        // toUri() produit une URL "file:///..." valide sur Windows comme sur Linux.
        // Le slash final n'est ajouté que si le dossier existe déjà : on le force,
        // car addResourceLocations l'exige pour traiter l'emplacement comme un dossier.
        String location = uploadRoot.toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }

        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations(location);
    }
}
