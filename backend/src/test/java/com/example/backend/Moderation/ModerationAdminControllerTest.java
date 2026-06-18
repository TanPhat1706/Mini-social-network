package com.example.backend.Moderation;

import com.example.backend.Integration.BaseControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        value = ModerationAdminController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
class ModerationAdminControllerTest extends BaseControllerTest {

    @MockBean
    private BlacklistedWordRepository blacklistedWordRepository;

    @MockBean
    private AutoModerationService autoModerationService;

    // ==========================================
    // 1. TEST API GET (LẤY DANH SÁCH TỪ CẤM)
    // ==========================================
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /blacklist -> Trả về danh sách từ khóa và HTTP 200")
    void getAllBadWords_shouldReturnListAnd200() throws Exception {
        // Khởi tạo đối tượng giả lập
        BlacklistedWord word1 = new BlacklistedWord("từ_cấm_1");
        BlacklistedWord word2 = new BlacklistedWord("từ_cấm_2");

        when(blacklistedWordRepository.findAll()).thenReturn(List.of(word1, word2));

        mockMvc.perform(get("/api/admin/moderation/blacklist")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].word").value("từ_cấm_1"))
                .andExpect(jsonPath("$[1].word").value("từ_cấm_2"));

        verify(blacklistedWordRepository).findAll();
    }

    // ==========================================
    // 2. TEST API POST (THÊM TỪ CẤM)
    // ==========================================
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /blacklist (Hợp lệ) -> Lưu DB, Làm mới RAM và HTTP 200")
    void addBadWord_whenValid_shouldSaveAndRefreshCache() throws Exception {
        String payloadJson = "{\"word\": \"độc_hại\"}";

        mockMvc.perform(post("/api/admin/moderation/blacklist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadJson))
                .andExpect(status().isOk())
                .andExpect(content().string("Đã thêm từ khóa cấm thành công"));

        // Kiểm chứng Repository đã gọi save()
        verify(blacklistedWordRepository).save(any(BlacklistedWord.class));
        
        // Kiểm chứng Bot đã gọi refreshCache() để đồng bộ RAM
        verify(autoModerationService).refreshCache();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /blacklist (Để trống) -> Không lưu, Trả về HTTP 400")
    void addBadWord_whenEmptyWord_shouldReturn400() throws Exception {
        // Payload chứa khoảng trắng
        String payloadJson = "{\"word\": \"   \"}";

        mockMvc.perform(post("/api/admin/moderation/blacklist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Từ khóa không được để trống"));

        // Kiểm chứng không có thao tác nào gọi tới DB hoặc Cache
        verify(blacklistedWordRepository, org.mockito.Mockito.never()).save(any());
        verify(autoModerationService, org.mockito.Mockito.never()).refreshCache();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /blacklist (Null payload) -> Không lưu, Trả về HTTP 400")
    void addBadWord_whenNullWord_shouldReturn400() throws Exception {
        // Payload cố tình gửi thiếu field "word"
        String payloadJson = "{\"some_other_key\": \"value\"}";

        mockMvc.perform(post("/api/admin/moderation/blacklist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Từ khóa không được để trống"));
    }

    // ==========================================
    // 3. TEST API DELETE (XÓA TỪ CẤM)
    // ==========================================
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /blacklist/{id} -> Xóa DB, Làm mới RAM và HTTP 200")
    void removeBadWord_shouldDeleteAndRefreshCache() throws Exception {
        mockMvc.perform(delete("/api/admin/moderation/blacklist/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Đã xóa từ khóa cấm"));

        // Kiểm chứng DB bị xóa đúng ID
        verify(blacklistedWordRepository).deleteById(1);
        
        // Kiểm chứng Bot đồng bộ RAM
        verify(autoModerationService).refreshCache();
    }
}