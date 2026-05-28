package com.example.backend.Game;

import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import com.example.backend.VPTLpoint.VptlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GameService {

    @Autowired
    private GameScoreRepository gameScoreRepository;
    
    @Autowired
    private UserRepository userRepository;

    // 🟢 1. Inject Service tính điểm VPTL
    @Autowired
    private VptlService vptlService; // <--- THÊM DÒNG NÀY

    public GameScore saveScore(Integer userId, String gameKey, int score) {
        // Tìm User để lấy tên và avatar
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        String displayName = (user.getFullName() != null) ? user.getFullName() : user.getStudentCode();
        
        // Tạo object điểm số
        GameScore newScore = new GameScore(
            gameKey, 
            userId, 
            displayName, 
            user.getAvatarUrl(),
            score
        );
        
        // Lưu điểm vào DB
        GameScore savedScore = gameScoreRepository.save(newScore);

        // 🟢 2. Gọi Service để cộng EXP ngay sau khi lưu điểm
        // Logic: Score 100 -> EXP 10 (đã định nghĩa trong VptlService)
        vptlService.addGameExp(userId, score); // <--- THÊM DÒNG NÀY

        return savedScore;
    }

    public List<GameScore> getLeaderboard(String gameKey) {
        return gameScoreRepository.findTop10ByGameKey(gameKey);
    }
}