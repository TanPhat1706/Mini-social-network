package com.example.backend.Post;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    @EntityGraph(attributePaths = {"author"})
    // @Query(value = "SELECT p FROM Post p JOIN FETCH p.author u WHERE p.visibility = 'PUBLIC'", countQuery = "SELECT COUNT(p) FROM Post p WHERE p.visibility = 'PUBLIC'")
    @Query(value = "SELECT p FROM Post p WHERE p.visibility = 'PUBLIC'", countQuery = "SELECT COUNT(p) FROM Post p WHERE p.visibility = 'PUBLIC'")
    Page<Post> findAllForNewsFeed(Pageable pageable);

    @Query("SELECT COUNT(p) FROM Post p")
    long countPosts();

    // @Query("SELECT DISTINCT p FROM Post p JOIN FETCH p.author LEFT JOIN FETCH p.media ORDER BY p.id DESC")
    // List<Post> findAllWithAuthorAndMedia();

    @EntityGraph(attributePaths = {"author"})
    @Query(value = "SELECT p FROM Post p ORDER BY p.createdAt DESC", countQuery = "SELECT COUNT(p) FROM Post p")
    Page<Post> findAllWithAuthorAndMedia(Pageable pageable);
    

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

    // Page<Post> findByAuthorId(Integer authorId, Pageable pageable);

    @Query(value = "SELECT p FROM Post p LEFT JOIN FETCH p.media WHERE p.author.id = :authorId", countQuery = "SELECT COUNT(DISTINCT p) FROM Post p WHERE p.author.id = :authorId")
    Page<Post> findByAuthorId(@Param("authorId") Integer authorId, Pageable pageable);

    // Page<Post> findByAuthorStudentCode(String studentCode, Pageable pageable);

    @Query(value = "SELECT p FROM Post p LEFT JOIN FETCH p.media WHERE p.author.studentCode = :studentCode", countQuery = "SELECT COUNT(DISTINCT p) FROM Post p WHERE p.author.studentCode = :studentCode")
    Page<Post> findByAuthorStudentCode(@Param("studentCode") String studentCode, Pageable pageable);

    // 🟢 NEW: Helper method to load original post with media
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.media WHERE p.id = :id")
    Optional<Post> findByIdWithMedia(@Param("id") Long id);

    // 🟢 NEW: Find posts by ID list with eager loading (for batch operations)
    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.media WHERE p.id IN :ids")
    List<Post> findByIdInWithMedia(@Param("ids") List<Long> ids);
}