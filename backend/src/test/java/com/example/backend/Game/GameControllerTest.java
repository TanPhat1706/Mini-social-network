package com.example.backend.Game;

import com.example.backend.Integration.BaseControllerTest;
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

    private GameScore gameScore;

    @BeforeEach
    void setUp() {
        // Tui giữ nguyên constructor của bạn, giả sử tham số thứ 2 là userId (Integer)
        gameScore = new GameScore("snake", 1412, "Test STUDENT", "avatar.png", 321);
        gameScore.setPlayedAt(LocalDateTime.of(2026, 5, 4, 18, 0));
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
    @WithMockUser(username = "1412") // 🟢 Tiêm thẳng danh tính "1412" vào Context
    void submitScore_whenAuthenticated_shouldReturnSavedScore() throws Exception {
        ScoreRequest request = new ScoreRequest();
        request.setGameKey("snake");
        request.setScore(321);

        // Lưu ý: Mock tham số đầu tiên là chuỗi "1412" vì Controller đã được sửa lấy studentCode
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

        when(gameService.saveScore(eq(1412), eq("snake"), eq(-50)))
                .thenThrow(new RuntimeException("Điểm số không hợp lệ"));

        mockMvc.perform(post("/api/games/score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Điểm số không hợp lệ"));
    }
}