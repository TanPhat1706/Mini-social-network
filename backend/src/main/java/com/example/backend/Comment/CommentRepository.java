package com.example.backend.Comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.parent IS NULL ORDER BY c.createdAt DESC")
    Page<Comment> findRootCommentsByPostId(@Param("postId") Long postId, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.parent.id = :parentId ORDER BY c.createdAt ASC")
    Page<Comment> findRepliesByParentId(@Param("parentId") Long parentId, Pageable pageable);

    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :id")
    void incrementLikeCount(Long id);

    @Modifying
    @Query("UPDATE Comment c SET c.replyCount = c.replyCount + 1 WHERE c.id = :id")
    void incrementReplyCount(Long id);

    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount - 1 WHERE c.id = :id AND c.likeCount > 0")
    void decrementLikeCount(Long id);

    @Modifying
    @Query("UPDATE Comment c SET c.replyCount = c.replyCount - 1 WHERE c.id = :id AND c.replyCount > 0")
    void decrementReplyCount(Long id);
}
