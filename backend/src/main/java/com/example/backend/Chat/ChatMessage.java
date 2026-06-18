package com.example.backend.Chat;

import com.example.backend.Enum.MessageType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ChatMessage")
@Getter @Setter // 🟢 Dùng Getter Setter thay cho Data để an toàn với Lazy Load
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer senderId;
    private Integer receiverId;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private MessageType messageType = MessageType.TEXT;

    private Long gameSessionId;

    private LocalDateTime timestamp;

    @Column(name = "is_read")
    private boolean isRead = false;

    // --- CÁC TRƯỜNG THÊM MỚI CHO TÍNH NĂNG THU HỒI ---
    @Column(name = "is_deleted_everyone")
    private Boolean isDeletedEveryone = false;

    @Column(name = "deleted_by_sender_id")
    private Integer deletedBySenderId; // Lưu ID của user ấn "Thu hồi ở phía tôi"

    // --- CÁC TRƯỜNG THÊM MỚI CHO TÍNH NĂNG REACTION ---
    @OneToMany(mappedBy = "chatMessage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ChatReaction> reactions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
        if (messageType == null) {
            messageType = MessageType.TEXT;
        }
    }
}