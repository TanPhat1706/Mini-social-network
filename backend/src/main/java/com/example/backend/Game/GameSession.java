package com.example.backend.Game;

import com.example.backend.Enum.GameSessionStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "game_sessions")
@Data
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "host_id", nullable = false)
    private Integer hostId;

    @Column(name = "guest_id")
    private Integer guestId;

    @Column(nullable = false, length = 20)
    private String board;

    @Column(name = "current_turn")
    private Integer currentTurn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameSessionStatus status;

    @Column(name = "winner_id")
    private Integer winnerId;

    // THÊM TRƯỜNG NÀY (Không cho phép update sau khi đã tạo)
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // TỰ ĐỘNG GÁN THỜI GIAN KHI TẠO MỚI (INSERT)
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // TỰ ĐỘNG GÁN THỜI GIAN KHI CẬP NHẬT (UPDATE)
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}