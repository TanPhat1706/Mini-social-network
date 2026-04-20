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
}

