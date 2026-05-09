package com.example.backend.Game;

import com.example.backend.Enum.GameEventType;
import com.example.backend.Enum.GameSessionStatus;
import com.example.backend.Game.dto.AcceptInviteRequest;
import com.example.backend.Game.dto.GameMoveRequest;
import com.example.backend.Game.dto.GameWsEvent;
import com.example.backend.Game.dto.JoinRoomRequest;
import com.example.backend.Game.dto.StartGameRequest;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import static org.mockito.ArgumentMatchers.isNull;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicTacToeWsControllerTest {

    @Mock
    private TicTacToeService ticTacToeService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TicTacToeWsController ticTacToeWsController;

    private User hostUser;
    private User guestUser;
    private GameSession waitingSession;
    private GameSession finishedSession;

    @BeforeEach
    void setUp() {
        hostUser = buildUser(1, "HOST001", "host@example.com");
        guestUser = buildUser(2, "GUEST002", "guest@example.com");

        waitingSession = buildSession(88L, GameSessionStatus.WAITING, "---------", null, null);
        finishedSession = buildSession(88L, GameSessionStatus.FINISHED, "XXXOO----", null, 1);
    }

    @Test
    void acceptInvite_whenSuccess_shouldNotifyBothPlayers() {
        AcceptInviteRequest request = new AcceptInviteRequest();
        request.setInviteMessageId(99L);
        mockAuthenticatedUser(guestUser);

        when(ticTacToeService.acceptInvite(99L, 2)).thenReturn(waitingSession);
        when(userRepository.findById(1)).thenReturn(Optional.of(hostUser));
        when(userRepository.findById(2)).thenReturn(Optional.of(guestUser));

        ticTacToeWsController.acceptInvite(request, authentication);

        ArgumentCaptor<GameWsEvent> eventCaptor = ArgumentCaptor.forClass(GameWsEvent.class);
        verify(messagingTemplate).convertAndSendToUser(eq("HOST001"), eq("/queue/game-events"), eventCaptor.capture());
        verify(messagingTemplate).convertAndSendToUser(eq("GUEST002"), eq("/queue/game-events"), eventCaptor.capture());

        GameWsEvent firstEvent = eventCaptor.getAllValues().get(0);
        GameWsEvent secondEvent = eventCaptor.getAllValues().get(1);
        assertEquals(GameEventType.GAME_INVITE_ACCEPTED, firstEvent.getType());
        assertEquals("Invite accepted. Waiting room created.", firstEvent.getMessage());
        assertEquals(88L, firstEvent.getSession().getId());
        assertEquals(GameEventType.GAME_INVITE_ACCEPTED, secondEvent.getType());
        assertEquals(88L, secondEvent.getSession().getId());
    }

    @Test
    void acceptInvite_whenServiceThrows_shouldSendErrorToCurrentUser() {
        AcceptInviteRequest request = new AcceptInviteRequest();
        request.setInviteMessageId(99L);
        mockAuthenticatedUser(guestUser);

        when(ticTacToeService.acceptInvite(99L, 2)).thenThrow(new GameException("Invite already handled"));
        when(userRepository.findById(2)).thenReturn(Optional.of(guestUser));

        ticTacToeWsController.acceptInvite(request, authentication);

        ArgumentCaptor<GameWsEvent> eventCaptor = ArgumentCaptor.forClass(GameWsEvent.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq("GUEST002"),
                eq("/queue/game-events"),
                eventCaptor.capture());

        GameWsEvent errorEvent = eventCaptor.getValue();
        assertEquals(GameEventType.ERROR, errorEvent.getType());
        assertEquals("Invite already handled", errorEvent.getMessage());
    }

    @Test
    void joinRoom_whenSuccess_shouldBroadcastJoinedEventToRoom() {
        JoinRoomRequest request = new JoinRoomRequest();
        request.setSessionId(88L);
        mockAuthenticatedUser(guestUser);

        when(ticTacToeService.getSession(88L)).thenReturn(waitingSession);

        ticTacToeWsController.joinRoom(request, authentication);

        ArgumentCaptor<GameWsEvent> eventCaptor = ArgumentCaptor.forClass(GameWsEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/game/88"), eventCaptor.capture());

        GameWsEvent event = eventCaptor.getValue();
        assertEquals(GameEventType.ROOM_JOINED, event.getType());
        assertEquals("Player joined waiting room.", event.getMessage());
        assertEquals(2, event.getActorId());
        assertEquals(88L, event.getSession().getId());
    }

    @Test
    void makeMove_whenGameEnds_shouldBroadcastGameEndEvent() {
        GameMoveRequest request = new GameMoveRequest();
        request.setSessionId(88L);
        request.setRow(0);
        request.setCol(2);
        mockAuthenticatedUser(hostUser);

        when(ticTacToeService.makeMove(88L, 1, 0, 2)).thenReturn(finishedSession);

        ticTacToeWsController.makeMove(request, authentication);

        ArgumentCaptor<GameWsEvent> eventCaptor = ArgumentCaptor.forClass(GameWsEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/game/88"), eventCaptor.capture());

        GameWsEvent event = eventCaptor.getValue();
        assertEquals(GameEventType.GAME_END, event.getType());
        assertEquals("Game finished.", event.getMessage());
        assertEquals(1, event.getActorId());
        assertEquals(0, event.getRow());
        assertEquals(2, event.getCol());
        assertEquals(1, event.getSession().getWinnerId());
        assertEquals("X", event.getSession().getBoard()[0][0]);
        assertEquals("X", event.getSession().getBoard()[0][2]);
    }

    @Test
    void startGame_whenAuthenticationMissing_shouldThrowGameException() {
        StartGameRequest request = new StartGameRequest();
        request.setSessionId(88L);
        
        // Báo cho hệ thống biết là người dùng chưa đăng nhập
        when(authentication.isAuthenticated()).thenReturn(false);

        // Hứng lỗi văng ra
        GameException ex = assertThrows(
                GameException.class,
                () -> ticTacToeWsController.startGame(request, authentication));

        // Xác minh nội dung lỗi
        assertEquals("Unauthorized websocket request", ex.getMessage());
        
        // Xác minh Service chưa từng được gọi
        verify(ticTacToeService, never()).startGame(any(), any());
        
        // 🟢 SỬA CHỐT HẠ TẠI ĐÂY: Dùng any(GameWsEvent.class) để dập tắt lỗi Ambiguous
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(GameWsEvent.class));
    }

    private void mockAuthenticatedUser(User currentUser) {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(currentUser.getStudentCode());
        when(userRepository.findByStudentCode(currentUser.getStudentCode())).thenReturn(Optional.of(currentUser));
    }

    private User buildUser(Integer id, String studentCode, String email) {
        User user = new User();
        user.setId(id);
        user.setStudentCode(studentCode);
        user.setEmail(email);
        return user;
    }

    private GameSession buildSession(Long id, GameSessionStatus status, String board, Integer currentTurn,
            Integer winnerId) {
        GameSession session = new GameSession();
        session.setId(id);
        session.setHostId(1);
        session.setGuestId(2);
        session.setStatus(status);
        session.setBoard(board);
        session.setCurrentTurn(currentTurn);
        session.setWinnerId(winnerId);
        return session;
    }
}
