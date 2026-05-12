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

    @Value("${cors.allowed.origins:http://localhost:5173,http://localhost:3000}")
    private String corsAllowedOrigins;

    @Value("${frontend.url:http://localhost:5173,http://localhost:3000}")
    private String frontendUrl;

    @Bean
    public WebMvcConfigurer corsConfigurer() {

        return new WebMvcConfigurer() {

            @Override
            public void addCorsMappings(CorsRegistry registry) {

                // Kết hợp FRONTEND_URL với các CORS origins khác
                List<String> allowedOriginsList = new ArrayList<>();

                // Thêm FRONTEND_URL (S3)
                if (frontendUrl != null && !frontendUrl.isEmpty()) {
                    allowedOriginsList.add(frontendUrl);
                }

                // Parse comma-separated CORS origins from environment variable
                if (corsAllowedOrigins != null && !corsAllowedOrigins.isEmpty()) {
                    Arrays.stream(corsAllowedOrigins.split(","))
                            .map(String::trim)
                            .filter(origin -> !origin.isEmpty())
                            .forEach(allowedOriginsList::add);
                }

                String[] origins = allowedOriginsList.toArray(new String[0]);

                var registration = registry.addMapping("/**")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                        .allowedHeaders("*")
                        .allowCredentials(true);

                // Support wildcard origins safely with credentials
                if (Arrays.stream(origins).anyMatch("*"::equals)) {
                    registration.allowedOriginPatterns(origins);
                } else {
                    registration.allowedOrigins(origins);
                }
            }
        };
    }
}
