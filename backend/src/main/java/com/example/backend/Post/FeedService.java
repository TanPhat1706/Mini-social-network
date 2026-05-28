package com.example.backend.Post;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedService {
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostService postService;

    @Autowired
    public FeedService(PostRepository postRepository, 
                       PostLikeRepository postLikeRepository, 
                       PostService postService) {
        this.postRepository = postRepository;
        this.postLikeRepository = postLikeRepository;
        this.postService = postService;
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<PostResponse> getNewsFeed(Integer currentUserId, Long lastPostId, int size) {
        // Luôn luôn lấy trang 0 với kích thước size + 1 để kiểm tra xem còn bài ở phía sau không
        PageRequest pageRequest = PageRequest.of(0, size + 1); 
        
        // 1. Lấy danh sách bài viết từ DB (sử dụng Cursor)
        List<Post> posts = postRepository.findCursorBasedNewsFeed(lastPostId, pageRequest);
        
        // Xác định xem còn bài viết nào để tải tiếp không
        boolean hasNext = posts.size() > size;
        if (hasNext) {
            // Cắt bỏ phần tử dư thừa (do lấy size + 1) để trả về đúng số lượng size
            posts.remove(posts.size() - 1);
        }

        // Lấy list ID để query like 1 lần (tối ưu hiệu năng - bạn làm rất tốt phần này!)
        List<Long> postIds = posts.stream().map(Post::getId).toList();
        
        Set<Long> likedPostIds = Collections.emptySet();
        if (!postIds.isEmpty() && currentUserId != null) {
            likedPostIds = postLikeRepository.findPostIdsLikedByUser((long) currentUserId, postIds);
        }

        final Set<Long> finalLikedPostIds = likedPostIds;
        
        // 2. Map dữ liệu sang PostResponse
        List<PostResponse> responseList = posts.stream().map(post -> {
            PostResponse response = postService.mapToPostResponse(post);            
            response.setLikedByCurrentUser(finalLikedPostIds.contains(post.getId()));            
            return response;
        }).collect(Collectors.toList());

        // 3. Lấy con trỏ (ID) của bài viết cuối cùng để Front-end dùng cho lần gọi sau
        Long nextCursor = responseList.isEmpty() ? null : responseList.get(responseList.size() - 1).getId();

        return new CursorPageResponse<>(responseList, nextCursor, hasNext);
    }
}