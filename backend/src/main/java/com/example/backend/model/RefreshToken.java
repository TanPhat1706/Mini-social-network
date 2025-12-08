package com.example.backend.model; // Đảm bảo đúng tên package của bạn

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
@Data // Tạo getters, setters, toString, equals và hashCode (từ Lombok)
@NoArgsConstructor // Tạo constructor không tham số (từ Lombok)
public class RefreshToken {

    @Id
    // Sử dụng GenerationType.IDENTITY nếu bạn dùng SQL Server để tự động tăng
    // (AUTO-INCREMENT)
    // Nếu dùng GenerationType.AUTO/TABLE/SEQUENCE, cần cấu hình thêm
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Liên kết 1-1 với bảng User.
     * Refresh Token chỉ thuộc về một User duy nhất.
     */
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    /**
     * Chuỗi Refresh Token thực tế.
     * Phải là UNIQUE và NOT NULL.
     */
    @Column(nullable = false, unique = true)
    private String token;

    /**
     * Thời gian hết hạn của Refresh Token.
     * Sử dụng Instant để lưu trữ thời gian ở UTC, phù hợp cho việc kiểm tra so
     * sánh.
     */
    @Column(nullable = false)
    private Instant expiryDate;

    // Các getters/setters/constructor khác được tự động tạo bởi Lombok @Data và
    // @NoArgsConstructor
}