package com.example.backend.Game.dto;

import lombok.Data;

@Data
public class GameMoveRequest {
    private Long sessionId;
    private Integer row;
    private Integer col;
}
