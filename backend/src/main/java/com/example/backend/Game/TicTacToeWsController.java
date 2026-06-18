package com.example.backend.Game;

import com.example.backend.Enum.GameEventType;
import com.example.backend.Enum.GameSessionStatus;
import com.example.backend.Game.dto.AcceptInviteRequest;
import com.example.backend.Game.dto.GameMoveRequest;
import com.example.backend.Game.dto.GameSessionPayload;
import com.example.backend.Game.dto.GameWsEvent;
import com.example.backend.Game.dto.JoinRoomRequest;
import com.example.backend.Game.dto.StartGameRequest;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class TicTacToeWsController {
    private final TicTacToeService ticTacToeService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    @MessageMapping("/game.invite.accept")
    public void acceptInvite(AcceptInviteRequest request, Authentication authentication) {
        Integer currentUserId = getCurrentUserId(authentication);

        try {
            GameSession session = ticTacToeService.acceptInvite(request.getInviteMessageId(), currentUserId);
            GameWsEvent event = buildEvent(GameEventType.GAME_INVITE_ACCEPTED, "Invite accepted. Waiting room created.", session);

            sendToUser(session.getHostId(), event);
            sendToUser(session.getGuestId(), event);
        } catch (GameException ex) {
            sendError(currentUserId, ex.getMessage());
        }
    }

    @MessageMapping("/game.room.join")
    public void joinRoom(JoinRoomRequest request, Authentication authentication) {
        Integer currentUserId = getCurrentUserId(authentication);
        try {
            GameSession session = ticTacToeService.getSession(request.getSessionId());
            GameWsEvent event = buildEvent(GameEventType.ROOM_JOINED, "Player joined waiting room.", session);
            event.setActorId(currentUserId);
            broadcastToRoom(session.getId(), event);
        } catch (GameException ex) {
            sendError(currentUserId, ex.getMessage());
        }
    }

    @MessageMapping("/game.start")
    public void startGame(StartGameRequest request, Authentication authentication) {
        Integer currentUserId = getCurrentUserId(authentication);
        try {
            GameSession session = ticTacToeService.startGame(request.getSessionId(), currentUserId);
            GameWsEvent event = buildEvent(GameEventType.GAME_START, "Game started.", session);
            broadcastToRoom(session.getId(), event);
        } catch (GameException ex) {
            sendError(currentUserId, ex.getMessage());
        }
    }

    @MessageMapping("/game.move")
    public void makeMove(GameMoveRequest request, Authentication authentication) {
        Integer currentUserId = getCurrentUserId(authentication);
        try {
            GameSession session = ticTacToeService.makeMove(
                    request.getSessionId(),
                    currentUserId,
                    request.getRow(),
                    request.getCol()
            );

            GameEventType eventType = session.getWinnerId() != null || session.getStatus() == GameSessionStatus.FINISHED
                    ? GameEventType.GAME_END
                    : GameEventType.GAME_UPDATE;

            GameWsEvent event = buildEvent(eventType, eventType == GameEventType.GAME_END ? "Game finished." : "Move updated.", session);
            event.setActorId(currentUserId);
            event.setRow(request.getRow());
            event.setCol(request.getCol());
            broadcastToRoom(session.getId(), event);
        } catch (GameException ex) {
            sendError(currentUserId, ex.getMessage());
        }
    }

    private Integer getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new GameException("Unauthorized websocket request");
        }
        String principal = authentication.getName();
        User currentUser = userRepository.findByStudentCode(principal)
                .orElseGet(() -> userRepository.findByEmail(principal).orElse(null));
        if (currentUser == null) {
            throw new GameException("Current user not found");
        }
        return currentUser.getId();
    }

    private GameWsEvent buildEvent(GameEventType type, String message, GameSession session) {
        GameWsEvent event = new GameWsEvent();
        event.setType(type);
        event.setMessage(message);
        event.setSession(GameSessionPayload.from(session));
        return event;
    }

    private void broadcastToRoom(Long sessionId, GameWsEvent event) {
        messagingTemplate.convertAndSend("/topic/game/" + sessionId, event);
    }

    private void sendToUser(Integer userId, GameWsEvent event) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            messagingTemplate.convertAndSendToUser(user.getStudentCode(), "/queue/game-events", event);
        }
    }

    private void sendError(Integer userId, String message) {
        GameWsEvent event = new GameWsEvent();
        event.setType(GameEventType.ERROR);
        event.setMessage(message);
        sendToUser(userId, event);
    }
}
