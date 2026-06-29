package com.example.backend.Item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserInventoryRepository extends JpaRepository<UserInventory, Integer> {
    // Hàm này cực kỳ quan trọng: Dùng để check xem User đã sở hữu Item này chưa!
    boolean existsByUserIdAndItemId(Integer userId, Integer itemId);

    List<UserInventory> findByUserId(Integer userId);

    // Thêm hàm này vào UserInventoryRepository
    @Query("SELECT ui.purchasedAt, i FROM UserInventory ui, Item i " +
           "WHERE ui.itemId = i.id AND ui.userId = :userId AND i.theme = :theme " +
           "ORDER BY ui.purchasedAt DESC")
    List<Object[]> findHistoryWithItem(@Param("userId") Integer userId, @Param("theme") com.example.backend.Enum.CosmeticTheme theme);
}