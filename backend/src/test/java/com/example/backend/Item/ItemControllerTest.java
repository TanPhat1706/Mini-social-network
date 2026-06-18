package com.example.backend.Item;

import com.example.backend.Integration.BaseControllerTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        value = ItemController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
class ItemControllerTest extends BaseControllerTest {

    @MockBean
    private ItemService itemService;

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

    @Test
    @WithMockUser(username = "1412")
    void getAllItems_shouldReturn200() throws Exception {
        when(itemService.getAllActiveItems()).thenReturn(List.of(mockItemResponse));

        mockMvc.perform(get("/api/shop/items")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Viền Vàng"));
    }

    @Test
    @WithMockUser(username = "1412")
    void getItemsByType_shouldReturn200() throws Exception {
        when(itemService.getItemsByType("AVATAR_FRAME")).thenReturn(List.of(mockItemResponse));

        mockMvc.perform(get("/api/shop/items/type")
                        .param("type", "AVATAR_FRAME")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("AVATAR_FRAME"));
    }

    @Test
    @WithMockUser(username = "141204", roles = "ADMIN")
    void createItem_whenSuccess_shouldReturn200() throws Exception {
        ItemRequest request = new ItemRequest();
        request.setName("Viền Vàng");
        request.setType("AVATAR_FRAME");

        when(itemService.createItem(any(ItemRequest.class))).thenReturn(mockItemResponse);

        mockMvc.perform(post("/api/shop/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Viền Vàng"));
    }

    @Test
    @WithMockUser(username = "141204", roles = "ADMIN")
    void createItem_whenServiceThrowsException_shouldReturn400() throws Exception {
        ItemRequest request = new ItemRequest();
        when(itemService.createItem(any())).thenThrow(new RuntimeException("Lỗi tạo vật phẩm"));

        mockMvc.perform(post("/api/shop/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1412")
    void buyItem_whenSuccess_shouldReturn200() throws Exception {
        when(itemService.buyItem(eq("1412"), eq(10))).thenReturn("Mua thành công");

        mockMvc.perform(post("/api/shop/items/10/buy")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Mua thành công"));
    }

    @Test
    @WithMockUser(username = "1412")
    void buyItem_whenException_shouldReturn400() throws Exception {
        when(itemService.buyItem(eq("1412"), eq(10)))
                .thenThrow(new RuntimeException("Không đủ tiền"));

        mockMvc.perform(post("/api/shop/items/10/buy")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Không đủ tiền"));
    }

    @Test
    @WithMockUser(username = "1412")
    void getMyInventory_whenSuccess_shouldReturn200() throws Exception {
        when(itemService.getUserInventory("1412")).thenReturn(List.of(mockItemResponse));

        mockMvc.perform(get("/api/shop/items/inventory")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Viền Vàng"));
    }

    @Test
    @WithMockUser(username = "1412")
    void getMyInventory_whenException_shouldReturn400() throws Exception {
        when(itemService.getUserInventory("1412")).thenThrow(new RuntimeException("Lỗi tủ đồ"));

        mockMvc.perform(get("/api/shop/items/inventory")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Lỗi tủ đồ"));
    }

    @Test
    @WithMockUser(username = "1412")
    void equipItem_whenSuccess_shouldReturn200() throws Exception {
        when(itemService.equipFrame("1412", 1)).thenReturn("Đã trang bị");

        mockMvc.perform(put("/api/shop/items/1/equip")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã trang bị"));
    }

    @Test
    @WithMockUser(username = "1412")
    void equipItem_whenException_shouldReturn400() throws Exception {
        when(itemService.equipFrame("1412", 1)).thenThrow(new RuntimeException("Bạn chưa sở hữu vật phẩm này"));

        mockMvc.perform(put("/api/shop/items/1/equip")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bạn chưa sở hữu vật phẩm này"));
    }

    @Test
    @WithMockUser(username = "1412")
    void unequipItem_whenSuccess_shouldReturn200() throws Exception {
        when(itemService.unequipFrame("1412")).thenReturn("Đã tháo trang bị");

        mockMvc.perform(put("/api/shop/items/unequip")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã tháo trang bị"));
    }

    @Test
    @WithMockUser(username = "1412")
    void unequipItem_whenException_shouldReturn400() throws Exception {
        when(itemService.unequipFrame("1412")).thenThrow(new RuntimeException("Lỗi tháo trang bị"));

        mockMvc.perform(put("/api/shop/items/unequip")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Lỗi tháo trang bị"));
    }
}
