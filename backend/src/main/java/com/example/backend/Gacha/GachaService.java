package com.example.backend.Gacha;

import com.example.backend.Enum.CosmeticTheme;
import com.example.backend.Item.Item;
import com.example.backend.Item.ItemRepository;
import com.example.backend.Item.UserInventory;
import com.example.backend.Item.UserInventoryRepository;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GachaService {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final UserInventoryRepository userInventoryRepository;

    private static final int GACHA_COST = 10; // Giá mỗi lượt quay

    // Tỉ lệ % gốc cho từng độ hiếm
    private static final Map<String, Double> BASE_WEIGHTS = Map.of(
            "MYTHIC", 0.1,
            "LEGENDARY", 4.9,
            "EPIC", 10.0,
            "RARE", 25.0,
            "COMMON", 60.0
    );

    // =========================================================================
    // API 1: QUAY RƯƠNG MAY MẮN
    // =========================================================================
    @Transactional
    public GachaResponse spin(Integer userId, CosmeticTheme theme) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User"));

        // 1. Kiểm tra số dư điểm
        int currentPoints = (user.getVptlPoints() != null) ? user.getVptlPoints() : 0;
        if (currentPoints < GACHA_COST) {
            throw new RuntimeException("Bạn không đủ " + GACHA_COST + " VPTL Points để mở rương!");
        }

        // 2. Lọc danh sách vật phẩm user chưa có trong chủ đề này
        List<Item> unownedItems = itemRepository.findUnownedGachaItems(userId, theme);
        if (unownedItems.isEmpty()) {
            throw new RuntimeException("Chúc mừng! Bạn đã thu thập toàn bộ vật phẩm của chủ đề này!");
        }

        // 3. Phân nhóm vật phẩm theo độ hiếm
        Map<String, List<Item>> itemsByRarity = unownedItems.stream()
                .collect(Collectors.groupingBy(item -> item.getRarity().name()));

        // 4. Tính tổng trọng số động (Dùng Stream để biến luôn thành effectively final)
        final double totalDynamicWeight = itemsByRarity.keySet().stream()
                .mapToDouble(rarity -> BASE_WEIGHTS.getOrDefault(rarity, 0.0))
                .sum();

        // 5. Quay số ngẫu nhiên từ 0 đến tổng trọng số
        double roll = Math.random() * totalDynamicWeight;
        double currentWeight = 0.0;
        String wonRarity = null;

        for (Map.Entry<String, List<Item>> entry : itemsByRarity.entrySet()) {
            currentWeight += BASE_WEIGHTS.getOrDefault(entry.getKey(), 0.0);
            if (roll <= currentWeight) {
                wonRarity = entry.getKey();
                break;
            }
        }

        // Fallback an toàn
        if (wonRarity == null) {
            wonRarity = itemsByRarity.keySet().iterator().next();
        }

        // 6. Chọn ngẫu nhiên 1 vật phẩm trong nhóm độ hiếm vừa quay trúng
        List<Item> candidateItems = itemsByRarity.get(wonRarity);
        Item wonItem = candidateItems.get(new Random().nextInt(candidateItems.size()));

        // 7. Trừ tiền và lưu vật phẩm vào kho đồ (Dùng đúng ID do thiết kế bảng không dùng @ManyToOne)
        user.setVptlPoints(currentPoints - GACHA_COST);
        userRepository.save(user);

        UserInventory inventory = new UserInventory();
        inventory.setUserId(user.getId());
        inventory.setItemId(wonItem.getId());
        // purchasedAt sẽ được tự động gắn bởi @PrePersist trong Entity
        userInventoryRepository.save(inventory);

        // 8. Trả về kết quả
        return GachaResponse.builder()
                .message("Chúc mừng bạn đã quay trúng " + wonItem.getName() + "!")
                .wonItem(wonItem)
                .remainingPoints(user.getVptlPoints())
                .build();
    }

    // =========================================================================
    // API 2: LẤY THÔNG TIN PITY VÀ TỶ LỆ RƯƠNG
    // =========================================================================
    public GachaInfoResponse getGachaInfo(Integer userId, CosmeticTheme theme) {
        long totalPoolSize = itemRepository.countByThemeAndActiveFalse(theme);

        List<Item> unownedItems = itemRepository.findUnownedGachaItems(userId, theme);
        int remainingForGuarantee = unownedItems.size();
        int ownedCount = (int) totalPoolSize - remainingForGuarantee;

        Map<String, List<Item>> itemsByRarity = unownedItems.stream()
                .collect(Collectors.groupingBy(item -> item.getRarity().name()));

        final double totalDynamicWeight = itemsByRarity.keySet().stream()
                .mapToDouble(rarity -> BASE_WEIGHTS.getOrDefault(rarity, 0.0))
                .sum();

        Map<String, Double> currentRates = itemsByRarity.keySet().stream()
                .collect(Collectors.toMap(
                        rarity -> rarity,
                        rarity -> {
                            double weight = BASE_WEIGHTS.getOrDefault(rarity, 0.0);
                            return Math.round((weight / totalDynamicWeight) * 10000.0) / 100.0;
                        }
                ));

        return GachaInfoResponse.builder()
                .theme(theme.name())
                .totalPoolSize((int) totalPoolSize)
                .ownedCount(ownedCount)
                .remainingForGuarantee(remainingForGuarantee)
                .currentRates(currentRates)
                .build();
    }

    // =========================================================================
    // API 3: LẤY LỊCH SỬ QUAY RƯƠNG (GIẢM DẦN THEO THỜI GIAN)
    // =========================================================================
    public List<GachaHistoryItemDTO> getGachaHistory(Integer userId, CosmeticTheme theme) {
        // Hàm findHistoryWithItem trả về Object[] vì ta dùng câu lệnh JOIN trong Repository
        List<Object[]> rawHistory = userInventoryRepository.findHistoryWithItem(userId, theme);

        return rawHistory.stream().map(record -> {
            LocalDateTime purchasedAt = (LocalDateTime) record[0];
            Item item = (Item) record[1];

            return GachaHistoryItemDTO.builder()
                    .itemId(item.getId())
                    .name(item.getName())
                    .type(item.getType().name())
                    .rarity(item.getRarity().name())
                    .effectKey(item.getEffectKey())
                    .purchasedAt(purchasedAt)
                    .build();
        }).collect(Collectors.toList());
    }
}