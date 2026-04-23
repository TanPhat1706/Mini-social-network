package com.example.backend.Config;

import com.example.backend.User.JwtFilter;
import com.example.backend.Security.SecurityLoggingFilter; // 👈 thêm

import org.springframework.beans.factory.annotation.Autowired;
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

import java.util.List;

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Autowired
    private SecurityLoggingFilter loggingFilter; // 👈 thêm

    @Autowired
    private UserDetailsService userDetailsService;

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

        http
            .csrf(csrf -> csrf.disable())

            // ✅ CORS
            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration cfg = new CorsConfiguration();

                cfg.setAllowedOriginPatterns(List.of(
                        "http://localhost:5173",
                        "http://localhost:3000",
                        "https://*.ngrok-free.app"
                ));

                cfg.setAllowedMethods(List.of(
                        "GET", "POST", "PUT", "DELETE", "OPTIONS"
                ));

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
                        "/api/game/**"
                ).permitAll()

                .requestMatchers("/api/games/score").authenticated()
                .requestMatchers("/api/shop/**").authenticated()

                .anyRequest().authenticated()
            )

            // ✅ Stateless (JWT)
            .sessionManagement(sess ->
                sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .authenticationProvider(daoAuthenticationProvider())

            // 🔥 QUAN TRỌNG: thêm logging filter trước security
            .addFilterBefore(loggingFilter, UsernamePasswordAuthenticationFilter.class)

            // 🔐 JWT filter
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