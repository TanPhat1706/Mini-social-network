package com.example.backend.Config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
@Slf4j
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class WebMvcConfig implements WebMvcConfigurer {
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
            String uploadAbsolutePath = uploadPath.toString();
            
            if (!Files.exists(uploadPath)) {
                log.warn("Upload directory does not exist, creating: {}", uploadAbsolutePath);
                Files.createDirectories(uploadPath);
            }
            
            String resourceLocation = "file:///" + uploadAbsolutePath.replace("\\", "/") + "/";
            
            // log.info("Configured static resource handler for /uploads/**");
            // log.info("Upload directory: {}", uploadAbsolutePath);
            // log.info("Resource location: {}", resourceLocation);
            
            registry.addResourceHandler("/uploads/**")
                    .addResourceLocations(resourceLocation)
                    .setCachePeriod(3600);
                    
        } catch (Exception e) {
            log.error("Failed to configure upload directory resource handler", e);
            throw new RuntimeException("Failed to configure upload directory: " + e.getMessage(), e);
        }
    }
}
