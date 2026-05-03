package com.example.backend.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig {
    @Value("${cors.allowed.origins}")
    private String corsAllowedOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Parse comma-separated CORS origins from environment variable
                String[] allowedOrigins = Arrays.stream(corsAllowedOrigins.split(","))
                        .map(String::trim)
                        .toArray(String[]::new);
                
                registry.addMapping("/**")
<<<<<<< HEAD
                        .allowedOrigins(allowedOrigins)
=======
                        .allowedOriginPatterns(
                                "http://localhost:5173",
                                "http://localhost:3000",
                                "http://127.0.0.1:5173",
                                "https://*.ngrok-free.app",
                                "https://*.ngrok.io"
                        )
>>>>>>> main
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}