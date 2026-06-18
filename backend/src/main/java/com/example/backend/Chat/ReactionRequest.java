package com.example.backend.Chat;
import com.example.backend.Enum.ReactionType;
import lombok.Data;

@Data
public class ReactionRequest {
    private Long messageId;
    private Integer userId; // Người thả cảm xúc
    private ReactionType reactionType;
}