package com.example.backend.Post;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    private Map<String, Integer> reactionCounts;
    private Long commentCount;
    private Long shareCount;
    private String currentUserReaction;
    private boolean isSelfPost;
    private PostResponse originalPost;

    // Tui khuyên bạn nên làm luôn cho isSelfPost để phòng hờ lỗi tương tự
    public void setSelfPost(boolean selfPost) {
        this.isSelfPost = selfPost;
    }
}
