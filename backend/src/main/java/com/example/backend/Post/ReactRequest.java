package com.example.backend.Post;

import com.example.backend.Enum.ReactionType;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor @AllArgsConstructor
public class ReactRequest {
    @NotNull(message = "Reaction type is required")
    private ReactionType reactionType;
}
