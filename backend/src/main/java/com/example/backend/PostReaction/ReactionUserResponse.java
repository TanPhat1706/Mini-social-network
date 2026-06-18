package com.example.backend.PostReaction;

import com.example.backend.Enum.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionUserResponse {
    private Long id;
    private String studentCode;
    private String fullName;
    private String avatarUrl;
    private ReactionType reactionType;

    public ReactionUserResponse(Integer id, String studentCode, String fullName, String avatarUrl, ReactionType reactionType) {
        this.id = id != null ? id.longValue() : null;
        this.studentCode = studentCode;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.reactionType = reactionType;
    }
}