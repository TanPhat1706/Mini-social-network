package com.example.backend.Presence;

import com.example.backend.Integration.BaseControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = PresenceController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
class PresenceControllerTest extends BaseControllerTest {

    @MockBean
    private PresenceService presenceService;

    @Test
    @WithMockUser
    @DisplayName("Lấy trạng thái Presence của User thành công -> Trả về 200 OK")
    void getUserPresence_shouldReturn200AndPresenceData() throws Exception {
        // Chuẩn bị dữ liệu giả
        LocalDateTime mockTime = LocalDateTime.of(2026, 6, 18, 15, 30, 0);
        UserPresenceDTO mockResponse = new UserPresenceDTO("SV1412", true, mockTime);

        when(presenceService.getUserPresence("SV1412")).thenReturn(mockResponse);

        // Thực thi gọi API ảo
        mockMvc.perform(get("/api/users/SV1412/presence")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentCode").value("SV1412"))
                // Jackson tự động map 'boolean isOnline' thành 'online' trong JSON
                .andExpect(jsonPath("$.online").value(true)) 
                .andExpect(jsonPath("$.lastSeen").exists());

        // Kiểm chứng service có được gọi đúng tham số
        verify(presenceService).getUserPresence("SV1412");
    }

    @Test
    @WithMockUser
    @DisplayName("Lấy trạng thái khi User Offline -> isOnline = false")
    void getUserPresence_whenOffline_shouldReturnFalse() throws Exception {
        UserPresenceDTO mockResponse = new UserPresenceDTO("GHOST", false, null);

        when(presenceService.getUserPresence("GHOST")).thenReturn(mockResponse);

        mockMvc.perform(get("/api/users/GHOST/presence")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentCode").value("GHOST"))
                .andExpect(jsonPath("$.online").value(false))
                .andExpect(jsonPath("$.lastSeen").isEmpty());
    }
}