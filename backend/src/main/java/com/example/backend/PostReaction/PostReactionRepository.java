package com.example.backend.PostReaction;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.backend.Enum.ReactionType;

public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {
    @Query("SELECT r.reactionType, COUNT(r) FROM PostReaction r WHERE r.post.id = :postId GROUP BY r.reactionType")
    List<Object[]> countReactionsByPostId(@Param("postId") Long postId);

    @Query("SELECT r.post.id, r.reactionType FROM PostReaction r WHERE r.user.id = :userId AND r.post.id IN :postIds")
    List<Object[]> findReactionsByUserAndPosts(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);

    @Query("SELECT u FROM User u JOIN PostReaction r ON u.id = r.user.id WHERE r.post.id = :postId")
    Page<ReactionUserResponse> findUsersByPostId(@Param("postId") Long postId, Pageable pageable);

    @Query("SELECT new com.example.backend.PostReaction.ReactionUserResponse(u.id, u.studentCode, u.fullName, u.avatarUrl, r.reactionType) " +
            "FROM PostReaction r JOIN r.user u " +
            "WHERE r.post.id = :postId " +
            "AND (:reactionType IS NULL OR r.reactionType = :reactionType)")
    Page<ReactionUserResponse> findUsersReationByPostId(
            @Param("postId") Long postId,
            @Param("reactionType") ReactionType reactionType,
            Pageable pageable);

    Optional<PostReaction> findByPostIdAndUserId(Long postId, Long userId);

    @Modifying
    @Query("DELETE FROM PostReaction r WHERE r.post.id = :postId")
    void deleteAllByPostId(Long postId);
}
