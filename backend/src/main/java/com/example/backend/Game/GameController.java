package com.example.backend.Game;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.example.backend.User.User;
import com.example.backend.User.UserRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/games")
public class GameController {

    @Autowired
    private GameService gameService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/leaderboard/{gameKey}")
    public ResponseEntity<?> getLeaderboard(@PathVariable String gameKey) {
        List<GameScore> topScores = gameService.getLeaderboard(gameKey);
        return ResponseEntity.ok(topScores);
    }

    @PostMapping("/score")
    public ResponseEntity<?> submitScore(@RequestBody ScoreRequest request) {
        try {
            // JWT subject = studentCode (String), KHÔNG phải numeric userId
            String studentCode = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByStudentCode(studentCode)
                    .orElseThrow(() -> new RuntimeException("User not found: " + studentCode));

            GameScore savedScore = gameService.saveScore(user.getId(), request.getGameKey(), request.getScore());
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