package com.example.backend.Game;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_scores")
public class GameScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ID của bảng điểm vẫn để Long cho thoải mái

    @Column(nullable = false)
    private String gameKey;

    @Column(nullable = false)
    private Integer userId; // <--- ĐÃ SỬA: Khớp với Integer id của User

    private String username;
    
    // Thêm cái này để hiển thị avatar trên Bảng Xếp Hạng cho đẹp
    private String avatarUrl; 

    @Column(nullable = false)
    private int score;

    private LocalDateTime playedAt;

    public GameScore() {}

    public GameScore(String gameKey, Integer userId, String username, String avatarUrl, int score) {
        this.gameKey = gameKey;
        this.userId = userId;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.score = score;
        this.playedAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() { return id; }
    public String getGameKey() { return gameKey; }
    public void setGameKey(String gameKey) { this.gameKey = gameKey; }
    public Integer getUserId() { return userId; } // <--- ĐÃ SỬA
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public LocalDateTime getPlayedAt() { return playedAt; }
    public void setPlayedAt(LocalDateTime playedAt) { this.playedAt = playedAt; }
}