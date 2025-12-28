package com.example.backend.User;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "student_code", length = 50, unique = true)
    private String studentCode;

    @Column(length = 100, unique = true)
    private String email;

    // Map cột password_hash trong SQL vào biến password trong Java
    @Column(name = "password_hash", length = 255)
    private String password;

    @Column(name = "full_name", columnDefinition = "nvarchar(50)")
    private String fullName;

    @Column(name = "class_name", length = 50)
    private String className;

    @Column(length = 50)
    private String role; // "STUDENT", "ADMIN"

    @Column(name = "avatar_url", length = 50)
    private String avatarUrl;

    @Column(columnDefinition = "nvarchar(50)")
    private String bio;

    private Boolean active;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (active == null)
            active = false;
        if (role == null)
            role = "STUDENT";
    }
}
