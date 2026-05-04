package com.example.backend.Game;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/games")
public class GameController {

    @Autowired
    private GameService gameService;

    @GetMapping("/leaderboard/{gameKey}")
    public ResponseEntity<?> getLeaderboard(@PathVariable String gameKey) {
        List<GameScore> topScores = gameService.getLeaderboard(gameKey);
        return ResponseEntity.ok(topScores);
    }

    @PostMapping("/score")
    public ResponseEntity<?> submitScore(@RequestBody ScoreRequest request) {
        try {
            // Ép kiểu chuỗi thành số nguyên
            String tokenName = SecurityContextHolder.getContext().getAuthentication().getName();
            Integer userId = Integer.parseInt(tokenName);

            // Truyền biến userId (Integer) vào service
            GameScore savedScore = gameService.saveScore(userId, request.getGameKey(), request.getScore());
            return ResponseEntity.ok(Map.of("message", "Score saved!", "data", savedScore));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

// 🟢 DTO đã được tối giản: Xóa bỏ userId vì không được phép tin tưởng ID do
// Client gửi lên
class ScoreRequest {
    private String gameKey;
    private int score;

    public String getGameKey() {
        return gameKey;
    }

    public void setGameKey(String gameKey) {
        this.gameKey = gameKey;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}