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

       // =========================
       // FEED (CURSOR BASED)
       // =========================
       @EntityGraph(attributePaths = { "author" })
       @Query(value = "SELECT p FROM Post p WHERE p.visibility = 'PUBLIC' " +
                     "AND (:lastPostId IS NULL OR p.id < :lastPostId) " +
                     "ORDER BY p.id DESC")
       List<Post> findCursorBasedNewsFeed(@Param("lastPostId") Long lastPostId, Pageable pageable);

       // =========================
       // TÌM TẤT CẢ KÈM THEO MEDIA VÀ TÁC GIẢ (CÓ PHÂN TRANG)
       // =========================

       // 🟢 Cập nhật findAllWithAuthorAndMedia
       // Bỏ "media" khỏi EntityGraph để tránh in-memory pagination
       @EntityGraph(attributePaths = { "author" })
       @Query(value = "SELECT p FROM Post p", countQuery = "SELECT COUNT(p) FROM Post p")
       Page<Post> findAllWithAuthorAndMedia(Pageable pageable);

       // 🟢 Cập nhật findByAuthorId
       // Bỏ LEFT JOIN FETCH và ORDER BY. Việc Sort sẽ do Pageable tự động gắn vào.
       @EntityGraph(attributePaths = { "author" })
       @Query(value = "SELECT p FROM Post p WHERE p.author.id = :authorId", countQuery = "SELECT COUNT(p) FROM Post p WHERE p.author.id = :authorId")
       Page<Post> findByAuthorId(@Param("authorId") Integer authorId, Pageable pageable);

       // 🟢 Cập nhật findByAuthorStudentCode
       // Tương tự, bỏ FETCH và ORDER BY đi.
       @EntityGraph(attributePaths = { "author" })
       @Query(value = "SELECT p FROM Post p WHERE p.author.studentCode = :studentCode", countQuery = "SELECT COUNT(p) FROM Post p WHERE p.author.studentCode = :studentCode")
       Page<Post> findByAuthorStudentCode(@Param("studentCode") String studentCode, Pageable pageable);

       @EntityGraph(attributePaths = { "author" })
       @Query(value = "SELECT p FROM Post p WHERE p.author.studentCode = :studentCode AND p.visibility = 'PUBLIC'", countQuery = "SELECT COUNT(p) FROM Post p WHERE p.author.studentCode = :studentCode AND p.visibility = 'PUBLIC'")
       Page<Post> findPublicByAuthorStudentCode(@Param("studentCode") String studentCode, Pageable pageable);

       // =========================
       // OPTIMIZED MEDIA LOADING
       // =========================
       @Query("SELECT p FROM Post p LEFT JOIN FETCH p.media WHERE p.id = :id")
       Optional<Post> findByIdWithMedia(@Param("id") Long id);

       @Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.media WHERE p.id IN :ids")
       List<Post> findByIdInWithMedia(@Param("ids") List<Long> ids);

       // =========================
       // STATS
       // =========================
       @Query("SELECT COUNT(p) FROM Post p")
       long countPosts();

       // =========================
       // LIKE / COMMENT UPDATE
       // =========================
       @Modifying
       @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :postId")
       void incrementLikeCount(@Param("postId") Long postId);

       @Modifying
       @Query("UPDATE Post p SET p.likeCount = p.likeCount - 1 WHERE p.id = :postId AND p.likeCount > 0")
       void decrementLikeCount(@Param("postId") Long postId);

       @Modifying
       @Query("UPDATE Post p SET p.commentCount = p.commentCount + 1 WHERE p.id = :postId")
       void incrementCommentCount(@Param("postId") Long postId);

       @Modifying
       @Query("UPDATE Post p SET p.commentCount = p.commentCount - 1 WHERE p.id = :postId AND p.commentCount > 0")
       void decrementCommentCount(@Param("postId") Long postId);

       @Modifying
       @Query("UPDATE Post p SET p.originalPost = NULL WHERE p.originalPost.id = :postId")
       void unlinkSharedPosts(@Param("postId") Long postId);
}