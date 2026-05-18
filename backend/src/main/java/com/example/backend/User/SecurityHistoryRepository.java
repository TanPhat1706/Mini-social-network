package com.example.backend.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional; // 🟢 THÊM IMPORT NÀY

@Repository
public interface SecurityHistoryRepository extends JpaRepository<SecurityHistory, Integer> {
    List<SecurityHistory> findByUserIdOrderByLoginTimeDesc(Integer userId);
    
    // 🟢 MỚI: Tìm lịch sử dựa trên Session ID
    Optional<SecurityHistory> findBySessionId(String sessionId);
}