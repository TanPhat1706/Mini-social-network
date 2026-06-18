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
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;

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

    private List<String> resolveAllowedOrigins() {
        List<String> origins = new ArrayList<>();
        appendCommaSeparatedOrigins(origins, frontendUrl);
        appendCommaSeparatedOrigins(origins, corsAllowedOrigins);
        if (origins.isEmpty()) {
            origins.add("http://localhost:5173");
            origins.add("http://localhost:3000");
        }
        return origins.stream().distinct().toList();
    }

    private static void appendCommaSeparatedOrigins(List<String> target, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .forEach(target::add);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> allowedOrigins = resolveAllowedOrigins();

        CorsConfiguration healthCors = new CorsConfiguration();
        healthCors.setAllowedOriginPatterns(List.of("*"));
        healthCors.setAllowedMethods(List.of("GET", "OPTIONS"));
        healthCors.setAllowedHeaders(List.of("*"));
        healthCors.setAllowCredentials(false);

        CorsConfiguration appCors = new CorsConfiguration();
        appCors.setAllowedOriginPatterns(allowedOrigins);
        appCors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        appCors.setAllowedHeaders(List.of("*"));
        appCors.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/health/**", healthCors);
        source.registerCorsConfiguration("/**", appCors);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ✅ Authorization
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers(
                                "/api/internal/test/**",
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/uploads/**",
                                "/ws/**",
                                "/api/games/leaderboard/**",
                                "/api/health/**",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api/game/**",
                                "/error")
                        .permitAll()

                        .requestMatchers("/api/games/score").authenticated()

                        // Only users with ROLE_ADMIN may create shop items
                        .requestMatchers(HttpMethod.POST, "/api/shop/items").hasRole("ADMIN")

                        // Other shop endpoints require authentication (users can view/buy/equip)
                        .requestMatchers("/api/shop/**").authenticated()

                        // Only users with ROLE_ADMIN may access admin endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        .anyRequest().authenticated())

                // ✅ Stateless JWT
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

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