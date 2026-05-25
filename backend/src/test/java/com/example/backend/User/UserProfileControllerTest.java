package com.example.backend.User;

import com.example.backend.Integration.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = UserProfileController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
class UserProfileControllerTest extends BaseControllerTest {

    @MockBean
    private UserProfileService userProfileService;

    private UserProfileResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockResponse = new UserProfileResponse(
                1,
                "TARGET_SV", // Sẽ được map thành field "username" trong JSON
                "Nguyễn Văn Target",
                "target@test.com",
                "/avatar.jpg",
                "Bio của Target",
                "05/2026",
                false,
                "gold_frame",
                "#FF0000"
        );
    }

    // ==========================================
    // 1. TEST HAPPY PATH: NGƯỜI DÙNG ĐÃ ĐĂNG NHẬP
    // ==========================================
    @Test
    @DisplayName("Trả về Profile thành công khi người xem ĐÃ đăng nhập")
    void getUserProfile_whenAuthenticated_shouldReturn200() throws Exception {
        
        // 🟢 CÁCH FIX BẢO MẬT: Khởi tạo trực tiếp đối tượng Authentication giả
        Authentication mockAuth = new UsernamePasswordAuthenticationToken("VIEWER_SV", null);

        when(userProfileService.getProfile("VIEWER_SV", "TARGET_SV"))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/users/TARGET_SV/profile")
                        .principal(mockAuth)) // 🟢 Nhồi trực tiếp Auth giả vào Request
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("TARGET_SV")) // 🟢 SỬA THÀNH username
                .andExpect(jsonPath("$.fullName").value("Nguyễn Văn Target"))
                .andExpect(jsonPath("$.isSelfProfile").value(false));
    }

    // ==========================================
    // 2. TEST HAPPY PATH: KHÁCH VÃNG LAI (CHƯA ĐĂNG NHẬP)
    // ==========================================
    @Test
    @DisplayName("Trả về Profile thành công khi người xem CHƯA đăng nhập (Authentication null)")
    void getUserProfile_whenUnauthenticated_shouldReturn200() throws Exception {
        
        when(userProfileService.getProfile(isNull(), eq("TARGET_SV")))
                .thenReturn(mockResponse);

        // Không nhồi .principal() vào đây, nên Controller sẽ nhận Authentication = null
        mockMvc.perform(get("/api/users/TARGET_SV/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("TARGET_SV")); // 🟢 SỬA THÀNH username
    }

    // ==========================================
    // 3. TEST EXCEPTION PATH: KHÔNG TÌM THẤY USER
    // ==========================================
    @Test
    @DisplayName("Trả về lỗi 404 Not Found khi User mục tiêu không tồn tại")
    void getUserProfile_whenUserNotFound_shouldReturn404() throws Exception {
        
        // Nhồi giả Authentication
        Authentication mockAuth = new UsernamePasswordAuthenticationToken("VIEWER_SV", null);

        // Giờ thì bẫy Mockito đã khớp hoàn toàn với những gì Controller sẽ gọi
        when(userProfileService.getProfile("VIEWER_SV", "GHOST_SV"))
                .thenThrow(new UserProfileNotFoundException("Target user not found or inactive"));

        mockMvc.perform(get("/api/users/GHOST_SV/profile")
                        .principal(mockAuth)) // 🟢 Bắt buộc nhồi Auth giả
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"))
                .andExpect(jsonPath("$.message").value("Target user not found or inactive"));
    }
}