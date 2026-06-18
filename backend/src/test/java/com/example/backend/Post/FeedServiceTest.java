package com.example.backend.Post;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.example.backend.Enum.ReactionType;
import com.example.backend.PostReaction.PostReactionRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock
    private PostRepository postRepository;

    // 🟢 FIXED: Đổi mock từ PostLikeRepository sang PostReactionRepository
    @Mock
    private PostReactionRepository postReactionRepository;

    @Mock
    private PostService postService;

    @InjectMocks
    private FeedService feedService;

    // ==========================================
    // 1. KỊCH BẢN: BẢNG TIN TRỐNG (KHÔNG CÓ BÀI VIẾT)
    // ==========================================
    @Test
    void getNewsFeed_whenNoPosts_shouldNotQueryLikes_andReturnEmptyCursor() {
        // Cấu hình: size = 10, service sẽ query size = 11
        when(postRepository.findCursorBasedNewsFeed(eq(null), any(PageRequest.class)))
                .thenReturn(new ArrayList<>()); // Trả về list rỗng

        // Thực thi
        CursorPageResponse<PostResponse> result = feedService.getNewsFeed(1, null, 10);

        // Kiểm chứng
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertFalse(result.isHasNext());
        assertNull(result.getNextCursor());
        
        // 🟢 FIXED: Đảm bảo không gọi thừa các hàm query phụ của PostReactionRepository
        verify(postReactionRepository, never()).findReactionsByUserAndPosts(anyLong(), anyList());
        verify(postRepository, never()).findByIdInWithMedia(anyList());
        verify(postService, never()).mapToPostResponse(any());
    }

    // ==========================================
    // 2. KỊCH BẢN: CÓ BÀI VIẾT, CÓ REACTION VÀ CÓ PRE-WARM CACHE (ORIGINAL POST)
    // ==========================================
    @Test
    void getNewsFeed_shouldSetReaction_andHandlePreWarmCache() {
        // Chuẩn bị dữ liệu giả lập
        Post originalPost = Post.builder().id(99L).build(); // Bài gốc dùng để test Pre-warm cache
        Post p1 = Post.builder().id(10L).originalPost(originalPost).build(); 
        Post p2 = Post.builder().id(20L).build();

        // Bắt buộc dùng new ArrayList<>() để tránh lỗi UnsupportedOperationException khi Service gọi remove()
        List<Post> mockPosts = new ArrayList<>(List.of(p1, p2));
        
        when(postRepository.findCursorBasedNewsFeed(eq(null), any(PageRequest.class)))
                .thenReturn(mockPosts);

        // 🟢 FIXED: Giả lập User ID 1 đã thả LIKE bài 20L (trả về mảng Object[] theo thiết kế Query)
        // 🟢 FIXED: Dùng Collections.singletonList để ép kiểu chính xác thành List<Object[]>
        // Tránh lỗi suy luận kiểu varargs của List.of()
        when(postReactionRepository.findReactionsByUserAndPosts(1L, List.of(10L, 20L)))
                .thenReturn(Collections.singletonList(new Object[]{20L, ReactionType.LIKE}));

        // Giả lập PostService map sang Response
        PostResponse r1 = PostResponse.builder().id(10L).build();
        PostResponse r2 = PostResponse.builder().id(20L).build();
        when(postService.mapToPostResponse(p1)).thenReturn(r1);
        when(postService.mapToPostResponse(p2)).thenReturn(r2);

        // Thực thi
        CursorPageResponse<PostResponse> result = feedService.getNewsFeed(1, null, 10);

        // Kiểm chứng kết quả
        assertEquals(2, result.getContent().size());
        
        // 🟢 FIXED: Kiểm chứng dựa trên currentUserReaction thay vì isLikedByCurrentUser
        assertNull(result.getContent().get(0).getCurrentUserReaction(), "Bài 10L chưa thả cảm xúc");
        assertEquals("LIKE", result.getContent().get(1).getCurrentUserReaction(), "Bài 20L đã thả LIKE");
        
        assertEquals(20L, result.getNextCursor(), "Con trỏ tiếp theo phải là ID bài cuối cùng");
        assertFalse(result.isHasNext(), "Chỉ có 2 bài, size = 10 -> Không còn bài tiếp theo");

        // Đảm bảo kỹ thuật Pre-warm Cache đã được kích hoạt cho bài gốc (ID 99L)
        verify(postRepository).findByIdInWithMedia(List.of(99L));
    }

    // ==========================================
    // 3. KỊCH BẢN: KIỂM TRA LOGIC CHẶT CỤT (HAS_NEXT = TRUE)
    // ==========================================
    @Test
    void getNewsFeed_whenHasNext_shouldTruncateListAndSetHasNextTrue() {
        // Chuẩn bị 3 bài viết. Nhưng chúng ta giả lập Frontend chỉ gọi size = 2
        Post p1 = Post.builder().id(30L).build();
        Post p2 = Post.builder().id(20L).build();
        Post p3 = Post.builder().id(10L).build(); // Bài này dư ra dùng để xác định hasNext

        // Trả về Mutable List
        when(postRepository.findCursorBasedNewsFeed(eq(50L), any(PageRequest.class)))
                .thenReturn(new ArrayList<>(List.of(p1, p2, p3)));

        // 🟢 FIXED: User chưa thả cảm xúc bài nào (trả về List rỗng)
        when(postReactionRepository.findReactionsByUserAndPosts(eq(1L), anyList()))
                .thenReturn(Collections.emptyList());

        PostResponse r1 = PostResponse.builder().id(30L).build();
        PostResponse r2 = PostResponse.builder().id(20L).build();
        // Không mock r3 vì nó sẽ bị Service remove() trước khi vào vòng lặp map
        when(postService.mapToPostResponse(p1)).thenReturn(r1);
        when(postService.mapToPostResponse(p2)).thenReturn(r2);

        // Thực thi: currentUserId = 1, lastPostId = 50L, size = 2
        CursorPageResponse<PostResponse> result = feedService.getNewsFeed(1, 50L, 2);

        // Kiểm chứng
        assertEquals(2, result.getContent().size(), "Danh sách phải bị cắt từ 3 xuống 2");
        assertTrue(result.isHasNext(), "Do query ra 3 bài (lớn hơn size 2) nên hasNext phải là true");
        assertEquals(20L, result.getNextCursor(), "Con trỏ phải trỏ vào phần tử thứ 2 (ID 20L), không phải ID 10L");
        
        // Đảm bảo r3 không bao giờ được đưa vào map
        verify(postService, never()).mapToPostResponse(p3);
    }
}