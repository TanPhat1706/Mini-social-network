package com.example.backend.Item;

import com.example.backend.Enum.CosmeticTheme;
import com.example.backend.Enum.CosmeticType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Integer> {

    List<Item> findByActiveTrueOrderByDisplayOrderAsc();

    List<Item> findByActiveTrueAndTypeOrderByDisplayOrderAsc(CosmeticType type);

    // Lấy các vật phẩm thuộc theme, đang ẩn (dành cho gacha) và user chưa sở hữu
    @Query("SELECT i FROM Item i WHERE i.theme = :theme AND i.active = false AND NOT EXISTS (SELECT 1 FROM UserInventory ui WHERE ui.userId = :userId AND ui.itemId = i.id)")
    List<Item> findUnownedGachaItems(@Param("userId") Integer userId, @Param("theme") CosmeticTheme theme);

    // Thêm hàm này vào dưới hàm findUnownedGachaItems cũ
    long countByThemeAndActiveFalse(CosmeticTheme theme);

}
