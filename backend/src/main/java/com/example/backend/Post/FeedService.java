package com.example.backend.Post;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedService {
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostService postService;

    @Transactional(readOnly = true)
    public Page<PostResponse> getNewsFeed(Long currentUserId, Pageable pageable) {
        // 1. Lấy danh sách bài viết từ DB (Chỉ lấy Post + Author, không fetch Collection để an toàn phân trang)
        Page<Post> postPage = postRepository.findAllForNewsFeed(pageable);
        
        if (postPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Post> posts = postPage.getContent();
        List<Long> postIds = posts.stream().map(Post::getId).toList();
        
        // ⭐️ KỸ THUẬT PRE-WARM CACHE: Gom các bài gốc (Original Post) lại để fetch một lần
        List<Long> originalPostIds = posts.stream()
                .map(Post::getOriginalPost)
                .filter(op -> op != null && op.getId() != null)
                .map(Post::getId)
                .toList();
        
        // Bắn 1 câu query để lôi toàn bộ Original Post + Media lên RAM (Session L1 Cache của Hibernate)
        if (!originalPostIds.isEmpty()) {
            postRepository.findByIdInWithMedia(originalPostIds);  
        }
        
        // 2. Lấy danh sách ID các bài viết user hiện tại đã like bằng 1 câu query
        Set<Long> likedPostIds = Collections.emptySet();
        if (!postIds.isEmpty()) {
            likedPostIds = postLikeRepository.findPostIdsLikedByUser(currentUserId, postIds);
        }

        final Set<Long> finalLikedPostIds = likedPostIds;
        
        // 3. Map sang DTO (Lúc này getOriginalPost hay getMedia sẽ không sinh ra query con nữa)
        return postPage.map(post -> {
            PostResponse response = postService.mapToPostResponse(post, false);            
            response.setLikedByCurrentUser(finalLikedPostIds.contains(post.getId()));            
            return response;
        });
    }
}