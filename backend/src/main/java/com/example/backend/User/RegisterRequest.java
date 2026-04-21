package com.example.backend.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Student code is required")
    private String studentCode;

    @NotBlank(message = "Full name is required")
    @Pattern(regexp = "^[a-zA-Z0-9 \\p{L}]+$", message = "Tên không hợp lệ, không được chứa emoji hoặc ký tự đặc biệt")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email is not valid")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private String className;
    private String role;
}