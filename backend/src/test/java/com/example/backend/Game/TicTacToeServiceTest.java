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
    // ==========================================
    // 🟢 BỔ SUNG: TEST CREATE SESSION & GET SESSION
    // ==========================================

    @Test
    void createSession_shouldReturnNewWaitingSession() {
        when(gameSessionRepository.save(any(GameSession.class))).thenAnswer(inv -> inv.getArgument(0));

        GameSession created = ticTacToeService.createSession(1);

        assertEquals(1, created.getHostId());
        assertEquals(GameSessionStatus.WAITING, created.getStatus());
        assertEquals("---------", created.getBoard());
        assertNotNull(created.getUpdatedAt());
    }

    @Test
    void getSession_whenNotFound_shouldThrowException() {
        when(gameSessionRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> ticTacToeService.getSession(99L));
        assertEquals("Game session not found", ex.getMessage());
    }

    // ==========================================
    // 🟢 BỔ SUNG: CÁC NHÁNH EXCEPTION CỦA ACCEPT INVITE
    // ==========================================

    @Test
    void acceptInvite_whenMessageNotFound_shouldThrow() {
        when(chatMessageRepository.findById(99L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ticTacToeService.acceptInvite(99L, 2));
        assertEquals("Invite message not found", ex.getMessage());
    }

    @Test
    void acceptInvite_whenNotGameInvite_shouldThrow() {
        ChatMessage msg = new ChatMessage();
        msg.setMessageType(MessageType.TEXT); // Sai loại tin nhắn
        when(chatMessageRepository.findById(100L)).thenReturn(Optional.of(msg));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> ticTacToeService.acceptInvite(100L, 2));
        assertEquals("Message is not a game invite", ex.getMessage());
    }

    @Test
    void acceptInvite_whenNotReceiver_shouldThrow() {
        ChatMessage msg = new ChatMessage();
        msg.setMessageType(MessageType.GAME_INVITE);
        msg.setReceiverId(2); // Người nhận là 2
        when(chatMessageRepository.findById(100L)).thenReturn(Optional.of(msg));

        // Người bấm chấp nhận là 3 (Kẻ gian)
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ticTacToeService.acceptInvite(100L, 3));
        assertEquals("Only invite receiver can accept", ex.getMessage());
    }

    // ==========================================
    // 🟢 BỔ SUNG: CÁC NHÁNH EXCEPTION CỦA START GAME
    // ==========================================

    @Test
    void startGame_whenNotHost_shouldThrow() {
        when(gameSessionRepository.findById(10L)).thenReturn(Optional.of(session));

        // Người bấm start là Guest (2), không phải Host (1)
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ticTacToeService.startGame(10L, 2));
        assertEquals("Only host can start game", ex.getMessage());
    }

    @Test
    void startGame_whenStatusNotWaiting_shouldThrow() {
        session.setStatus(GameSessionStatus.PLAYING); // Đã start rồi
        when(gameSessionRepository.findById(10L)).thenReturn(Optional.of(session));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> ticTacToeService.startGame(10L, 1));
        assertEquals("Game is not in waiting state", ex.getMessage());
    }

    // ==========================================
    // 🟢 BỔ SUNG: CÁC NHÁNH EXCEPTION CỦA MAKE MOVE & VALIDATE PLAYER
    // ==========================================

    @Test
    void makeMove_whenNotInSession_shouldThrow() {
        when(gameSessionRepository.findById(10L)).thenReturn(Optional.of(session));

        // Người đánh là 99 (Không phải host 1, cũng không phải guest 2)
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ticTacToeService.makeMove(10L, 99, 0, 0));
        assertEquals("You are not in this game session", ex.getMessage());
    }

    @Test
    void makeMove_whenGameNotPlaying_shouldThrow() {
        session.setStatus(GameSessionStatus.FINISHED);
        when(gameSessionRepository.findById(10L)).thenReturn(Optional.of(session));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> ticTacToeService.makeMove(10L, 1, 0, 0));
        assertEquals("Game is not playing", ex.getMessage());
    }

    @Test
    void makeMove_whenBoundsAreNullOrNegative_shouldThrow() {
        // Kiểm tra nhánh row == null hoặc col == null hoặc số âm
        assertThrows(RuntimeException.class, () -> ticTacToeService.makeMove(10L, 1, null, 0));
        assertThrows(RuntimeException.class, () -> ticTacToeService.makeMove(10L, 1, 0, null));
        assertThrows(RuntimeException.class, () -> ticTacToeService.makeMove(10L, 1, -1, 0));
        assertThrows(RuntimeException.class, () -> ticTacToeService.makeMove(10L, 1, 0, 3));
    }

    // ==========================================
    // 🟢 BỔ SUNG: KỊCH BẢN GUEST (O) CHIẾN THẮNG
    // ==========================================

    @Test
    void makeMove_whenGuestWins_shouldFinishAndReward() {
        // Tình huống: O O - | X X - | - - -
        session.setBoard("OO-XX----");
        session.setCurrentTurn(2); // Lượt của Guest
        when(gameSessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(gameSessionRepository.save(any(GameSession.class))).thenAnswer(inv -> inv.getArgument(0));

        // Guest (O) đánh vào ô (0, 2) tạo thành hàng ngang 3 O
        GameSession updated = ticTacToeService.makeMove(10L, 2, 0, 2);

        assertEquals("OOOXX----", updated.getBoard());
        assertEquals(GameSessionStatus.FINISHED, updated.getStatus());
        assertEquals(2, updated.getWinnerId()); // Guest thắng

        // Kiểm tra phần thưởng (Guest +100, Host +20)
        verify(vptlService).addGameExp(2, 100); 
        verify(vptlService).addGameExp(1, 20);  
    }
}