package com.earacg.earaconnect.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.PostConstruct;
import java.io.File;

@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        // Create upload directory if it doesn't exist
        File uploadDirectory = new File(uploadDir);
        if (!uploadDirectory.exists()) {
            boolean created = uploadDirectory.mkdirs();
            if (created) {
                System.out.println("Upload directory created: " + uploadDirectory.getAbsolutePath());
            } else {
                System.err.println("Failed to create upload directory: " + uploadDirectory.getAbsolutePath());
            }
        }

        // Create profile pictures directory if it doesn't exist
        File profilePicturesDirectory = new File(uploadDir + "/profile-pictures");
        if (!profilePicturesDirectory.exists()) {
            boolean created = profilePicturesDirectory.mkdirs();
            if (created) {
                System.out.println("Profile pictures directory created: " + profilePicturesDirectory.getAbsolutePath());
            } else {
                System.err.println("Failed to create profile pictures directory: " + profilePicturesDirectory.getAbsolutePath());
            }
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve uploaded files statically
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + uploadDir + "/")
                .setCachePeriod(3600);
        
        // Serve profile pictures with specific cache settings
        registry.addResourceHandler("/files/profile-pictures/**")
                .addResourceLocations("file:" + uploadDir + "/profile-pictures/")
                .setCachePeriod(86400); // 24 hours cache for profile pictures
    }
}