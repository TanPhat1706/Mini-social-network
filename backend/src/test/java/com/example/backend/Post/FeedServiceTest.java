package com.example.backend.Post;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Set;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostService postService;

    @InjectMocks
    private FeedService feedService;

    @Test
    void getNewsFeed_whenNoPosts_shouldNotQueryLikes_andReturnEmptyPage() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(postRepository.findAllForNewsFeed(pageable)).thenReturn(Page.empty(pageable));

        Page<PostResponse> result = feedService.getNewsFeed(1, pageable);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        verify(postLikeRepository, never()).findPostIdsLikedByUser(anyLong(), anyList());
        verify(postService, never()).mapToPostResponse(any());
    }

    @Test
    void getNewsFeed_shouldSetLikedByCurrentUser_basedOnLikeSet() {
        PageRequest pageable = PageRequest.of(0, 10);

        Post p1 = Post.builder().id(10L).build();
        Post p2 = Post.builder().id(20L).build();
        Page<Post> postPage = new PageImpl<>(List.of(p1, p2), pageable, 2);
        when(postRepository.findAllForNewsFeed(pageable)).thenReturn(postPage);

        when(postLikeRepository.findPostIdsLikedByUser(1L, List.of(10L, 20L)))
                .thenReturn(Set.of(20L));

        PostResponse r1 = PostResponse.builder().id(10L).build();
        PostResponse r2 = PostResponse.builder().id(20L).build();
        when(postService.mapToPostResponse(p1)).thenReturn(r1);
        when(postService.mapToPostResponse(p2)).thenReturn(r2);

        Page<PostResponse> result = feedService.getNewsFeed(1, pageable);

        assertEquals(2, result.getContent().size());
        assertFalse(result.getContent().get(0).isLikedByCurrentUser());
        assertTrue(result.getContent().get(1).isLikedByCurrentUser());
    }
    @Test
    void getNewsFeed_whenPostsExistButUserHasNotLikedAny_shouldSetAllLikedToFalse() {
        // Chuẩn bị Page Request
        PageRequest pageable = PageRequest.of(0, 10);

        // Giả lập Repository trả về 2 bài viết
        Post p1 = Post.builder().id(30L).build();
        Post p2 = Post.builder().id(40L).build();
        Page<Post> postPage = new PageImpl<>(List.of(p1, p2), pageable, 2);
        
        when(postRepository.findAllForNewsFeed(pageable)).thenReturn(postPage);

        // KỊCH BẢN BIÊN: Repository xác nhận User chưa like bài nào trong list này (trả về Set rỗng)
        when(postLikeRepository.findPostIdsLikedByUser(1L, List.of(30L, 40L)))
                .thenReturn(Collections.emptySet());

        // Giả lập hàm map của PostService
        PostResponse r1 = PostResponse.builder().id(30L).build();
        PostResponse r2 = PostResponse.builder().id(40L).build();
        when(postService.mapToPostResponse(p1)).thenReturn(r1);
        when(postService.mapToPostResponse(p2)).thenReturn(r2);

        // Thực thi
        Page<PostResponse> result = feedService.getNewsFeed(1, pageable);

        // Kiểm chứng kết quả
        assertEquals(2, result.getContent().size());
        
        // Đảm bảo tuyệt đối không bài nào bị dính cờ isLikedByCurrentUser = true
        assertFalse(result.getContent().get(0).isLikedByCurrentUser());
        assertFalse(result.getContent().get(1).isLikedByCurrentUser());
    }
}

