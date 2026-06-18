package com.example.backend.Chat;

import lombok.Data;

@Data
public class RevokeRequest {
    private Long messageId;
    private Integer requesterId; // Người yêu cầu thu hồi
    private String revokeType; // "EVERYONE" hoặc "SELF"
}