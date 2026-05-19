package com.example.backend.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional; // 🟢 THÊM IMPORT NÀY
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface SecurityHistoryRepository extends JpaRepository<SecurityHistory, Integer> {
    Page<SecurityHistory> findByUserIdOrderByLoginTimeDesc(Integer userId, Pageable pageable);

    // 🟢 MỚI: Tìm lịch sử dựa trên Session ID
    Optional<SecurityHistory> findBySessionId(String sessionId);
}