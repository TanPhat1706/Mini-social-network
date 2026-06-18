package com.example.backend.Post;

import com.example.backend.Enum.Visibility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostDto {
    private Long id;
    private Long authorId;
    private String authorUsername;
    private String content;
    private Visibility visibility;
    private Long likeCount;
    private Long commentCount;
    private Long shareCount;
    // 🟢 BỔ SUNG 2 TRƯỜNG NÀY ĐỂ PHỦ SÓNG MA THUẬT CSS
    private String authorAvatarFrame;
    private String authorNameColor;
}
