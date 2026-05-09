package com.example.backend.Item;

import com.example.backend.Integration.BaseTest; // Nhớ import BaseTest của bạn vào đây
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 🟢 KẾ THỪA TỪ BASE TEST: Đã bao gồm SpringBootTest, MockMvc, DB Test và Transactional
class ItemControllerTest extends BaseTest {

    // Vẫn Mock tầng Service để Controller dễ dàng test các kịch bản ngoại lệ (Exception)
    @MockBean
    private ItemService itemService;

    @Autowired
    private ObjectMapper objectMapper;

    private ItemResponse mockItemResponse;

    @BeforeEach
    void setUp() {
        mockItemResponse = ItemResponse.builder()
                .id(1)
                .name("Viền Vàng")
                .type("AVATAR_FRAME")
                .price(100)
                .active(true)
                .build();
    }

    // ==========================================
    // 1. TEST GET ALL ITEMS
    // ==========================================
    @Test
    void getAllItems_shouldReturn200() throws Exception {
        when(itemService.getAllActiveItems()).thenReturn(List.of(mockItemResponse));

        mockMvc.perform(get("/api/shop/items")
                .header("Authorization", getUserToken()) // 🟢 SỬ DỤNG TOKEN THẬT TỪ BASE TEST
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Viền Vàng"));
    }

    // ==========================================
    // 2. TEST GET ITEMS BY TYPE
    // ==========================================
    @Test
    void getItemsByType_shouldReturn200() throws Exception {
        when(itemService.getItemsByType("AVATAR_FRAME")).thenReturn(List.of(mockItemResponse));

        mockMvc.perform(get("/api/shop/items/type")
                .header("Authorization", getUserToken())
                .param("type", "AVATAR_FRAME")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("AVATAR_FRAME"));
    }

    // ==========================================
    // 3. TEST CREATE ITEM (CẦN QUYỀN ADMIN)
    // ==========================================
    @Test
    void createItem_whenSuccess_shouldReturn200() throws Exception {
        ItemRequest request = new ItemRequest();
        request.setName("Viền Vàng");
        request.setType("AVATAR_FRAME");

        when(itemService.createItem(any(ItemRequest.class))).thenReturn(mockItemResponse);

        mockMvc.perform(post("/api/shop/items")
                .header("Authorization", getAdminToken()) // 🟢 DÙNG TOKEN CỦA ADMIN
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Viền Vàng"));
    }

    @Test
    void createItem_whenServiceThrowsException_shouldReturn400() throws Exception {
        ItemRequest request = new ItemRequest();
        when(itemService.createItem(any())).thenThrow(new RuntimeException("Lỗi tạo vật phẩm"));

        mockMvc.perform(post("/api/shop/items")
                .header("Authorization", getAdminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // 4. TEST BUY ITEM
    // ==========================================
    @Test
    void buyItem_whenSuccess_shouldReturn200() throws Exception {
        // Chú ý: Vì dùng Token thật, username truyền vào hàm service phải khớp với username cấu hình trong BaseTest ("1412")
        when(itemService.buyItem(eq("1412"), eq(10))).thenReturn("Mua thành công");

        mockMvc.perform(post("/api/shop/items/10/buy")
                .header("Authorization", getUserToken())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Mua thành công"));
    }

    @Test
    void buyItem_whenException_shouldReturn400() throws Exception {
        when(itemService.buyItem(eq("1412"), eq(10)))
                .thenThrow(new RuntimeException("Không đủ tiền"));

        mockMvc.perform(post("/api/shop/items/10/buy")
                .header("Authorization", getUserToken())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Không đủ tiền"));
    }

    // ==========================================
    // 5. TEST GET INVENTORY
    // ==========================================
    @Test
    void getMyInventory_whenSuccess_shouldReturn200() throws Exception {
        when(itemService.getUserInventory("1412")).thenReturn(List.of(mockItemResponse));

        mockMvc.perform(get("/api/shop/items/inventory")
                .header("Authorization", getUserToken())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Viền Vàng"));
    }

    @Test
    void getMyInventory_whenException_shouldReturn400() throws Exception {
        when(itemService.getUserInventory("1412")).thenThrow(new RuntimeException("Lỗi tủ đồ"));

        mockMvc.perform(get("/api/shop/items/inventory")
                .header("Authorization", getUserToken())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Lỗi tủ đồ"));
    }

    // ==========================================
    // 6. TEST EQUIP ITEM
    // ==========================================
    @Test
    void equipItem_whenSuccess_shouldReturn200() throws Exception {
        when(itemService.equipFrame("1412", 1)).thenReturn("Đã trang bị");

        mockMvc.perform(put("/api/shop/items/1/equip")
                .header("Authorization", getUserToken())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã trang bị"));
    }

    @Test
    void equipItem_whenException_shouldReturn400() throws Exception {
        when(itemService.equipFrame("1412", 1)).thenThrow(new RuntimeException("Bạn chưa sở hữu vật phẩm này"));

        mockMvc.perform(put("/api/shop/items/1/equip")
                .header("Authorization", getUserToken())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bạn chưa sở hữu vật phẩm này"));
    }

    // ==========================================
    // 7. TEST UNEQUIP ITEM
    // ==========================================
    @Test
    void unequipItem_whenSuccess_shouldReturn200() throws Exception {
        when(itemService.unequipFrame("1412")).thenReturn("Đã tháo trang bị");

        mockMvc.perform(put("/api/shop/items/unequip")
                .header("Authorization", getUserToken())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã tháo trang bị"));
    }

    @Test
    void unequipItem_whenException_shouldReturn400() throws Exception {
        when(itemService.unequipFrame("1412")).thenThrow(new RuntimeException("Lỗi tháo trang bị"));

        mockMvc.perform(put("/api/shop/items/unequip")
                .header("Authorization", getUserToken())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Lỗi tháo trang bị"));
    }
}