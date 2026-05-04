package com.example.backend.User;

import com.example.backend.Integration.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    private User mockUser;
    private UserResponse mockUserResponse;

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
    }

    // ==========================================
    // 1. TEST ĐĂNG KÝ (REGISTER)
    // ==========================================
    @Test
    void register_shouldReturn200() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setStudentCode("1412");
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

        when(authService.login(any(LoginRequest.class))).thenReturn("fake-jwt-token");
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

        // Chú ý: Multipart PUT cần dùng .with(request -> { request.setMethod("PUT"); return request; })
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/auth/profile")
                        .file(avatar)
                        .param("fullName", "Lê Hồng Phát Updated")
                        .param("bio", "Học lập trình vui vẻ")
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").exists());
    }
}