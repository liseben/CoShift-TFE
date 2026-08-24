package com.coshift.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
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

        /* ─── Interface embarquée (déploiement à une seule URL) ───────────────
           En production, le frontend construit par Vite est copié dans
           classpath:/static/ et le backend sert tout : une seule origine, donc
           ni CORS ni deuxième hébergement, et la PWA installable depuis la
           même adresse que l'API.

           Une application monopage a des adresses qui ne correspondent à aucun
           fichier — /trips/search, /actus/… n'existent que dans le routeur du
           navigateur. Un rechargement sur ces chemins doit servir index.html
           et laisser React Router faire le reste ; c'est ce que fait le
           résolveur ci-dessous quand la ressource demandée n'existe pas.

           Les chemins de l'API n'y retombent jamais : /api/**, /uploads/** et
           /actuator/** sont pris avant par leurs contrôleurs et gestionnaires.
           En développement, classpath:/static/ ne contient pas index.html et
           le repli renvoie 404 comme avant — rien ne change pour Vite. */
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location)
                            throws IOException {
                        Resource ressource = location.createRelative(resourcePath);
                        if (ressource.exists() && ressource.isReadable()) {
                            return ressource;
                        }
                        Resource index = new ClassPathResource("/static/index.html");
                        return index.exists() ? index : null;
                    }
                });
    }
}
