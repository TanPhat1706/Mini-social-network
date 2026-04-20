package com.example.backend.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

@Component
public class SecurityLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("SECURITY");

    // ===== Detect patterns =====
    private static final Pattern SQLI = Pattern.compile(
            "(\\b(select|union|insert|delete|drop|or|and)\\b.*=)|(--)|(')",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern XSS = Pattern.compile(
            "(<script>|javascript:|onerror=|onload=|alert\\()",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PATH_TRAVERSAL = Pattern.compile(
            "(\\.\\./|\\.\\.\\\\)",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
                System.out.println(">>> SecurityLoggingFilter triggered");


        String ip = getClientIp(request);
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String ua = request.getHeader("User-Agent");

        String payload = buildPayload(uri, query);

        try {
            if (payload != null && !payload.isEmpty()) {

                if (SQLI.matcher(payload).find()) {
                    log.warn("type=SQL_INJECTION ip={} method={} uri={} query={} ua={}",
                            ip, method, uri, safe(query), safe(ua));
                }

                if (XSS.matcher(payload).find()) {
                    log.warn("type=XSS_ATTACK ip={} method={} uri={} query={} ua={}",
                            ip, method, uri, safe(query), safe(ua));
                }

                if (PATH_TRAVERSAL.matcher(payload).find()) {
                    log.warn("type=PATH_TRAVERSAL ip={} method={} uri={} query={} ua={}",
                            ip, method, uri, safe(query), safe(ua));
                }
            }
        } catch (Exception ex) {
            // Không để logging làm crash request
            log.error("type=SECURITY_LOG_ERROR message={}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    // ===== Helper methods =====

    private String buildPayload(String uri, String query) {
        if (query == null) return uri;
        return uri + "?" + query;
    }

    private String safe(String value) {
        return value == null ? "" : value.replaceAll("[\\n\\r]", "");
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0];
        }
        return request.getRemoteAddr();
    }
}