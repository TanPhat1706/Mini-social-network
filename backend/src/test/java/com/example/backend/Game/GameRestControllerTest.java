package com.example.backend.Game;

import com.example.backend.Integration.BaseControllerTest;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = GameRestController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
class GameRestControllerTest extends BaseControllerTest {

    @MockBean
    private TicTacToeService ticTacToeService;

    @MockBean
    private UserRepository userRepository;

    private User mockUser;
    private GameSession mockSession;

    @BeforeEach
    void setUp() {
        // Khởi tạo User giả
        mockUser = new User();
        mockUser.setId(1);
        mockUser.setStudentCode("HOST123");
        mockUser.setEmail("host@example.com");

        // Khởi tạo Session giả trả về từ Service
        mockSession = new GameSession();
        mockSession.setId(99L);
        mockSession.setHostId(1);
    }

    // ==========================================
    // TEST LUỒNG THÀNH CÔNG (TÌM BẰNG STUDENT CODE)
    // ==========================================
    @Test
    @DisplayName("Trả về GameSession thành công khi User tìm thấy bằng StudentCode")
    void createRoom_whenUserFoundByStudentCode_shouldReturnSession() throws Exception {
        when(userRepository.findByStudentCode("HOST123")).thenReturn(Optional.of(mockUser));
        when(ticTacToeService.createSession(1)).thenReturn(mockSession);

        // 🟢 CÁCH FIX: Tạo Auth giả và nhồi vào principal()
        Authentication mockAuth = new UsernamePasswordAuthenticationToken("HOST123", null);

        mockMvc.perform(post("/api/game/create")
                        .principal(mockAuth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.hostId").value(1));
    }

    // ==========================================
    // TEST LUỒNG THÀNH CÔNG (TÌM BẰNG EMAIL - LAMBDA)
    // ==========================================
    @Test
    @DisplayName("Trả về GameSession thành công khi User tìm thấy bằng Email")
    void createRoom_whenUserFoundByEmail_shouldReturnSession() throws Exception {
        when(userRepository.findByStudentCode("host@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("host@example.com")).thenReturn(Optional.of(mockUser));
        when(ticTacToeService.createSession(1)).thenReturn(mockSession);

        // 🟢 CÁCH FIX: Tạo Auth giả và nhồi vào principal()
        Authentication mockAuth = new UsernamePasswordAuthenticationToken("host@example.com", null);

        mockMvc.perform(post("/api/game/create")
                        .principal(mockAuth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.hostId").value(1));
    }

    // ==========================================
    // TEST LUỒNG THẤT BẠI (KHÔNG TÌM THẤY USER)
    // ==========================================
    @Test
    @DisplayName("Trả về HTTP 401 khi không tìm thấy User")
    void createRoom_whenUserNotFound_shouldReturn401() throws Exception {
        when(userRepository.findByStudentCode("GHOST")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("GHOST")).thenReturn(Optional.empty());

        // 🟢 CÁCH FIX: Tạo Auth giả và nhồi vào principal()
        Authentication mockAuth = new UsernamePasswordAuthenticationToken("GHOST", null);

        mockMvc.perform(post("/api/game/create")
                        .principal(mockAuth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized()) // 401
                .andExpect(jsonPath("$").value("User not found"));
    }
}