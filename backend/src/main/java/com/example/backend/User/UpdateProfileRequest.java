package com.example.backend.User;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private String className;
    private String bio;
    private String avatarUrl; // Người dùng có thể dán link ảnh vào đây
}