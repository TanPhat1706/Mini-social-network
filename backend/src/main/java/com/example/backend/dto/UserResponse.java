package com.example.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
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

    // Bạn có thể dùng thư viện MapStruct để map, hoặc viết hàm constructor thủ công
}