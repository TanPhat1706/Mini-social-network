package com.example.backend.Gacha;

import com.example.backend.Enum.CosmeticRarity;
import com.example.backend.Enum.CosmeticTheme;
import com.example.backend.Enum.CosmeticType;
import com.example.backend.Item.Item;
import com.example.backend.Item.ItemRepository;
import com.example.backend.Item.UserInventory;
import com.example.backend.Item.UserInventoryRepository;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GachaServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserInventoryRepository userInventoryRepository;

    @InjectMocks
    private GachaService gachaService;

    private User user;
    private Item commonItem;
    private Item epicItem;
    private Item legendaryItem;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(42);
        user.setVptlPoints(1200);

        commonItem = Item.builder()
                .id(101)
                .code("frame-common")
                .name("Khung Thường")
                .type(CosmeticType.AVATAR_FRAME)
                .theme(CosmeticTheme.FIRE)
                .rarity(CosmeticRarity.COMMON)
                .effectKey("css-frame-common")
                .price(100)
                .active(false)
                .build();

        epicItem = Item.builder()
                .id(102)
                .code("frame-epic")
                .name("Khung Huyền Thoại")
                .type(CosmeticType.AVATAR_FRAME)
                .theme(CosmeticTheme.FIRE)
                .rarity(CosmeticRarity.EPIC)
                .effectKey("css-frame-epic")
                .price(400)
                .active(false)
                .build();

        legendaryItem = Item.builder()
                .id(103)
                .code("frame-legendary")
                .name("Khung Thần Thoại")
                .type(CosmeticType.AVATAR_FRAME)
                .theme(CosmeticTheme.FIRE)
                .rarity(CosmeticRarity.LEGENDARY)
                .effectKey("css-frame-legendary")
                .price(800)
                .active(false)
                .build();
    }

    @Test
    void spin_whenUserNotFound_shouldThrow() {
        when(userRepository.findById(42)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> gachaService.spin(42, CosmeticTheme.FIRE));

        assertEquals("Không tìm thấy User", ex.getMessage());
    }

    @Test
    void spin_whenNotEnoughPoints_shouldThrow() {
        // Fix: Set điểm nhỏ hơn 10 (GACHA_COST) để trigger Exception thiếu tiền
        user.setVptlPoints(5);
        when(userRepository.findById(42)).thenReturn(Optional.of(user));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> gachaService.spin(42, CosmeticTheme.FIRE));

        // Fix: Update expected message khớp với GACHA_COST = 10
        assertEquals("Bạn không đủ 10 VPTL Points để mở rương!", ex.getMessage());
        verify(itemRepository, never()).findUnownedGachaItems(any(), any());
    }

    @Test
    void spin_whenNoUnownedItems_shouldThrow() {
        when(userRepository.findById(42)).thenReturn(Optional.of(user));
        when(itemRepository.findUnownedGachaItems(42, CosmeticTheme.FIRE)).thenReturn(List.of());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> gachaService.spin(42, CosmeticTheme.FIRE));

        assertEquals("Chúc mừng! Bạn đã thu thập toàn bộ vật phẩm của chủ đề này!", ex.getMessage());
    }

    @Test
    void spin_whenSingleRarity_shouldDeductPointsAndSaveInventory() {
        when(userRepository.findById(42)).thenReturn(Optional.of(user));
        when(itemRepository.findUnownedGachaItems(42, CosmeticTheme.FIRE)).thenReturn(List.of(epicItem));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userInventoryRepository.save(any(UserInventory.class))).thenAnswer(inv -> inv.getArgument(0));

        GachaResponse response = gachaService.spin(42, CosmeticTheme.FIRE);

        assertNotNull(response);
        assertEquals("Khung Huyền Thoại", response.getWonItem().getName());
        // Fix: 1200 - 10 = 1190
        assertEquals(1190, response.getRemainingPoints());
        assertTrue(response.getMessage().contains("Khung Huyền Thoại"));

        // Fix: Expect 1190 thay vì 700
        assertEquals(1190, user.getVptlPoints());
        verify(userRepository).save(user);

        ArgumentCaptor<UserInventory> inventoryCaptor = ArgumentCaptor.forClass(UserInventory.class);
        verify(userInventoryRepository).save(inventoryCaptor.capture());
        assertEquals(42, inventoryCaptor.getValue().getUserId());
        assertEquals(102, inventoryCaptor.getValue().getItemId());
    }

    @Test
    void getGachaInfo_shouldReturnCorrectCountsAndRates() {
        when(itemRepository.countByThemeAndActiveFalse(CosmeticTheme.FIRE)).thenReturn(5L);
        when(itemRepository.findUnownedGachaItems(42, CosmeticTheme.FIRE))
                .thenReturn(List.of(commonItem, epicItem));

        GachaInfoResponse result = gachaService.getGachaInfo(42, CosmeticTheme.FIRE);

        assertEquals("FIRE", result.getTheme());
        assertEquals(5, result.getTotalPoolSize());
        assertEquals(3, result.getOwnedCount());
        assertEquals(2, result.getRemainingForGuarantee());
        assertNotNull(result.getCurrentRates());
        assertEquals(2, result.getCurrentRates().size());
        assertEquals(85.71, result.getCurrentRates().get("COMMON"));
        assertEquals(14.29, result.getCurrentRates().get("EPIC"));
    }

    @Test
    void getGachaHistory_shouldMapHistoryItemsCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        List<Object[]> rawHistory = List.<Object[]>of(new Object[]{now, legendaryItem});
        when(userInventoryRepository.findHistoryWithItem(42, CosmeticTheme.FIRE))
                .thenReturn(rawHistory);

        List<GachaHistoryItemDTO> history = gachaService.getGachaHistory(42, CosmeticTheme.FIRE);

        assertEquals(1, history.size());
        GachaHistoryItemDTO entry = history.get(0);
        assertEquals(103, entry.getItemId());
        assertEquals("Khung Thần Thoại", entry.getName());
        assertEquals("AVATAR_FRAME", entry.getType());
        assertEquals("LEGENDARY", entry.getRarity());
        assertEquals("css-frame-legendary", entry.getEffectKey());
        assertEquals(now, entry.getPurchasedAt());
    }
}