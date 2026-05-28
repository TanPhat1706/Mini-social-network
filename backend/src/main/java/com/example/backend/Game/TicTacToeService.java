package com.example.backend.Game;

import com.example.backend.Chat.ChatMessage;
import com.example.backend.Chat.ChatMessageRepository;
import com.example.backend.Enum.GameSessionStatus;
import com.example.backend.Enum.MessageType;
import com.example.backend.VPTLpoint.VptlService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
// import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class TicTacToeService {
    private static final String EMPTY_BOARD = "---------";

    private final GameSessionRepository gameSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final VptlService vptlService;
    // private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public GameSession acceptInvite(Long inviteMessageId, Integer accepterId) {
        ChatMessage inviteMessage = chatMessageRepository.findById(inviteMessageId)
                .orElseThrow(() -> new GameException("Invite message not found"));

        if (inviteMessage.getMessageType() != MessageType.GAME_INVITE) {
            throw new GameException("Message is not a game invite");
        }
        if (!accepterId.equals(inviteMessage.getReceiverId())) {
            throw new GameException("Only invite receiver can accept");
        }

        GameSession session = new GameSession();
        session.setHostId(inviteMessage.getSenderId());
        session.setGuestId(inviteMessage.getReceiverId());
        session.setStatus(GameSessionStatus.WAITING);
        session.setBoard(EMPTY_BOARD);
        session.setCurrentTurn(null);
        session.setWinnerId(null);
        session.setUpdatedAt(LocalDateTime.now());
        GameSession saved = gameSessionRepository.save(session);

        inviteMessage.setGameSessionId(saved.getId());
        chatMessageRepository.save(inviteMessage);
        return saved;
    }

    @Transactional
    public GameSession startGame(Long sessionId, Integer actorId) {
        GameSession session = getSession(sessionId);
        if (!actorId.equals(session.getHostId())) {
            throw new GameException("Only host can start game");
        }
        if (session.getStatus() != GameSessionStatus.WAITING) {
            throw new GameException("Game is not in waiting state");
        }

        Integer firstTurn = ThreadLocalRandom.current().nextBoolean() ? session.getHostId() : session.getGuestId();
        session.setBoard(EMPTY_BOARD);
        session.setCurrentTurn(firstTurn);
        session.setStatus(GameSessionStatus.PLAYING);
        session.setWinnerId(null);
        session.setUpdatedAt(LocalDateTime.now());
        return gameSessionRepository.save(session);
    }

    @Transactional
    public GameSession makeMove(Long sessionId, Integer actorId, Integer row, Integer col) {
        if (row == null || col == null || row < 0 || row > 2 || col < 0 || col > 2) {
            throw new GameException("Invalid move position");
        }

        GameSession session = getSession(sessionId);
        validatePlayer(session, actorId);

        if (session.getStatus() != GameSessionStatus.PLAYING) {
            throw new GameException("Game is not playing");
        }
        if (!actorId.equals(session.getCurrentTurn())) {
            throw new GameException("Not your turn");
        }

        int index = row * 3 + col;
        char[] board = session.getBoard().toCharArray();
        if (board[index] != '-') {
            throw new GameException("Cell already occupied");
        }

        board[index] = actorId.equals(session.getHostId()) ? 'X' : 'O';
        session.setBoard(new String(board));
        session.setUpdatedAt(LocalDateTime.now());

        Integer winnerId = resolveWinnerId(session, board);
        boolean draw = isDraw(board);

        if (winnerId != null) {
            session.setStatus(GameSessionStatus.FINISHED);
            session.setWinnerId(winnerId);
            rewardForWinLose(session, winnerId);
            session.setCurrentTurn(null);
        } else if (draw) {
            session.setStatus(GameSessionStatus.FINISHED);
            session.setWinnerId(null);
            rewardForDraw(session);
            session.setCurrentTurn(null);
        } else {
            session.setCurrentTurn(actorId.equals(session.getHostId()) ? session.getGuestId() : session.getHostId());
        }

        return gameSessionRepository.save(session);
    }

    public GameSession getSession(Long sessionId) {
        return gameSessionRepository.findById(sessionId)
                .orElseThrow(() -> new GameException("Game session not found"));
    }

    private void validatePlayer(GameSession session, Integer playerId) {
        boolean inSession = playerId.equals(session.getHostId()) || playerId.equals(session.getGuestId());
        if (!inSession) {
            throw new GameException("You are not in this game session");
        }
    }

    private Integer resolveWinnerId(GameSession session, char[] board) {
        int[][] wins = {
                { 0, 1, 2 }, { 3, 4, 5 }, { 6, 7, 8 },
                { 0, 3, 6 }, { 1, 4, 7 }, { 2, 5, 8 },
                { 0, 4, 8 }, { 2, 4, 6 }
        };

        for (int[] line : wins) {
            char a = board[line[0]];
            if (a != '-' && a == board[line[1]] && a == board[line[2]]) {
                return a == 'X' ? session.getHostId() : session.getGuestId();
            }
        }
        return null;
    }

    private boolean isDraw(char[] board) {
        for (char cell : board) {
            if (cell == '-') {
                return false;
            }
        }
        return true;
    }

    private void rewardForWinLose(GameSession session, Integer winnerId) {
        Integer loserId = winnerId.equals(session.getHostId()) ? session.getGuestId() : session.getHostId();
        vptlService.addGameExp(winnerId, 100); // +10 points
        vptlService.addGameExp(loserId, 20); // +2 points
    }

    private void rewardForDraw(GameSession session) {
        vptlService.addGameExp(session.getHostId(), 50); // +5 points
        vptlService.addGameExp(session.getGuestId(), 50); // +5 points
    }

    public GameSession createSession(Integer hostId) {
        GameSession session = new GameSession();
        session.setHostId(hostId);
        session.setStatus(GameSessionStatus.WAITING);

        // Sử dụng chuỗi 9 ký tự đồng bộ với toàn bộ logic game thay vì chuỗi JSON dài dòng
        session.setBoard("---------");
        session.setUpdatedAt(java.time.LocalDateTime.now());

        return gameSessionRepository.save(session);
    }
}
