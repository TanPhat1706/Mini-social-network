package com.example.backend.Game;

import com.example.backend.Chat.ChatMessage;
import com.example.backend.Chat.ChatMessageRepository;
import com.example.backend.Enum.GameSessionStatus;
import com.example.backend.Enum.MessageType;
import com.example.backend.VPTLpoint.VptlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicTacToeServiceTest {

    @Mock
    private GameSessionRepository gameSessionRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private VptlService vptlService;

    @InjectMocks
    private TicTacToeService ticTacToeService;

    private GameSession session;

    @BeforeEach
    void setUp() {
        session = new GameSession();
        session.setId(10L);
        session.setHostId(1);
        session.setGuestId(2);
        session.setBoard("---------");
        session.setStatus(GameSessionStatus.PLAYING);
        session.setCurrentTurn(1);
    }

    // ==========================================
    // TEST LỜI MỜI & KHỞI TẠO
    // ==========================================

    @Test
    void acceptInvite_whenValid_shouldCreateSession() {
        ChatMessage msg = new ChatMessage();
        msg.setId(100L);
        msg.setMessageType(MessageType.GAME_INVITE);
        msg.setSenderId(1);
        msg.setReceiverId(2);

        when(chatMessageRepository.findById(100L)).thenReturn(Optional.of(msg));
        when(gameSessionRepository.save(any(GameSession.class))).thenAnswer(inv -> {
            GameSession s = inv.getArgument(0);
            s.setId(99L);
            return s;
        });

        GameSession saved = ticTacToeService.acceptInvite(100L, 2);

        assertEquals(GameSessionStatus.WAITING, saved.getStatus());
        assertEquals(1, saved.getHostId());
        assertEquals(2, saved.getGuestId());
        assertEquals("---------", saved.getBoard());
        verify(chatMessageRepository).save(msg);
        assertEquals(99L, msg.getGameSessionId());
    }

    @Test
    void startGame_whenValid_shouldSetPlayingAndRandomTurn() {
        session.setStatus(GameSessionStatus.WAITING);
        when(gameSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(gameSessionRepository.save(any(GameSession.class))).thenAnswer(inv -> inv.getArgument(0));

        GameSession started = ticTacToeService.startGame(10L, 1); // 1 is Host

        assertEquals(GameSessionStatus.PLAYING, started.getStatus());
        assertNotNull(started.getCurrentTurn());
        assertTrue(started.getCurrentTurn() == 1 || started.getCurrentTurn() == 2);
    }

    // ==========================================
    // TEST ĐÁNH CỜ (NGOẠI LỆ & BẢO MẬT)
    // ==========================================

    @Test
    void makeMove_whenInvalidBounds_shouldThrow() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ticTacToeService.makeMove(10L, 1, 3, 0));
        assertEquals("Invalid move position", ex.getMessage());
    }

    @Test
    void makeMove_whenNotYourTurn_shouldThrow() {
        when(gameSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        session.setCurrentTurn(2); // Lượt của Guest

        RuntimeException ex = assertThrows(RuntimeException.class, () -> ticTacToeService.makeMove(10L, 1, 0, 0));
        assertEquals("Not your turn", ex.getMessage());
    }

    @Test
    void makeMove_whenCellOccupied_shouldThrow() {
        session.setBoard("X--------"); // Ô (0,0) đã bị đánh
        when(gameSessionRepository.findById(10L)).thenReturn(Optional.of(session));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> ticTacToeService.makeMove(10L, 1, 0, 0));
        assertEquals("Cell already occupied", ex.getMessage());
    }

    // ==========================================
    // TEST ĐÁNH CỜ (THẮNG - THUA - HÒA)
    // ==========================================

    @Test
    void makeMove_whenNormalMove_shouldSwitchTurn() {
        when(gameSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(gameSessionRepository.save(any(GameSession.class))).thenAnswer(inv -> inv.getArgument(0));

        GameSession updated = ticTacToeService.makeMove(10L, 1, 1, 1); // Đánh vào giữa

        assertEquals("----X----", updated.getBoard());
        assertEquals(2, updated.getCurrentTurn()); // Đổi turn cho Guest
        assertEquals(GameSessionStatus.PLAYING, updated.getStatus());
    }

    @Test
    void makeMove_whenHostWins_shouldFinishAndReward() {
        // Tình huống: X X - | O O - | - - -
        session.setBoard("XX-OO----");
        when(gameSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(gameSessionRepository.save(any(GameSession.class))).thenAnswer(inv -> inv.getArgument(0));

        // Host (X) đánh vào ô (0, 2) tạo thành hàng ngang 3 X
        GameSession updated = ticTacToeService.makeMove(10L, 1, 0, 2);

        assertEquals("XXXOO----", updated.getBoard());
        assertEquals(GameSessionStatus.FINISHED, updated.getStatus());
        assertEquals(1, updated.getWinnerId());
        assertNull(updated.getCurrentTurn());

        // Kiểm tra phần thưởng
        verify(vptlService).addGameExp(1, 100); // Host thắng
        verify(vptlService).addGameExp(2, 20);  // Guest thua
    }

    @Test
    void makeMove_whenDraw_shouldFinishAndRewardBoth() {
        // Tình huống 1 bước trước khi hòa: 
        // X O X
        // X O O
        // O X -
        session.setBoard("XOXXOOOX-"); 
        when(gameSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(gameSessionRepository.save(any(GameSession.class))).thenAnswer(inv -> inv.getArgument(0));

        // Host (X) đánh vào ô cuối (2, 2)
        GameSession updated = ticTacToeService.makeMove(10L, 1, 2, 2);

        assertEquals("XOXXOOOXX", updated.getBoard());
        assertEquals(GameSessionStatus.FINISHED, updated.getStatus());
        assertNull(updated.getWinnerId()); // Hòa thì không có Winner

        // Kiểm tra phần thưởng (Hòa chia đều 50)
        verify(vptlService).addGameExp(1, 50); 
        verify(vptlService).addGameExp(2, 50); 
    }
}