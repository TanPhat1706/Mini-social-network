package com.example.backend.repository; // Đảm bảo đúng tên package của bạn

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying; // Cần thiết cho các thao tác sửa đổi (DELETE)
import org.springframework.stereotype.Repository;

import com.example.backend.model.RefreshToken;
import com.example.backend.model.User;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Tìm kiếm RefreshToken dựa trên chuỗi token.
     * Spring Data JPA tự động tạo truy vấn: SELECT * FROM refresh_tokens WHERE
     * token = ?
     *
     * @param token Chuỗi Refresh Token
     * @return Optional<RefreshToken>
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Xóa tất cả các Refresh Token liên quan đến một User cụ thể.
     * Cần @Modifying và @Transactional để thực hiện thao tác DELETE.
     *
     * @param user Đối tượng User
     * @return Số lượng bản ghi đã bị xóa
     */
    @Modifying
    int deleteByUser(User user);

    // Bạn có thể thêm các phương thức khác nếu cần, ví dụ:
    // Optional<RefreshToken> findByUser(User user);
}