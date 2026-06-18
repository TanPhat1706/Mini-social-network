package com.example.backend.Message;

import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import com.example.backend.Conversation.Conversation;
import com.example.backend.Enum.MessageType;
import com.example.backend.User.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "messages")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @Column(nullable = false)
    private boolean isDeleted = false;

    private boolean isEdited = false;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private MessageType messageType = MessageType.TEXT;

    @CreationTimestamp
    private LocalDateTime createdAt; 
}
