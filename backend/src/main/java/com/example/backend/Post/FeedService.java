package com.example.backend.Post;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.Enum.ReactionType;
import com.example.backend.PostReaction.PostReactionRepository;

@Service
public class FeedService {
    private final PostRepository postRepository;
    private final PostReactionRepository postReactionRepository;
    private final PostService postService;

    @Autowired
    public FeedService(PostRepository postRepository, 
                       PostReactionRepository postReactionRepository, 
                       PostService postService) {
        this.postRepository = postRepository;
        this.postReactionRepository = postReactionRepository;
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
        
        // 2. Lấy map cảm xúc của User hiện tại bằng Batch Query
        Map<Long, String> userReactionsMap = new HashMap<>();
        if (!postIds.isEmpty() && currentUserId != null) {
            List<Object[]> reactions = postReactionRepository.findReactionsByUserAndPosts((long) currentUserId, postIds);
            
            for (Object[] row : reactions) {
                Long postId = (Long) row[0];
                ReactionType reactionType = (ReactionType) row[1];
                userReactionsMap.put(postId, reactionType.name());
            }
        }
        
        // 3. Map dữ liệu sang PostResponse (Đã sửa lỗi cú pháp ở đây)
        List<PostResponse> responseList = posts.stream().map(post -> {
            PostResponse response = postService.mapToPostResponse(post);            
            response.setCurrentUserReaction(userReactionsMap.get(post.getId()));
            return response;
        }).collect(Collectors.toList());

        // 4. Lấy con trỏ (ID) của bài viết cuối cùng để Front-end dùng cho lần gọi sau
        Long nextCursor = responseList.isEmpty() ? null : responseList.get(responseList.size() - 1).getId();

        return new CursorPageResponse<>(responseList, nextCursor, hasNext);
    }
}