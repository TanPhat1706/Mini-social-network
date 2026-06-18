package com.example.backend.Item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserInventoryRepository extends JpaRepository<UserInventory, Integer> {
    // Hàm này cực kỳ quan trọng: Dùng để check xem User đã sở hữu Item này chưa!
    boolean existsByUserIdAndItemId(Integer userId, Integer itemId);

    List<UserInventory> findByUserId(Integer userId);
}