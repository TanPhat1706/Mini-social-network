package com.example.backend.Game.dto;

import com.example.backend.Enum.GameEventType;
import lombok.Data;

@Data
public class GameWsEvent {
    private GameEventType type;
    private String message;
    private Integer actorId;
    private Integer row;
    private Integer col;
    private GameSessionPayload session;
}
