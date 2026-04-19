// 

package com.example.backend.Config;

import com.example.backend.User.JwtFilter;
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

            // ✅ FIX CORS cho localhost + ngrok
            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration cfg = new CorsConfiguration();

                // ❗ DÙNG setAllowedOriginPatterns thay vì setAllowedOrigins
                cfg.setAllowedOriginPatterns(List.of(
                        "http://localhost:5173",
                        "https://*.ngrok-free.app"
                ));

                cfg.setAllowedMethods(List.of(
                        "GET", "POST", "PUT", "DELETE", "OPTIONS"
                ));

                cfg.setAllowedHeaders(List.of("*"));
                cfg.setAllowCredentials(true);

                return cfg;
            }))

            .authorizeHttpRequests(auth -> auth

                // 1. Public APIs (giữ nguyên)
                .requestMatchers(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/uploads/**",
                        "/ws/**",
                        "/api/games/leaderboard/**",
                        "/api/game/**"
                ).permitAll()

                // 2. Authenticated APIs (giữ nguyên)
                .requestMatchers("/api/games/score").authenticated()
                .requestMatchers("/api/shop/**").authenticated()

                // 3. Admin (giữ nguyên - comment)
                // .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // 4. Tất cả request khác cần login
                .anyRequest().authenticated()
            )

            .sessionManagement(sess ->
                sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .authenticationProvider(daoAuthenticationProvider())

            // JWT filter (giữ nguyên)
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