package com.example.backend.payload.response;

import lombok.Data;

@Data
public class JwtResponse {
    private String accessToken;
    private String refreshToken;
    private String type = "Bearer";

    private Integer id;
    private String studentCode;
    private String email;
    private String fullName;
    private String role;

    public JwtResponse(String accessToken, String refreshToken, Integer id, String studentCode, String email,
            String fullName, String role) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.id = id;
        this.studentCode = studentCode;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }
}