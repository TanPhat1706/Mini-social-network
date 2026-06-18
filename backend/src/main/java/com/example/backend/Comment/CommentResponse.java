package com.example.backend.Comment;

import java.time.LocalDateTime;
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
    private Long likeCount;
    private Long replyCount;
    private boolean isLikedByCurrentUser;
    private Long parentId;
}
