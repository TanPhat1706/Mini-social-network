package com.example.backend.Post;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

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
        if (!postIds.isEmpty() && currentUserId != null) {
            likedPostIds = postLikeRepository.findPostIdsLikedByUser((long) currentUserId, postIds);
        }

        final Set<Long> finalLikedPostIds = likedPostIds;
        
        // 2. Map dữ liệu sang PostResponse
        List<PostResponse> responseList = posts.stream().map(post -> {
            PostResponse response = postService.mapToPostResponse(post);            
        Long nextCursor = responseList.isEmpty() ? null : responseList.get(responseList.size() - 1).getId();

        return new CursorPageResponse<>(responseList, nextCursor, hasNext);
    }
}