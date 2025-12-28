package com.example.backend.FriendRequest;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "Friendship")
@Data
public class Friendship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FriendshipID")
    private Integer friendshipId;

    @Column(name = "User1ID")
    private Integer user1Id;

    @Column(name = "User2ID")
    private Integer user2Id;

    @Column(name = "Status", length = 20)
    private String status; // PENDING, ACCEPTED, DELETED

    @Column(name = "ActionUserID")
    private Integer actionUserId;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}