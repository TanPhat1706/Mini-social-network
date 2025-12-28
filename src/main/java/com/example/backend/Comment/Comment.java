package com.example.backend.Comment;

import java.util.ArrayList;
import java.util.List;
import com.example.backend.BaseEntity.BaseEntity;
import com.example.backend.Post.Post;
import com.example.backend.User.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "comments")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Comment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id")
    @JsonIgnore
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User author;

    @ManyToOne
    @JoinColumn(name = "parent_comment_id")
    @JsonIgnore
    private Comment parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Comment> replies = new ArrayList<>();

    @Column(columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String content;

    @Column(nullable = false)
    private Long likeCount = 0L;

    @Column(nullable = false)
    private Long replyCount = 0L;
}
