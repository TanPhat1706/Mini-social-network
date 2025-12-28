package com.example.backend.Post;

import java.time.LocalDateTime;
import java.util.List;
import com.example.backend.Enum.Visibility;
import com.example.backend.PostMedia.MediaResponse;
import com.example.backend.User.UserResponse;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostResponse {
    private Long id;
    private String content;
    private Visibility visibility;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UserResponse author;
    private List<MediaResponse> media;
    private Long likeCount;
    private Long commentCount;
    private Long shareCount;
    private boolean isLikedByCurrentUser;
    private PostResponse originalPost;
}
