package com.example.backend.Chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatConversationDTO {
    private Integer partnerId;
    private String partnerName;
    private String partnerAvatar;
    private String lastMessage;
    private LocalDateTime timestamp;

    @JsonProperty("isRead")
    private boolean isRead;
}