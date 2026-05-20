package com.example.backend.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface UserDailyStatRepository extends JpaRepository<UserDailyStat, Long> {
    
    // Tìm bản ghi của User cụ thể vào một Ngày cụ thể
    // Ví dụ: Tìm stat của user ID 1 vào ngày 2024-02-04
    Optional<UserDailyStat> findByUserIdAndDate(Integer userId, LocalDate date);
}
