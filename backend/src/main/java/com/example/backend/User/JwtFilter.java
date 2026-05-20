package com.example.backend.User;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.ZoneId;
import java.util.Date;

@Component
public class JwtFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private CustomUserDetailsService userDetailsService;
    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(token);
            } catch (Exception e) {
                logger.error("Lỗi trích xuất token", e);
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (jwtUtil.validateToken(token)) {
                // session revocation: ensure token was issued after any password reset
                try {
                    Date issued = jwtUtil.extractIssuedAt(token);
                    User userEntity = userRepository.findByStudentCode(username).orElse(null);
                    if (userEntity != null && userEntity.getLastPasswordResetAt() != null) {
                        ZoneId zid = ZoneId.systemDefault();
                        Date pwReset = Date.from(userEntity.getLastPasswordResetAt().atZone(zid).toInstant());
                        if (issued.before(pwReset)) {
                            // token issued before password reset -> reject
                            filterChain.doFilter(request, response);
                            return;
                        }
                    }
                } catch (Exception e) {
                    // ignore and continue validation
                }

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}