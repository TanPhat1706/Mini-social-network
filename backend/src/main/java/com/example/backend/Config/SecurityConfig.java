package com.example.backend.Config;

import com.example.backend.Security.SecurityLoggingFilter;
import com.example.backend.User.JwtFilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Autowired
    private SecurityLoggingFilter loggingFilter;

    @Autowired
    private UserDetailsService userDetailsService;

    @Value("${cors.allowed.origins:http://localhost:5173,http://localhost:3000}")
    private String corsAllowedOrigins;

    @Value("${frontend.url:http://localhost:5173,http://localhost:3000}")
    private String frontendUrl;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // Kết hợp FRONTEND_URL với các CORS origins khác
        List<String> allowedOrigins = new ArrayList<>();

        // Thêm FRONTEND_URL (S3)
        if (frontendUrl != null && !frontendUrl.isEmpty()) {
            allowedOrigins.add(frontendUrl);
        }

        // Parse CORS origins from .env
        if (corsAllowedOrigins != null && !corsAllowedOrigins.isEmpty()) {
            Arrays.stream(corsAllowedOrigins.split(","))
                    .map(String::trim)
                    .filter(origin -> !origin.isEmpty())
                    .forEach(allowedOrigins::add);
        }

        http
                .csrf(csrf -> csrf.disable())

                // ✅ CORS
                .cors(cors -> cors.configurationSource(request -> {

                    CorsConfiguration cfg = new CorsConfiguration();

                    // Support wildcard origins safely
                    if (allowedOrigins.stream().anyMatch("*"::equals)) {
                        cfg.setAllowedOriginPatterns(allowedOrigins);
                    } else {
                        cfg.setAllowedOrigins(allowedOrigins);
                    }

                    cfg.setAllowedMethods(List.of(
                            "GET",
                            "POST",
                            "PUT",
                            "DELETE",
                            "OPTIONS",
                            "PATCH"));

                    cfg.setAllowedHeaders(List.of("*"));
                    cfg.setAllowCredentials(true);

                    return cfg;
                }))

                // ✅ Authorization
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/uploads/**",
                                "/ws/**",
                                "/api/games/leaderboard/**",
                                "/api/health/**",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api/game/**")
                        .permitAll()

                        .requestMatchers("/api/games/score").authenticated()

                        .requestMatchers("/api/shop/**").authenticated()

                        .anyRequest().authenticated())

                // ✅ Stateless JWT
                .sessionManagement(sess ->
                        sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authenticationProvider(daoAuthenticationProvider())

                // ✅ Logging filter
                .addFilterBefore(loggingFilter, UsernamePasswordAuthenticationFilter.class)

                // ✅ JWT filter
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();

        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }
}