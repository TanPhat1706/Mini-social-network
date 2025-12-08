package com.example.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.backend.security.jwt.AuthEntryPointJwt;
import com.example.backend.security.jwt.AuthTokenFilter;
import com.example.backend.security.services.UserDetailsServiceImpl;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity // Cho phép bảo mật dựa trên Method (như @PreAuthorize)
public class SecurityConfig {

    @Autowired
    UserDetailsServiceImpl userDetailsService; // Service để load User từ DB

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler; // Xử lý lỗi 401 Unauthorized

    /**
     * @Bean: Khởi tạo và đăng ký AuthTokenFilter để kiểm tra JWT trong mỗi request.
     */
    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    /**
     * @Bean: Định nghĩa Authentication Provider, dùng để xác thực người dùng.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    /**
     * @Bean: Lấy AuthenticationManager từ cấu hình.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * @Bean: Định nghĩa PasswordEncoder, sử dụng BCrypt để băm mật khẩu.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * @Bean: Cấu hình CORS để cho phép frontend (React) gọi API.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Lấy từ application.properties hoặc list cứng các domain frontend của bạn
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://127.0.0.1:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * @Bean: Cấu hình chuỗi lọc bảo mật chính (Security Filter Chain).
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable) // Tắt CSRF vì sử dụng JWT (STATELESS)
                .cors(Customizer.withDefaults()) // Cho phép CORS với cấu hình ở trên

                // Xử lý ngoại lệ xác thực (Authentication Entry Point)
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))

                // Quản lý Session: STATELESS (không lưu trữ trạng thái người dùng trên server)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Phân quyền cho từng Request
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll() // Cho phép truy cập công khai các API
                                                                     // Login/Register/Refresh
                        .requestMatchers("/api/test/**").permitAll() // Thêm các API test nếu cần
                        .anyRequest().authenticated() // Tất cả các request khác phải được xác thực
                );

        // Đăng ký Authentication Provider (sử dụng UserDetailsService và
        // PasswordEncoder)
        http.authenticationProvider(authenticationProvider());

        // Thêm JWT Filter trước UsernamePasswordAuthenticationFilter (Spring Security
        // mặc định)
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}