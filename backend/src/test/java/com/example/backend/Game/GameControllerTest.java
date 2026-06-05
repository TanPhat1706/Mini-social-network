package com.example.backend.Game;

import com.example.backend.Integration.BaseControllerTest;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 🟢 Ép Spring chỉ load đúng GameController và cấm tự động load Security Config
@WebMvcTest(
        value = GameController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
class GameControllerTest extends BaseControllerTest {

    @MockBean
    private GameService gameService;

    // ✅ Thêm mock UserRepository — GameController dùng để lookup user từ studentCode
    @MockBean
    private UserRepository userRepository;

    private GameScore gameScore;
    private User mockUser;

    @BeforeEach
    void setUp() {
        gameScore = new GameScore("snake", 1412, "Test STUDENT", "avatar.png", 321);
        gameScore.setPlayedAt(LocalDateTime.of(2026, 5, 4, 18, 0));

        // User giả khớp với @WithMockUser(username = "1412") — studentCode = "1412"
        mockUser = new User();
        mockUser.setId(1412);
        mockUser.setStudentCode("1412");
        mockUser.setFullName("Test STUDENT");
    }

    // ==========================================
    // 1. TEST LẤY LEADERBOARD (API CÔNG KHAI)
    // ==========================================
    @Test
    void getLeaderboard_whenPublicRequest_shouldReturnTopScores() throws Exception {
        when(gameService.getLeaderboard("snake")).thenReturn(List.of(gameScore));

        mockMvc.perform(get("/api/games/leaderboard/snake")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].gameKey").value("snake"))
                .andExpect(jsonPath("$[0].userId").value(1412))
                .andExpect(jsonPath("$[0].username").value("Test STUDENT"))
                .andExpect(jsonPath("$[0].score").value(321));
    }

    // ==========================================
    // 2. TEST GHI ĐIỂM THÀNH CÔNG (CẦN ĐĂNG NHẬP)
    // ==========================================
    @Test
    @WithMockUser(username = "1412") // JWT subject = studentCode
    void submitScore_whenAuthenticated_shouldReturnSavedScore() throws Exception {
        ScoreRequest request = new ScoreRequest();
        request.setGameKey("snake");
        request.setScore(321);

        // Stub: studentCode "1412" → mockUser (id=1412)
        when(userRepository.findByStudentCode(eq("1412"))).thenReturn(Optional.of(mockUser));
        // Stub: gameService nhận đúng userId=1412 (Integer) từ mockUser.getId()
        when(gameService.saveScore(eq(1412), eq("snake"), eq(321))).thenReturn(gameScore);

        mockMvc.perform(post("/api/games/score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Score saved!"))
                .andExpect(jsonPath("$.data.gameKey").value("snake"))
                .andExpect(jsonPath("$.data.userId").value(1412))
                .andExpect(jsonPath("$.data.score").value(321));
    }

    // ==========================================
    // 3. TEST GHI ĐIỂM THẤT BẠI (LOGIC LỖI)
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void submitScore_whenServiceThrowsException_shouldReturn400() throws Exception {
        ScoreRequest request = new ScoreRequest();
        request.setGameKey("snake");
        request.setScore(-50); // Điểm âm không hợp lệ

        // Stub lookup user trước — controller cần tìm user trước khi gọi service
        when(userRepository.findByStudentCode(eq("1412"))).thenReturn(Optional.of(mockUser));
        when(gameService.saveScore(eq(1412), eq("snake"), eq(-50)))
                .thenThrow(new RuntimeException("Điểm số không hợp lệ"));

        mockMvc.perform(post("/api/games/score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Điểm số không hợp lệ"));
    }
}