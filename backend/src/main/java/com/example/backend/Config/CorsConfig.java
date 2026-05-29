package com.example.backend.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    private static void appendCommaSeparatedOrigins(List<String> target, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .forEach(target::add);
    }

    @Value("${cors.allowed.origins:http://localhost:5173,http://localhost:3000}")
    private String corsAllowedOrigins;

    @Value("${frontend.url:http://localhost:5173,http://localhost:3000}")
    private String frontendUrl;

    @Bean
    public WebMvcConfigurer corsConfigurer() {

        return new WebMvcConfigurer() {

            @Override
            public void addCorsMappings(CorsRegistry registry) {

                List<String> allowedOriginsList = new ArrayList<>();
                appendCommaSeparatedOrigins(allowedOriginsList, frontendUrl);
                appendCommaSeparatedOrigins(allowedOriginsList, corsAllowedOrigins);
                if (allowedOriginsList.isEmpty()) {
                    allowedOriginsList.add("http://localhost:5173");
                    allowedOriginsList.add("http://localhost:3000");
                }

                String[] origins = allowedOriginsList.stream().distinct().toArray(String[]::new);

                registry.addMapping("/api/health/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "OPTIONS")
                        .allowedHeaders("*");

                registry.addMapping("/**")
                        .allowedOriginPatterns(origins)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
