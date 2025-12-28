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
    public Page<PostResponse> getNewsFeed(Integer currentUserId, Pageable pageable) {
        // Lấy danh sách bài viết từ DB
        Page<Post> postPage = postRepository.findAllForNewsFeed(pageable);
        
        // Lấy list ID để query like 1 lần (tối ưu hiệu năng)
        List<Long> postIds = postPage.getContent().stream().map(Post::getId).toList();
        
        Set<Long> likedPostIds = Collections.emptySet();
        if (!postIds.isEmpty()) {
            likedPostIds = postLikeRepository.findPostIdsLikedByUser((long) currentUserId, postIds);
        }

        final Set<Long> finalLikedPostIds = likedPostIds;
        
        // 2. Map dữ liệu
        return postPage.map(post -> {
            PostResponse response = postService.mapToPostResponse(post);            
            response.setLikedByCurrentUser(finalLikedPostIds.contains(post.getId()));            
            return response;
        });
    }
}
