package com.example.backend.User;

import lombok.AllArgsConstructor; // Thêm
import lombok.Builder;            // Thêm
import lombok.Data;
import lombok.NoArgsConstructor;  // Thêm
import java.time.LocalDateTime;

@Data
@Builder           // ⭐️ Giúp tạo object kiểu UserResponse.builder()...
@NoArgsConstructor // ⭐️ Cần thiết cho Jackson (JSON parsing)
@AllArgsConstructor // ⭐️ Cần thiết cho Builder
public class UserResponse {
    private Integer id;
    private String studentCode;
    private String email;
    private String fullName;
    private String className;
    private String role;
    private String avatarUrl;
    private String bio;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
}
