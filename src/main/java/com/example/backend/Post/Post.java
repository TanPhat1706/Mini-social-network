package com.example.backend.Post;

import java.util.*;
import org.hibernate.annotations.*;
import com.example.backend.BaseEntity.BaseEntity;
import com.example.backend.Comment.Comment;
import com.example.backend.Enum.Visibility;
import com.example.backend.PostMedia.PostMedia;
import com.example.backend.User.User;
import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "posts")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Post extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id")
    @JsonIgnoreProperties({"posts", "comments", "password", "hibernateLazyInitializer", "handler"})
    private User author;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_post_id")
    @JsonIgnoreProperties({"originalPost", "comments", "media", "hibernateLazyInitializer", "handler"})
    private Post originalPost;

    @Enumerated(EnumType.STRING)
    private Visibility visibility = Visibility.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sharer_id")
    @JsonIgnoreProperties({"posts", "comments", "password", "hibernateLazyInitializer", "handler"})
    private User sharer;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    @JsonIgnoreProperties("post")
    private List<PostMedia> media = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Comment> comments = new ArrayList<>();

    @Column(nullable = false)
    private Long likeCount = 0L;

    @Column(nullable = false)
    private Long commentCount = 0L;

    @Column(nullable = false)
    private Long shareCount = 0L;
}
