package com.example.backend.Comment;

import java.time.LocalDateTime;
import java.util.Map;

import com.example.backend.Enum.ReactionType;
import com.example.backend.User.UserResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommentResponse {
    private Long id;
    private String content;
    private UserResponse author;
    private LocalDateTime createdAt;
    private Long reactionCount;
    private Map<String, Long> reactionCounts;
    private Long replyCount;
    private ReactionType currentUserReaction;
    private Long parentId;
}
