package com.example.backend.Notification;

import jakarta.persistence.*;
import lombok.*;
import com.example.backend.BaseEntity.BaseEntity;
import com.example.backend.Enum.NotificationType;
import com.example.backend.User.User;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_receiver", columnList = "receiver_id"),
    @Index(name = "idx_notification_created_at", columnList = "created_at")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Notification extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private NotificationType type;

    @Column(length = 50)
    private String entityType;

    private Long entityId;

    @Column(columnDefinition = "NVARCHAR(500)") 
    private String metadata; 

    private boolean isRead = false;
}
