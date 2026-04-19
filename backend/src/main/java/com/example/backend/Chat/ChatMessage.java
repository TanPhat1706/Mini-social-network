package com.example.backend.Chat;

import com.example.backend.Enum.MessageType;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "ChatMessage")
@Data
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer senderId;
    private Integer receiverId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private MessageType messageType = MessageType.TEXT;

    // Dùng cho message type GAME_INVITE để link đến room sau khi accept.
    private Long gameSessionId;

    private LocalDateTime timestamp;

    @Column(name = "is_read")
    private boolean isRead = false;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
        if (messageType == null) {
            messageType = MessageType.TEXT;
        }
    }
}