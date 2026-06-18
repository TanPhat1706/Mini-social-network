package com.example.backend.Notification;

import java.time.LocalDateTime;
import com.example.backend.Enum.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NotificationDTO {
    private Long id;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private String message; 
    private String targetUrl; 
    private LocalDateTime createdAt;
    private boolean isRead;
    private NotificationType type;
    // 🟢 MỚI: BỔ SUNG 2 TRƯỜNG NÀY CHO MA THUẬT GIAO DIỆN
    private String senderAvatarFrame;
    private String senderNameColor;
}
