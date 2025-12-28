package com.example.backend.Post;

import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    @Query(value = "SELECT p FROM Post p JOIN FETCH p.author u WHERE p.visibility = 'PUBLIC'",
        countQuery = "SELECT COUNT(p) FROM Post p WHERE p.visibility = 'PUBLIC'")
    Page<Post> findAllForNewsFeed(Pageable pageable);
    
    @Query("SELECT COUNT(p) FROM Post p")
    long countPosts();

    @Query("SELECT DISTINCT p FROM Post p JOIN FETCH p.author LEFT JOIN FETCH p.media ORDER BY p.id DESC")
    List<Post> findAllWithAuthorAndMedia();

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :postId")
    void incrementLikeCount(Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount - 1 WHERE p.id = :postId AND p.likeCount > 0")
    void decrementLikeCount(Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + 1 WHERE p.id = :postId")
    void incrementCommentCount(Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount - 1 WHERE p.id = :postId AND p.commentCount > 0")
    void decrementCommentCount(Long postId);

    Page<Post> findByAuthorId(Integer authorId, Pageable pageable);
}
