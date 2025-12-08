package com.example.backend.model; // Đảm bảo đúng tên package của bạn

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "Users")
@Data // Tự động tạo getters, setters, toString, equals và hashCode (từ Lombok)
@NoArgsConstructor // Thêm constructor không tham số, cần thiết cho JPA/Hibernate
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // Khóa chính, kiểu Long phổ biến hơn Integer cho ID

    @Column(name = "student_code", unique = true, nullable = false, length = 50) // Thêm độ dài
    private String studentCode;

    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100) // Lưu hash, cần độ dài lớn
    private String passwordHash;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "class_name", length = 50)
    private String className;

    // --- Các trường khác từ DB layout ---

    @Column(name = "role", length = 20)
    private String role = "USER"; // Mặc định là USER

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(name = "bio", columnDefinition = "NVARCHAR(MAX)") // Dùng NVARCHAR(MAX) cho text dài trong SQL Server
    private String bio;

    @Column(name = "active")
    private Boolean active = true; // Mặc định là True

    @Column(name = "created_at", updatable = false, columnDefinition = "DATETIME")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "DATETIME")
    private LocalDateTime updatedAt;

    @Column(name = "last_login", columnDefinition = "DATETIME")
    private LocalDateTime lastLogin;

    /**
     * @PrePersist: Hàm được gọi ngay trước khi Entity được lưu (persist) lần đầu.
     *              Dùng để tự động set thời gian tạo.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now(); // Cài đặt lần đầu
        this.active = true;
    }

    /**
     * @PreUpdate: Hàm được gọi ngay trước khi Entity được cập nhật (merge).
     *             Dùng để tự động cập nhật thời gian chỉnh sửa.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}