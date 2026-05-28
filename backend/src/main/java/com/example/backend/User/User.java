package com.example.backend.User;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {

    // ... (Giữ nguyên các trường cũ: id, studentCode, email, password...)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "student_code", length = 50, unique = true)
    private String studentCode;

    @Column(length = 100, unique = true)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String password;

    @Column(name = "full_name", columnDefinition = "nvarchar(50)")
    private String fullName;

    @Column(name = "class_name", length = 50)
    private String className;

    @Column(length = 50)
    private String role;

    // --- CẬP NHẬT: ẢNH ĐẠI DIỆN ---
    @Column(name = "avatar_url", length = 255) // Tăng độ dài để chứa đường dẫn file
    private String avatarUrl;

    // --- MỚI: THÊM ẢNH BÌA (BACKGROUND) ---
    @Column(name = "cover_photo_url", length = 255)
    private String coverPhotoUrl;

    @Column(columnDefinition = "nvarchar(500)") // Tăng độ dài Bio cho thoải mái
    private String bio;

    private Boolean active;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "last_password_reset_at")
    private LocalDateTime lastPasswordResetAt;

    @Column(name = "level", nullable = false)
    private Integer level = 1; // Mặc định level 1

    @Column(name = "exp", nullable = false)
    private Integer exp = 0; // Mặc định 0 exp

    // 🟢 MỚI: TÁCH RIÊNG ĐIỂM GIAO DỊCH VÀ KHUNG AVATAR
    @Column(name = "vptl_points", nullable = false)
    private Integer vptlPoints = 0; // Số dư tài khoản điểm của user

    @Column(name = "current_avatar_frame", length = 255)
    private String currentAvatarFrame; // Đường dẫn hoặc ID của viền avatar đang dùng

    @Column(name = "current_name_color")
    private String currentNameColor;

    public Integer getId() {
        return this.id;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (active == null) {
            active = false;
        }
        if (role == null) {
            role = "STUDENT";
        }
    }
}
