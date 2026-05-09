package com.example.backend.Game;

import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import com.example.backend.VPTLpoint.VptlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameScoreRepository gameScoreRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VptlService vptlService;

    @InjectMocks
    private GameService gameService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1);
        currentUser.setStudentCode("SV001");
        currentUser.setAvatarUrl("avatar.png");
    }

    @Test
    void saveScore_whenUserNotFound_shouldThrow() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> gameService.saveScore(99, "SNAKE", 100));
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void saveScore_whenFullNameIsNull_shouldUseStudentCode() {
        currentUser.setFullName(null); // Không có tên đầy đủ
        when(userRepository.findById(1)).thenReturn(Optional.of(currentUser));
        when(gameScoreRepository.save(any(GameScore.class))).thenAnswer(inv -> inv.getArgument(0));

        GameScore score = gameService.saveScore(1, "SNAKE", 500);

        assertEquals("SV001", score.getUsername()); // Dùng mã SV thay thế
        verify(vptlService).addGameExp(1, 500);
    }

    @Test
    void saveScore_whenFullNameExists_shouldUseFullName() {
        currentUser.setFullName("Lê Hồng Phát");
        when(userRepository.findById(1)).thenReturn(Optional.of(currentUser));
        when(gameScoreRepository.save(any(GameScore.class))).thenAnswer(inv -> inv.getArgument(0));

        GameScore score = gameService.saveScore(1, "SNAKE", 500);

        assertEquals("Lê Hồng Phát", score.getUsername());
        verify(vptlService).addGameExp(1, 500);
    }

    @Test
    void getLeaderboard_shouldCallRepository() {
        when(gameScoreRepository.findTop10ByGameKey("SNAKE")).thenReturn(List.of(new GameScore()));
        List<GameScore> board = gameService.getLeaderboard("SNAKE");
        assertEquals(1, board.size());
        verify(gameScoreRepository).findTop10ByGameKey("SNAKE");
    }
}