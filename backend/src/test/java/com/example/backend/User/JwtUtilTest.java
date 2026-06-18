package com.example.backend.User;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String SECRET = "daylasecretkeycuabanphaidairatdai32kytucaucaola";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
    }

    @Test
    @DisplayName("Tạo và trích xuất Token hợp lệ thành công")
    void generateAndExtractToken_shouldWorkProperly() {
        String token = jwtUtil.generateToken("SV001", "session-123");

        assertNotNull(token);
        assertEquals("SV001", jwtUtil.extractUsername(token));
        assertEquals("session-123", jwtUtil.extractSessionId(token));
        assertNotNull(jwtUtil.extractIssuedAt(token));
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("Bóc tách Token hỏng/sai chữ ký phải trả về null hoặc false")
    void extractFromInvalidToken_shouldReturnNullOrFalse() {
        String badToken = "eyJhbGciOiJIUzI1NiJ9.badpayload.badsignature";

        assertNull(jwtUtil.extractSessionId(badToken));
        assertFalse(jwtUtil.validateToken(badToken));
        assertThrows(Exception.class, () -> jwtUtil.extractUsername(badToken));
        assertThrows(Exception.class, () -> jwtUtil.extractIssuedAt(badToken));
    }

    @Test
    @DisplayName("Validate Token hết hạn phải trả về false")
    void validateToken_whenExpired_shouldReturnFalse() {
        // Tự tạo một token đã hết hạn (lùi về 1 ngày trước)
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        String expiredToken = Jwts.builder()
                .setSubject("SV001")
                .setExpiration(new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24)) // Past
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        assertFalse(jwtUtil.validateToken(expiredToken));
    }
}