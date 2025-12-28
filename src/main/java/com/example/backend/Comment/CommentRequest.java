package com.example.backend.Comment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentRequest {
    @NotBlank(message = "Nội dung không được để trống")
    private String content;

    private Long postId;
    private Long parentCommentId;
}
