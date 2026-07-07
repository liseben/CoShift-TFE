package com.coshift.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads/avatars}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Sert les fichiers du dossier uploads/ via l'URL /uploads/**
        String absolutePath = Paths.get(uploadDir).toAbsolutePath().getParent().toString()
                .replace("\\", "/");
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolutePath + "/uploads/");
    }
}
