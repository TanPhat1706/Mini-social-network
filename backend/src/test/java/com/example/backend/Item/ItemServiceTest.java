package com.example.backend.Item;

import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserInventoryRepository inventoryRepository;

    @InjectMocks
    private ItemService itemService;

    private User currentUser;
    private Item mockItem;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1);
        currentUser.setStudentCode("SV001");
        currentUser.setVptlPoints(1000); // Mặc định user có 1000 điểm

        mockItem = new Item();
        mockItem.setId(10);
        mockItem.setName("Viền Rồng Lửa");
        mockItem.setType("AVATAR_FRAME");
        mockItem.setImageUrl("dragon.png");
        mockItem.setPrice(500);
        mockItem.setActive(true);
    }

    // ==========================================
    // 1. TEST CÁC HÀM GET & CREATE
    // ==========================================

    @Test
    void getAllActiveItems_shouldReturnMappedList() {
        when(itemRepository.findByActiveTrue()).thenReturn(List.of(mockItem));
        List<ItemResponse> res = itemService.getAllActiveItems();
        assertEquals(1, res.size());
        assertEquals("Viền Rồng Lửa", res.get(0).getName());
    }

    @Test
    void getItemsByType_shouldReturnMappedList() {
        when(itemRepository.findByActiveTrueAndType("AVATAR_FRAME")).thenReturn(List.of(mockItem));
        List<ItemResponse> res = itemService.getItemsByType("AVATAR_FRAME");
        assertEquals(1, res.size());
    }

    @Test
    void createItem_shouldSaveAndReturnResponse() {
        ItemRequest req = new ItemRequest();
        req.setName("Màu Tên VIP");
        req.setType("NAME_COLOR");
        req.setPrice(300);

        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));

        ItemResponse res = itemService.createItem(req);

        assertEquals("Màu Tên VIP", res.getName());
        assertEquals("NAME_COLOR", res.getType());
        assertEquals(300, res.getPrice());
    }

    // ==========================================
    // 2. TEST HÀM MUA VẬT PHẨM (buyItem)
    // ==========================================

    @Test
    void buyItem_whenUserNotFound_shouldThrow() {
        when(userRepository.findByStudentCode("GHOST")).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> itemService.buyItem("GHOST", 10));
        assertEquals("Không tìm thấy người dùng.", ex.getMessage());
    }

    @Test
    void buyItem_whenItemNotFound_shouldThrow() {
        when(userRepository.findByStudentCode("SV001")).thenReturn(Optional.of(currentUser));
        when(itemRepository.findById(99)).thenReturn(Optional.empty());
        
        RuntimeException ex = assertThrows(RuntimeException.class, () -> itemService.buyItem("SV001", 99));
        assertEquals("Vật phẩm không tồn tại trên hệ thống.", ex.getMessage());
    }

    @Test
    void buyItem_whenItemInactive_shouldThrow() {
        mockItem.setActive(false); // Ngừng bán
        when(userRepository.findByStudentCode("SV001")).thenReturn(Optional.of(currentUser));
        when(itemRepository.findById(10)).thenReturn(Optional.of(mockItem));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> itemService.buyItem("SV001", 10));
        assertEquals("Vật phẩm này đã ngừng bán.", ex.getMessage());
    }

    @Test
    void buyItem_whenAlreadyOwned_shouldThrow() {
        when(userRepository.findByStudentCode("SV001")).thenReturn(Optional.of(currentUser));
        when(itemRepository.findById(10)).thenReturn(Optional.of(mockItem));
        when(inventoryRepository.existsByUserIdAndItemId(1, 10)).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> itemService.buyItem("SV001", 10));
        assertEquals("Bạn đã sở hữu vật phẩm này rồi, không cần mua lại đâu!", ex.getMessage());
    }

    @Test
    void buyItem_whenNotEnoughPoints_shouldThrow() {
        currentUser.setVptlPoints(100); // Chỉ có 100 điểm
        mockItem.setPrice(500); // Giá 500 điểm

        when(userRepository.findByStudentCode("SV001")).thenReturn(Optional.of(currentUser));
        when(itemRepository.findById(10)).thenReturn(Optional.of(mockItem));
        when(inventoryRepository.existsByUserIdAndItemId(1, 10)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> itemService.buyItem("SV001", 10));
        assertEquals("Số dư VPTL Points không đủ. Cày thêm game đi bạn ơi!", ex.getMessage());
    }

    @Test
    void buyItem_whenSuccess_shouldDeductPoints_andSaveInventory() {
        currentUser.setVptlPoints(1000); // Có 1000 điểm
        mockItem.setPrice(300); // Giá 300 điểm

        when(userRepository.findByStudentCode("SV001")).thenReturn(Optional.of(currentUser));
        when(itemRepository.findById(10)).thenReturn(Optional.of(mockItem));
        when(inventoryRepository.existsByUserIdAndItemId(1, 10)).thenReturn(false);

        String msg = itemService.buyItem("SV001", 10);

        assertEquals("Chúc mừng! Bạn đã mua thành công: Viền Rồng Lửa", msg);
        assertEquals(700, currentUser.getVptlPoints()); // Trừ đi 300 điểm
        
        verify(userRepository).save(currentUser);
        
        ArgumentCaptor<UserInventory> invCaptor = ArgumentCaptor.forClass(UserInventory.class);
        verify(inventoryRepository).save(invCaptor.capture());
        assertEquals(1, invCaptor.getValue().getUserId());
        assertEquals(10, invCaptor.getValue().getItemId());
    }

    // ==========================================
    // 3. TEST INVENTORY VÀ EQUIP / UNEQUIP
    // ==========================================

    @Test
    void getUserInventory_shouldReturnOwnedItems() {
        when(userRepository.findByStudentCode("SV001")).thenReturn(Optional.of(currentUser));
        
        UserInventory inv1 = new UserInventory(); inv1.setItemId(10);
        when(inventoryRepository.findByUserId(1)).thenReturn(List.of(inv1));
        when(itemRepository.findAllById(List.of(10))).thenReturn(List.of(mockItem));

        List<ItemResponse> res = itemService.getUserInventory("SV001");
        assertEquals(1, res.size());
        assertEquals("Viền Rồng Lửa", res.get(0).getName());
    }

    @Test
    void equipFrame_whenNotOwned_shouldThrow() {
        when(userRepository.findByStudentCode("SV001")).thenReturn(Optional.of(currentUser));
        when(inventoryRepository.existsByUserIdAndItemId(1, 10)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> itemService.equipFrame("SV001", 10));
        assertEquals("Bạn chưa sở hữu vật phẩm này! Vui lòng mua trước khi trang bị.", ex.getMessage());
    }

    @Test
    void equipFrame_whenAvatarFrame_shouldUpdateUser() {
        when(userRepository.findByStudentCode("SV001")).thenReturn(Optional.of(currentUser));
        when(inventoryRepository.existsByUserIdAndItemId(1, 10)).thenReturn(true);
        when(itemRepository.findById(10)).thenReturn(Optional.of(mockItem)); // Type = AVATAR_FRAME

        itemService.equipFrame("SV001", 10);

        assertEquals("dragon.png", currentUser.getCurrentAvatarFrame());
        verify(userRepository).save(currentUser);
    }

    @Test
    void equipFrame_whenNameColor_shouldUpdateUser() {
        mockItem.setType("NAME_COLOR");
        mockItem.setImageUrl("#FF0000");

        when(userRepository.findByStudentCode("SV001")).thenReturn(Optional.of(currentUser));
        when(inventoryRepository.existsByUserIdAndItemId(1, 10)).thenReturn(true);
        when(itemRepository.findById(10)).thenReturn(Optional.of(mockItem));

        itemService.equipFrame("SV001", 10);

        assertEquals("#FF0000", currentUser.getCurrentNameColor());
        verify(userRepository).save(currentUser);
    }

    @Test
    void equipFrame_whenUnsupportedType_shouldThrow() {
        mockItem.setType("INVALID_TYPE");
        when(userRepository.findByStudentCode("SV001")).thenReturn(Optional.of(currentUser));
        when(inventoryRepository.existsByUserIdAndItemId(1, 10)).thenReturn(true);
        when(itemRepository.findById(10)).thenReturn(Optional.of(mockItem));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> itemService.equipFrame("SV001", 10));
        assertEquals("Loại vật phẩm này không hỗ trợ trang bị!", ex.getMessage());
    }

    @Test
    void unequipFrame_shouldSetToNull() {
        currentUser.setCurrentAvatarFrame("old_frame.png");
        currentUser.setCurrentNameColor("#000000");
        when(userRepository.findByStudentCode("SV001")).thenReturn(Optional.of(currentUser));

        String msg = itemService.unequipFrame("SV001");

        assertEquals("Đã tháo trang bị (Viền & Màu tên). Trở về phong cách mộc mạc!", msg);
        assertNull(currentUser.getCurrentAvatarFrame());
        assertNull(currentUser.getCurrentNameColor());
        verify(userRepository).save(currentUser);
    }
}