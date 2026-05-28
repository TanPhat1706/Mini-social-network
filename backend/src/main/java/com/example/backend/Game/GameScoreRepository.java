package com.example.backend.Game;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameScoreRepository extends JpaRepository<GameScore, Long> {

    // Lấy Top 10 điểm cao nhất của game (ví dụ: SNAKE) giảm dần theo điểm
    @Query("SELECT s FROM GameScore s WHERE s.gameKey = :gameKey ORDER BY s.score DESC LIMIT 10")
    List<GameScore> findTop10ByGameKey(String gameKey);
    
    // Tìm điểm cao nhất của user này trong game này (để check kỷ lục)
    GameScore findTopByUserIdAndGameKeyOrderByScoreDesc(Long userId, String gameKey);
}
