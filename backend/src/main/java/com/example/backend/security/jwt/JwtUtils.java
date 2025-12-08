package com.example.backend.security.jwt; // Đảm bảo đúng tên package của bạn

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.example.backend.security.services.UserDetailsImpl;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    // Lấy secret key từ application.properties
    @Value("${jwt.secret}")
    private String jwtSecret;

    // Lấy thời gian hết hạn Access Token (ví dụ: 24h)
    @Value("${jwt.expiration}")
    private int jwtExpirationMs;

    // Lấy thời gian hết hạn Refresh Token (ví dụ: 7 ngày)
    @Value("${jwt.refresh.expiration}")
    private int jwtRefreshExpirationMs;

    /**
     * Tạo khóa ký bí mật từ chuỗi base64.
     */
    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    // --- 1. ACCESS TOKEN ---

    /**
     * Tạo Access Token từ thông tin xác thực.
     */
    public String generateJwtToken(Authentication authentication) {

        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername()) // Student Code
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Tạo Access Token trực tiếp từ studentCode (dùng cho Refresh Token).
     */
    public String generateJwtTokenFromUsername(String studentCode) {
        return Jwts.builder()
                .setSubject(studentCode) // Student Code
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Trích xuất studentCode từ Access Token.
     */
    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    // --- 2. REFRESH TOKEN (CÓ THỂ KHÔNG DÙNG NẾU LƯU CHUỖI RANDOM TRONG DB) ---

    /**
     * Tạo Refresh Token từ studentCode.
     * Phương thức này chỉ được dùng nếu bạn CHỌN lưu Refresh Token dưới dạng JWT.
     * (Trong hướng dẫn trước, chúng ta đang lưu chuỗi random trong DB và sử dụng
     * AuthService để quản lý).
     */
    public String generateRefreshToken(String studentCode) {
        return Jwts.builder()
                .setSubject(studentCode)
                .setIssuedAt(new Date())
                // Dùng thời gian hết hạn dài hơn
                .setExpiration(new Date((new Date()).getTime() + jwtRefreshExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    // --- 3. VALIDATE TOKEN ---

    /**
     * Kiểm tra Access Token có hợp lệ không.
     */
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parse(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}