package com.example.backend.PostMedia;

import com.example.backend.Enum.MediaType;
import com.example.backend.Post.Post;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "post_media")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PostMedia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(nullable = false, length = 2000)
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    private MediaType mediaType = MediaType.IMAGE;

    private int orderIndex = 0;
}
