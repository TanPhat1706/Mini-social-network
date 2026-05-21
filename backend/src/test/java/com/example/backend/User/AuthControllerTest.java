package com.example.backend.User;

import com.example.backend.Integration.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@WebMvcTest(
        value = AuthController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
class AuthControllerTest extends BaseControllerTest {

    @MockBean
    private AuthService authService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordResetService passwordResetService;


    private User mockUser;
    private UserResponse mockUserResponse;
    private SecurityHistory mockHistory;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1);
        mockUser.setStudentCode("1412");
        mockUser.setEmail("phat.le@student.com");
        mockUser.setFullName("Lê Hồng Phát");
        mockUser.setRole("STUDENT");
        mockUser.setLevel(10);
        mockUser.setVptlPoints(500);

        mockUserResponse = UserResponse.builder()
                .id(1)
                .studentCode("1412")
                .fullName("Lê Hồng Phát")
                .role("STUDENT")
                .level(10)
                .build();
                
        mockHistory = new SecurityHistory();
        mockHistory.setId(100);
        mockHistory.setUser(mockUser);
        mockHistory.setIpAddress("127.0.0.1");
        mockHistory.setBrowser("Chrome");
        mockHistory.setDevice("Windows");
        mockHistory.setLoginTime(LocalDateTime.now());
        mockHistory.setStatus("SUCCESS");
        mockHistory.setSessionId("mock-session-id");
        mockHistory.setIsActive(true);
    }

    // ==========================================
    // 1. TEST ĐĂNG KÝ (REGISTER)
    // ==========================================
    @Test
    void register_shouldReturn200() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setStudentCode("1412");
        req.setFullName("Le Hong Phat");
        req.setEmail("phat@student.com");
        req.setPassword("password123");

        when(authService.register(any(RegisterRequest.class))).thenReturn(mockUser);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // ==========================================
    // 2. TEST ĐĂNG NHẬP (LOGIN)
    // ==========================================
    @Test
    void login_shouldReturnFullInfoAndToken() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setIdentifier("1412");
        req.setPassword("password123");

        // 🟢 ĐÃ SỬA: Thêm anyString() cho tham số sessionId
        when(authService.login(any(LoginRequest.class), anyString())).thenReturn("fake-jwt-token");
        when(userRepository.findByStudentCodeOrEmail(anyString(), anyString()))
                .thenReturn(Optional.of(mockUser));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-jwt-token"))
                .andExpect(jsonPath("$.studentCode").value("1412"))
                .andExpect(jsonPath("$.level").value(10));
    }

    // ==========================================
    // 3. TEST TÌM KIẾM NGƯỜI DÙNG (SEARCH)
    // ==========================================
    @Test
    void searchUsers_shouldReturnList() throws Exception {
        when(authService.searchUsers("Hồng Phát")).thenReturn(List.of(mockUserResponse));

        mockMvc.perform(get("/api/auth/search")
                        .param("name", "Hồng Phát"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Lê Hồng Phát"));
    }

    // ==========================================
    // 4. TEST LẤY PROFILE CÁ NHÂN (GET PROFILE)
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void getProfile_shouldReturnUserResponse() throws Exception {
        when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(mockUser));

        mockMvc.perform(get("/api/auth/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentCode").value("1412"));
    }

    // ==========================================
    // 5. TEST CẬP NHẬT PROFILE (MULTIPART PUT)
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void updateProfile_shouldReturnUpdatedUser() throws Exception {
        MockMultipartFile avatar = new MockMultipartFile("avatar", "avatar.jpg", 
                MediaType.IMAGE_JPEG_VALUE, "image data".getBytes());
        
        when(authService.updateProfile(eq("1412"), anyString(), any(), any(), any(), any()))
                .thenReturn(mockUser);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/auth/profile")
                        .file(avatar)
                        .param("fullName", "Lê Hồng Phát Updated")
                        .param("bio", "Học lập trình vui vẻ")
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").exists());
    }

    // ==========================================
    // 6. 🟢 MỚI: TEST LẤY LỊCH SỬ BẢO MẬT
    // ==========================================
@Test
    @WithMockUser(username = "1412")
    void getSecurityHistory_shouldReturnHistoryList() throws Exception {
        when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(mockUser));
        
        // 🟢 CẬP NHẬT 1: Giả lập trả về đối tượng PageImpl thay vì List
        // Sử dụng eq(1) cho userId và any(Pageable.class) cho tham số phân trang
        when(securityHistoryRepository.findByUserIdOrderByLoginTimeDesc(eq(1), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(mockHistory)));
                
        when(jwtUtil.extractSessionId(anyString())).thenReturn("mock-session-id");

        mockMvc.perform(get("/api/auth/security-history")
                        // Tùy chọn: Truyền thêm param để mô phỏng giống Frontend gọi
                        .param("page", "0")
                        .param("size", "5")
                        .header("Authorization", "Bearer fake-token"))
                .andExpect(status().isOk())
                
                // 🟢 CẬP NHẬT 2: Sửa JSONPath từ $[0] thành $.content[0]
                .andExpect(jsonPath("$.content[0].ipAddress").value("127.0.0.1"))
                .andExpect(jsonPath("$.content[0].isCurrentDevice").value(true)) // Test cờ thiết bị hiện tại
                
                // 🟢 THÊM MỚI: Test luôn xem tổng số bản ghi có đúng là 1 không
                .andExpect(jsonPath("$.totalElements").value(1)); 
    }

    // ==========================================
    // 7. 🟢 MỚI: TEST ÉP ĐĂNG XUẤT (REVOKE)
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void revokeSession_shouldReturnOk_whenUserOwnsHistory() throws Exception {
        when(securityHistoryRepository.findById(100)).thenReturn(Optional.of(mockHistory));

        mockMvc.perform(post("/api/auth/security-history/100/revoke"))
                .andExpect(status().isOk())
                .andExpect(content().string("Đã đăng xuất thiết bị thành công"));

        // Kiểm tra xem cờ isActive đã được set thành false trước khi save chưa
        verify(securityHistoryRepository).save(argThat(history -> !history.getIsActive()));
    }
    
    @Test
    @WithMockUser(username = "HACKER_CODE")
    void revokeSession_shouldReturn403_whenUserDoesNotOwnHistory() throws Exception {
        when(securityHistoryRepository.findById(100)).thenReturn(Optional.of(mockHistory));

        mockMvc.perform(post("/api/auth/security-history/100/revoke"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Không có quyền thực hiện"));
    }
}