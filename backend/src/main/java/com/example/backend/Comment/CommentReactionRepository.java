package com.example.backend.Comment;

import com.example.backend.Enum.ReactionType;
import com.example.backend.PostReaction.ReactionUserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {
    Optional<CommentReaction> findByCommentIdAndUserId(Long commentId, Long userId);

    @Query("SELECT r FROM CommentReaction r WHERE r.user.id = :userId AND r.comment.id IN :commentIds")
    List<CommentReaction> findReactionsByUserIdAndCommentIds(@Param("userId") Long userId, @Param("commentIds") List<Long> commentIds);

    // 🟢 Sửa lại tên hàm cho chuẩn với Comment
    @Query("SELECT new com.example.backend.PostReaction.ReactionUserResponse(u.id, u.studentCode, u.fullName, u.avatarUrl, r.reactionType) " +
           "FROM CommentReaction r JOIN r.user u WHERE r.comment.id = :commentId AND (:type IS NULL OR r.reactionType = :type)")
    Page<ReactionUserResponse> findUsersReactionByCommentId(@Param("commentId") Long commentId, @Param("type") ReactionType type, Pageable pageable);

    // ==========================================
    // 🟢 QUERY TỐI ƯU ĐỂ ĐẾM SỐ LƯỢNG TỪNG LOẠI
    // ==========================================
    
    // 1. Đếm cho nhiều comment cùng lúc (Tránh N+1 khi get danh sách)
    @Query("SELECT r.comment.id as commentId, r.reactionType as reactionType, COUNT(r) as count " +
           "FROM CommentReaction r WHERE r.comment.id IN :commentIds GROUP BY r.comment.id, r.reactionType")
    List<ReactionCountProjection> countReactionsByCommentIds(@Param("commentIds") List<Long> commentIds);

    // 2. Đếm cho 1 comment (Dùng khi create/update)
    @Query("SELECT r.reactionType as reactionType, COUNT(r) as count " +
           "FROM CommentReaction r WHERE r.comment.id = :commentId GROUP BY r.reactionType")
    List<SingleReactionCountProjection> countReactionsByCommentId(@Param("commentId") Long commentId);

    interface ReactionCountProjection {
        Long getCommentId();
        ReactionType getReactionType();
        Long getCount();
    }

    interface SingleReactionCountProjection {
        ReactionType getReactionType();
        Long getCount();
    }
}