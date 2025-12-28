package com.example.backend.User;

import lombok.Data;

@Data
public class RegisterRequest {
    // Các trường bắt buộc nhập từ Form
    private String studentCode;
    private String fullName;
    private String email;
    private String password;

    // Trường tùy chọn (có thể null)
    private String className;

    private String role;


}