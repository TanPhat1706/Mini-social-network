package com.example.backend.service; // Đảm bảo đúng tên package của bạn

import com.example.backend.model.RefreshToken;
import com.example.backend.model.User;
import com.example.backend.repository.RefreshTokenRepository;
import com.example.backend.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.NoSuchElementException;

@Service
public class RefreshTokenService {

    // Lấy thời gian hết hạn Refresh Token từ application.properties
    @Value("${jwt.refresh.expiration}")
    private Long refreshTokenDurationMs;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Tạo và lưu Refresh Token mới cho một User.
     *
     * @param userId ID của User
     * @return Đối tượng RefreshToken mới được tạo
     */
    @Transactional
    public RefreshToken createRefreshToken(Integer userId) {

        // 1. Tìm User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + userId));
        // 2. Tạo Refresh Token mới

        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.flush();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);

        // 3. Tính toán thời gian hết hạn
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));

        // 4. Tạo chuỗi token ngẫu nhiên
        refreshToken.setToken(UUID.randomUUID().toString());

        // 5. Lưu vào DB
        refreshToken = refreshTokenRepository.save(refreshToken);

        return refreshToken;
    }

    /**
     * Tìm kiếm Refresh Token trong DB bằng chuỗi token.
     *
     * @param token Chuỗi Refresh Token
     * @return Optional<RefreshToken>
     */
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * Kiểm tra xem Refresh Token đã hết hạn chưa.
     * Nếu hết hạn, xóa nó khỏi DB và ném ngoại lệ.
     *
     * @param token Đối tượng RefreshToken
     * @return Đối tượng RefreshToken nếu còn hợp lệ
     * @throws RuntimeException Nếu token đã hết hạn
     */
    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {

            refreshTokenRepository.delete(token);

            throw new RuntimeException(
                    "Refresh token was expired. Please make a new signin request. Token: " + token.getToken());
        }
        return token;
    }

    /**
     * Xóa Refresh Token của User khi họ đăng xuất (Logout).
     */
    @Transactional
    public int deleteByUserId(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + userId));

        return refreshTokenRepository.deleteByUser(user);
    }
}