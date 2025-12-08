package com.example.backend.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    private String studentCode; // Hoặc dùng email

    @NotBlank
    private String password;
}