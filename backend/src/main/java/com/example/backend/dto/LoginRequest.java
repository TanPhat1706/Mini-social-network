package com.example.backend.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String identifier; // Người dùng nhập Email hoặc Student Code vào đây
    private String password;
}