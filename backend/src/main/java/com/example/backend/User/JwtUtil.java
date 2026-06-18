package com.example.backend.User;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {
    // Key bí mật (phải đủ dài, ít nhất 32 ký tự)
    private static final String SECRET = "daylasecretkeycuabanphaidairatdai32kytucaucaola";

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    // 🟢 SỬA: Nhận thêm tham số sessionId và nhét vào Token
    public String generateToken(String studentCode, String sessionId) {
        return Jwts.builder()
                .setSubject(studentCode)
                .claim("sessionId", sessionId) // <-- NHÉT SESSION ID VÀO ĐÂY
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // 1 ngày
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // 🟢 MỚI: Hàm giải mã để lấy sessionId ra khỏi Token
    public String extractSessionId(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(getSignKey()).build()
                    .parseClaimsJws(token).getBody().get("sessionId", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder().setSigningKey(getSignKey()).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public Date extractIssuedAt(String token) {
        return Jwts.parserBuilder().setSigningKey(getSignKey()).build()
                .parseClaimsJws(token).getBody().getIssuedAt();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}