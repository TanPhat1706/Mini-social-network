package com.example.backend.Game.dto;

import com.example.backend.Enum.GameSessionStatus;
import com.example.backend.Game.GameSession;
import lombok.Data;

@Data
public class GameSessionPayload {
    private Long id;
    private Integer hostId;
    private Integer guestId;
    private String[][] board; 
    private Integer currentTurnId; 
    private GameSessionStatus status;
    private Integer winnerId;

    public static GameSessionPayload from(GameSession session) {
        if (session == null) return null;

        GameSessionPayload payload = new GameSessionPayload();
        payload.setId(session.getId());
        payload.setHostId(session.getHostId());
        payload.setGuestId(session.getGuestId());
        payload.setStatus(session.getStatus());
        payload.setWinnerId(session.getWinnerId());
        payload.setCurrentTurnId(session.getCurrentTurn());

        // 🔴 ĐÃ SỬA: Cắt chuỗi 9 ký tự thành mảng 2 chiều 3x3 cho React
        String boardStr = session.getBoard();
        String[][] boardArray = new String[3][3];
        
        if (boardStr != null && boardStr.length() == 9) {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    // Lấy từng ký tự X, O hoặc - nhét vào mảng
                    boardArray[i][j] = String.valueOf(boardStr.charAt(i * 3 + j));
                }
            }
        } else {
            // Fallback an toàn nếu DB bị rỗng
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    boardArray[i][j] = "-";
                }
            }
        }
        
        payload.setBoard(boardArray);

        return payload;
    }
}