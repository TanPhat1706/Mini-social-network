package com.example.backend.Post;

import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    boolean existsByPostIdAndUserId(Long postId, Long userId);

    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);

    @Query("SELECT pl.post.id FROM PostLike pl WHERE pl.user.id = :userId AND pl.post.id IN :postIds")
    Set<Long> findPostIdsLikedByUser(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);
}
