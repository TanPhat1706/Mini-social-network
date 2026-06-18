package com.example.backend.Comment;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long>{
    Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);

    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.user.id = :userId AND cl.comment.id IN :commentIds")
    Set<Long> findCommentIdsLikedByUser(@Param("userId") Long userId, @Param("commentIds") List<Long> commentIds);
}
