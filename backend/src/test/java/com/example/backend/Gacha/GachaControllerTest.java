package com.example.backend.Gacha;

import com.example.backend.Integration.BaseControllerTest;
import com.example.backend.Item.Item;
import com.example.backend.Item.Item;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = GachaController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
class GachaControllerTest extends BaseControllerTest {

    @MockBean
    private GachaService gachaService;

    @MockBean
    private UserRepository userRepository;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(99);
        currentUser.setStudentCode("1412");
    }

    @Test
    @WithMockUser(username = "1412")
    @DisplayName("Spin gacha thành công trả về item và điểm còn lại")
    void spinGacha_shouldReturnGachaResponse() throws Exception {
        GachaResponse response = GachaResponse.builder()
                .message("Chúc mừng bạn đã quay trúng Khung Huyền Thoại!")
                .wonItem(Item.builder()
                        .id(200)
                        .name("Khung Huyền Thoại")
                        .rarity(com.example.backend.Enum.CosmeticRarity.EPIC)
                        .type(com.example.backend.Enum.CosmeticType.AVATAR_FRAME)
                        .effectKey("css-frame-epic")
                        .build())
                .remainingPoints(700)
                .build();

        when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(currentUser));
        when(gachaService.spin(99, com.example.backend.Enum.CosmeticTheme.FIRE)).thenReturn(response);

        mockMvc.perform(post("/api/gacha/spin")
                        .param("theme", "FIRE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Chúc mừng bạn đã quay trúng Khung Huyền Thoại!"))
                .andExpect(jsonPath("$.wonItem.name").value("Khung Huyền Thoại"))
                .andExpect(jsonPath("$.remainingPoints").value(700));

        verify(userRepository).findByStudentCode("1412");
        verify(gachaService).spin(99, com.example.backend.Enum.CosmeticTheme.FIRE);
    }

    @Test
    @WithMockUser(username = "1412")
    @DisplayName("Get gacha info thành công trả về tỷ lệ và số lượng")
    void getGachaInfo_shouldReturnInfoResponse() throws Exception {
        GachaInfoResponse infoResponse = GachaInfoResponse.builder()
                .theme("FIRE")
                .totalPoolSize(5)
                .ownedCount(2)
                .remainingForGuarantee(3)
                .currentRates(Map.of("COMMON", 60.0, "EPIC", 40.0))
                .build();

        when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(currentUser));
        when(gachaService.getGachaInfo(99, com.example.backend.Enum.CosmeticTheme.FIRE))
                .thenReturn(infoResponse);

        mockMvc.perform(get("/api/gacha/info")
                        .param("theme", "FIRE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("FIRE"))
                .andExpect(jsonPath("$.totalPoolSize").value(5))
                .andExpect(jsonPath("$.ownedCount").value(2))
                .andExpect(jsonPath("$.remainingForGuarantee").value(3))
                .andExpect(jsonPath("$.currentRates.COMMON").value(60.0))
                .andExpect(jsonPath("$.currentRates.EPIC").value(40.0));

        verify(userRepository).findByStudentCode("1412");
        verify(gachaService).getGachaInfo(99, com.example.backend.Enum.CosmeticTheme.FIRE);
    }

    @Test
    @WithMockUser(username = "1412")
    @DisplayName("Get gacha history thành công trả về danh sách lịch sử")
    void getGachaHistory_shouldReturnHistoryList() throws Exception {
        GachaHistoryItemDTO historyItem = GachaHistoryItemDTO.builder()
                .itemId(200)
                .name("Khung Huyền Thoại")
                .type("AVATAR_FRAME")
                .rarity("EPIC")
                .effectKey("css-frame-epic")
                .purchasedAt(java.time.LocalDateTime.now())
                .build();

        when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(currentUser));
        when(gachaService.getGachaHistory(99, com.example.backend.Enum.CosmeticTheme.FIRE))
                .thenReturn(List.of(historyItem));

        mockMvc.perform(get("/api/gacha/history")
                        .param("theme", "FIRE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemId").value(200))
                .andExpect(jsonPath("$[0].name").value("Khung Huyền Thoại"))
                .andExpect(jsonPath("$[0].rarity").value("EPIC"))
                .andExpect(jsonPath("$[0].effectKey").value("css-frame-epic"));

        verify(userRepository).findByStudentCode("1412");
        verify(gachaService).getGachaHistory(99, com.example.backend.Enum.CosmeticTheme.FIRE);
    }

    @Test
    @WithMockUser(username = "ghost")
    @DisplayName("Spin gacha khi user không tồn tại trả về lỗi bad request")
    void spinGacha_whenUserNotFound_shouldReturnBadRequest() throws Exception {
        when(userRepository.findByStudentCode("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/gacha/spin")
                        .param("theme", "FIRE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("User not found"));

        verify(userRepository).findByStudentCode("ghost");
    }

    @Test
    @WithMockUser(username = "ghost")
    @DisplayName("Get gacha info khi user không tồn tại trả về lỗi bad request")
    void getGachaInfo_whenUserNotFound_shouldReturnBadRequest() throws Exception {
        when(userRepository.findByStudentCode("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/gacha/info")
                        .param("theme", "FIRE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("User not found"));

        verify(userRepository).findByStudentCode("ghost");
    }

    @Test
    @WithMockUser(username = "ghost")
    @DisplayName("Get gacha history khi user không tồn tại trả về lỗi bad request")
    void getGachaHistory_whenUserNotFound_shouldReturnBadRequest() throws Exception {
        when(userRepository.findByStudentCode("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/gacha/history")
                        .param("theme", "FIRE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("User not found"));

        verify(userRepository).findByStudentCode("ghost");
    }
}
