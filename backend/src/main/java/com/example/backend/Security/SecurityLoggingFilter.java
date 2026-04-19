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

    private static final Pattern SQLI =
            Pattern.compile("('.+--)|(--)|(\\b(select|union|insert|delete|drop)\\b)", Pattern.CASE_INSENSITIVE);

    private static final Pattern XSS =
            Pattern.compile("(<script>|javascript:|onerror=)", Pattern.CASE_INSENSITIVE);

    private static final Pattern PATH_TRAVERSAL =
            Pattern.compile("(\\.\\./|\\.\\.\\\\)");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String ip = request.getRemoteAddr();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String ua = request.getHeader("User-Agent");

        String payload = uri + (query != null ? "?" + query : "");

        if (payload != null) {

            if (SQLI.matcher(payload).find()) {
                log.warn("SQL_INJECTION | ip={} | uri={} | query={} | ua={}",
                        ip, uri, query, ua);
            }

            if (XSS.matcher(payload).find()) {
                log.warn("XSS_ATTACK | ip={} | uri={} | query={} | ua={}",
                        ip, uri, query, ua);
            }

            if (PATH_TRAVERSAL.matcher(payload).find()) {
                log.warn("PATH_TRAVERSAL | ip={} | uri={} | query={} | ua={}",
                        ip, uri, query, ua);
            }
        }

        filterChain.doFilter(request, response);
    }
}
