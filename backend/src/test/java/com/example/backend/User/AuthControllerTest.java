package com.example.backend.User;

import com.example.backend.Integration.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@WebMvcTest(value = AuthController.class, excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
})
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
                                .with(request -> {
                                        request.setMethod("PUT");
                                        return request;
                                })
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
                                .andExpect(jsonPath("$.content[0].isCurrentDevice").value(true)) // Test cờ thiết bị
                                                                                                 // hiện tại

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

        // ==========================================
        // 🟢 BỔ SUNG: TEST CÁC LUỒNG EXCEPTION CỦA ĐĂNG KÝ / ĐĂNG NHẬP / SEARCH
        // ==========================================
        @Test
        void register_whenExceptionThrown_shouldReturn400() throws Exception {
                RegisterRequest req = new RegisterRequest();
                req.setStudentCode("1412");
                // 🟢 Bơm đủ dữ liệu bắt buộc để lọt qua ải @Valid
                req.setFullName("Lê Hồng Phát");
                req.setEmail("phat@test.com");
                req.setPassword("password123");

                // Giả lập logic Service ném ra lỗi do trùng mã SV
                when(authService.register(any())).thenThrow(new RuntimeException("Mã sinh viên đã tồn tại!"));

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string("Mã sinh viên đã tồn tại!"));
        }

        @Test
        void login_whenExceptionThrown_shouldReturn401() throws Exception {
                LoginRequest req = new LoginRequest();
                req.setIdentifier("1412");
                req.setPassword("wrongpass");

                when(authService.login(any(), any())).thenThrow(new RuntimeException("Sai mật khẩu"));

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(content().string("Sai mật khẩu"));
        }

        @Test
        void searchUsers_whenQueryEmpty_shouldReturnEmptyList() throws Exception {
                mockMvc.perform(get("/api/auth/search").param("name", "   "))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isEmpty());
        }

        // ==========================================
        // 🟢 BỔ SUNG: TEST QUÊN MẬT KHẨU & ĐẶT LẠI MẬT KHẨU
        // ==========================================

        @Test
        void forgotPassword_shouldAlwaysReturn200_ToPreventEnumeration() throws Exception {
                // Test luồng bình thường
                mockMvc.perform(post("/api/auth/forgot-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"test@test.com\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message")
                                                .value("If the email matches our records, a reset link has been sent"));

                // Test luồng văng Exception cũng phải trả về 200 y hệt (Tránh lộ info)
                doThrow(new RuntimeException("DB down")).when(passwordResetService)
                                .asyncInitiateForgotPassword(anyString());
                mockMvc.perform(post("/api/auth/forgot-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"error@test.com\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message")
                                                .value("If the email matches our records, a reset link has been sent"));
        }
        // ==========================================
        // 🟢 TEST ĐẶT LẠI MẬT KHẨU (RESET PASSWORD)
        // ==========================================

        @Test
        void resetPassword_whenSuccess_shouldReturn200() throws Exception {
                PasswordResetRequest req = new PasswordResetRequest();
                // 🟢 Bơm dữ liệu giả để qua ải @Valid
                req.setToken("valid-token");
                req.setNewPassword("newPass123");
                req.setConfirmPassword("newPass123");

                when(passwordResetService.resetPassword(any())).thenReturn(true);

                mockMvc.perform(post("/api/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Password updated successfully"));
        }

        @Test
        void resetPassword_whenInvalidToken_shouldReturn400() throws Exception {
                PasswordResetRequest req = new PasswordResetRequest();
                // 🟢 Bơm dữ liệu giả để qua ải @Valid
                req.setToken("invalid-token");
                req.setNewPassword("newPass123");
                req.setConfirmPassword("newPass123");

                when(passwordResetService.resetPassword(any())).thenReturn(false);

                mockMvc.perform(post("/api/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Invalid or expired token"));
        }

        @Test
        void resetPassword_whenBadRequestException_shouldReturn400() throws Exception {
                PasswordResetRequest req = new PasswordResetRequest();
                // 🟢 Bơm dữ liệu giả để qua ải @Valid
                req.setToken("valid-token");
                req.setNewPassword("newPass123");
                req.setConfirmPassword("mismatchPass");

                when(passwordResetService.resetPassword(any()))
                                .thenThrow(new BadRequestException("Mật khẩu không khớp"));

                mockMvc.perform(post("/api/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Mật khẩu không khớp"));
        }

        // ==========================================
        // 🟢 TEST ĐỔI MẬT KHẨU (CHANGE PASSWORD)
        // ==========================================

        @Test
        @WithMockUser(username = "1412")
        void changePassword_shouldReturn200() throws Exception {
                ChangePasswordRequest req = new ChangePasswordRequest();
                // 🟢 Bơm dữ liệu giả để qua ải @Valid
                req.setOldPassword("oldPass");
                req.setNewPassword("newPass123");
                req.setConfirmPassword("newPass123");

                when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(mockUser));

                // void method không cần mock return, nó sẽ chạy mượt qua
                mockMvc.perform(post("/api/auth/change-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Mật khẩu đã được cập nhật thành công"));
        }

        @Test
        @WithMockUser(username = "1412")
        void changePassword_whenBadRequestException_shouldReturn400() throws Exception {
                // 1. Cung cấp dữ liệu đầy đủ để vượt qua @Valid
                ChangePasswordRequest req = new ChangePasswordRequest();
                req.setOldPassword("oldPass");
                req.setNewPassword("newPass123");
                req.setConfirmPassword("newPass123");

                when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(mockUser));

                // 2. Giả lập logic Service ném ra lỗi
                doThrow(new BadRequestException("Mật khẩu cũ sai")).when(authService).changePassword(anyInt(), any());

                mockMvc.perform(post("/api/auth/change-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest()) // 400
                                .andExpect(jsonPath("$.message").value("Mật khẩu cũ sai")); // Chắc chắn sẽ pass
        }
        // ==========================================
        // 🟢 BỔ SUNG NHÓM 1: CÁC NHÁNH ĐIỀU KIỆN (BRANCHES) TRONG LOGIN & HISTORY
        // ==========================================

        @Test
        @DisplayName("Login - Trích xuất IP thành công từ X-Forwarded-For")
        void login_withValidXForwardedFor_shouldExtractIp() throws Exception {
                LoginRequest req = new LoginRequest();
                req.setIdentifier("1412");
                req.setPassword("pass");

                when(authService.login(any(), anyString())).thenReturn("token");
                when(userRepository.findByStudentCodeOrEmail(anyString(), anyString()))
                                .thenReturn(Optional.of(mockUser));

                mockMvc.perform(post("/api/auth/login")
                                .header("X-Forwarded-For", "192.168.1.100") // 🟢 Nhánh có IP proxy
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Login - X-Forwarded-For mang giá trị unknown hoặc rỗng")
        void login_withUnknownXForwardedFor_shouldFallbackToRemoteAddr() throws Exception {
                LoginRequest req = new LoginRequest();
                req.setIdentifier("1412");
                req.setPassword("pass");

                when(authService.login(any(), anyString())).thenReturn("token");
                when(userRepository.findByStudentCodeOrEmail(anyString(), anyString()))
                                .thenReturn(Optional.of(mockUser));

                // 🟢 Nhánh "unknown"
                mockMvc.perform(post("/api/auth/login")
                                .header("X-Forwarded-For", "unknown")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk());

                // 🟢 Nhánh rỗng
                mockMvc.perform(post("/api/auth/login")
                                .header("X-Forwarded-For", "")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "1412")
        @DisplayName("GetSecurityHistory - Bỏ qua khi không có Header hoặc Header sai định dạng")
        void getSecurityHistory_whenNoAuthHeader_orInvalidHeader() throws Exception {
                when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(mockUser));
                when(securityHistoryRepository.findByUserIdOrderByLoginTimeDesc(eq(1), any(Pageable.class)))
                                .thenReturn(new PageImpl<>(List.of(mockHistory)));

                // 🟢 Nhánh 1: Không truyền Authorization header
                mockMvc.perform(get("/api/auth/security-history"))
                                .andExpect(status().isOk());

                // 🟢 Nhánh 2: Truyền Header nhưng không có chữ Bearer
                mockMvc.perform(get("/api/auth/security-history")
                                .header("Authorization", "Basic fake-token"))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("SearchUsers - Xử lý an toàn khi query rỗng")
        void searchUsers_whenQueryIsEmptyString_shouldReturnEmptyList() throws Exception {
                // 🟢 Dùng "" (rỗng) thay vì " " để vét cạn luồng isEmpty()
                mockMvc.perform(get("/api/auth/search").param("name", ""))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isEmpty());
        }

        // ==========================================
        // 🟢 BỔ SUNG NHÓM 2: CÁC KHỐI CATCH (EXCEPTION E) PHÒNG THỦ
        // ==========================================

        @Test
        @WithMockUser(username = "1412")
        void updateProfile_whenGenericException_shouldReturn400() throws Exception {
                MockMultipartFile avatar = new MockMultipartFile("avatar", "a.jpg", "image/jpeg", "data".getBytes());

                // Giả lập lỗi ném ra từ Service
                when(authService.updateProfile(anyString(), any(), any(), any(), any(), any()))
                                .thenThrow(new RuntimeException("Lỗi máy chủ S3"));

                mockMvc.perform(MockMvcRequestBuilders.multipart("/api/auth/profile")
                                .file(avatar)
                                .with(request -> {
                                        request.setMethod("PUT");
                                        return request;
                                })
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string("Lỗi cập nhật: Lỗi máy chủ S3"));
        }

        @Test
        @WithMockUser(username = "1412")
        void changePassword_whenGenericException_shouldReturn400() throws Exception {
                ChangePasswordRequest req = new ChangePasswordRequest();
                req.setOldPassword("oldPass");
                req.setNewPassword("newPass");
                req.setConfirmPassword("newPass");
                when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(mockUser));

                // Giả lập Exception chung (không phải BadRequestException)
                doThrow(new RuntimeException("System error")).when(authService).changePassword(anyInt(), any());

                mockMvc.perform(post("/api/auth/change-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Lỗi khi thay đổi mật khẩu"));
        }

        @Test
        void resetPassword_whenGenericException_shouldReturn400() throws Exception {
                PasswordResetRequest req = new PasswordResetRequest();
                req.setToken("valid-token");
                req.setNewPassword("newPass");
                req.setConfirmPassword("newPass");

                // Giả lập lỗi Exception chung
                when(passwordResetService.resetPassword(any())).thenThrow(new RuntimeException("Unknown DB Error"));

                mockMvc.perform(post("/api/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Invalid or expired token"));
        }

        @Test
        @WithMockUser(username = "1412")
        void getSecurityHistory_whenException_shouldReturn400() throws Exception {
                when(userRepository.findByStudentCode("1412")).thenThrow(new RuntimeException("Database timeout"));

                mockMvc.perform(get("/api/auth/security-history"))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string("Lỗi lấy lịch sử bảo mật: Database timeout"));
        }

        @Test
        @WithMockUser(username = "1412")
        void revokeSession_whenException_shouldReturn400() throws Exception {
                when(securityHistoryRepository.findById(anyInt())).thenThrow(new RuntimeException("Lỗi hệ thống"));

                mockMvc.perform(post("/api/auth/security-history/100/revoke"))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string("Lỗi: Lỗi hệ thống"));
        }
        // ==========================================
        // 🟢 BỔ SUNG: TEST CÁC NHÁNH LAMBDA (.orElseThrow) BỊ MISS
        // ==========================================

        @Test
        @DisplayName("Login - Bắn lỗi 401 khi không tìm thấy User trong DB")
        void login_whenUserNotFoundInDb_shouldThrowAndReturn401() throws Exception {
                LoginRequest req = new LoginRequest();
                req.setIdentifier("1412");
                req.setPassword("password");

                when(authService.login(any(), anyString())).thenReturn("token");

                // 🟢 Ép Optional.empty() để kích hoạt nhánh .orElseThrow
                when(userRepository.findByStudentCodeOrEmail(anyString(), anyString())).thenReturn(Optional.empty());

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(content().string("User not found"));
        }

        @Test
        @WithMockUser(username = "1412")
        @DisplayName("Change Password - Bắn lỗi 400 khi User Token không hợp lệ")
        void changePassword_whenUserNotFound_shouldReturn400() throws Exception {
                ChangePasswordRequest req = new ChangePasswordRequest();
                req.setOldPassword("oldPass");
                req.setNewPassword("newPass");
                req.setConfirmPassword("newPass");

                // 🟢 Kích hoạt nhánh .orElseThrow
                when(userRepository.findByStudentCode("1412")).thenReturn(Optional.empty());

                mockMvc.perform(post("/api/auth/change-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Người dùng không hợp lệ"));
        }

        @Test
        @WithMockUser(username = "1412")
        @DisplayName("Get Profile - Ném RuntimeException khi không tìm thấy User")
        void getProfile_whenUserNotFound_shouldThrowException() throws Exception {
                // 🟢 Kích hoạt nhánh .orElseThrow
                when(userRepository.findByStudentCode("1412")).thenReturn(Optional.empty());

                mockMvc.perform(get("/api/auth/profile"))
                                // Vì Controller getProfile không có khối try-catch, Spring sẽ văng thẳng
                                // exception ra ngoài
                                .andExpect(result -> org.junit.jupiter.api.Assertions
                                                .assertTrue(result.getResolvedException() instanceof RuntimeException))
                                .andExpect(result -> org.junit.jupiter.api.Assertions.assertEquals("User not found",
                                                result.getResolvedException().getMessage()));
        }

        @Test
        @WithMockUser(username = "1412")
        @DisplayName("Revoke Session - Bắn lỗi 400 khi ID lịch sử không tồn tại")
        void revokeSession_whenHistoryNotFound_shouldReturn400() throws Exception {
                // 🟢 Kích hoạt nhánh .orElseThrow
                when(securityHistoryRepository.findById(999)).thenReturn(Optional.empty());

                mockMvc.perform(post("/api/auth/security-history/999/revoke"))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string("Lỗi: Không tìm thấy lịch sử"));
        }
}