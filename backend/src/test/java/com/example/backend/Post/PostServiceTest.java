package com.example.backend.Post;

import com.example.backend.Enum.Visibility;
import com.example.backend.Event.NotificationEvent;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import com.example.backend.VPTLpoint.VptlService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private ApplicationEventPublisher evenPublisher;

    @Mock
    private VptlService vptlService;

    @InjectMocks
    private PostService postService;

    private User currentUser;

    @org.junit.jupiter.api.io.TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(postService, "uploadDir", tempDir.toString());
        currentUser = new User();
        currentUser.setId(1);
        currentUser.setStudentCode("SV001");
        currentUser.setRole("STUDENT");
        currentUser.setFullName("User 1");

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("SV001", null));
        // Không phải test nào cũng gọi getCurrentUser(), nên cần lenient để tránh UnnecessaryStubbingException
        lenient().when(userRepository.findByStudentCode("SV001")).thenReturn(Optional.of(currentUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPost_whenNoContentAndNoMedia_shouldThrowBadRequest() {
        PostRequest req = new PostRequest();
        req.setContent("   ");
        req.setVisibility(Visibility.PRIVATE);
        req.setMediaFiles(List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> postService.createPost(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Bài viết phải có nội dung hoặc hình ảnh/video.", ex.getReason());
        verify(postRepository, never()).save(any());
    }

    @Test
    void createPost_whenVisibilityPublic_shouldForcePending_andNotTrackPublicActivity() {
        PostRequest req = new PostRequest();
        req.setContent(" hello ");
        req.setVisibility(Visibility.PUBLIC);

        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        PostResponse res = postService.createPost(req);

        assertNotNull(res);
        assertEquals("hello", res.getContent());
        assertEquals(Visibility.PENDING, res.getVisibility());
        verify(vptlService, never()).trackSocialActivity(anyInt(), anyString());
    }

    @Test
    void createPost_withMedia_shouldStoreFiles_andAttachMediaToPost() {
        PostRequest req = new PostRequest();
        req.setContent(""); // media-only is allowed
        req.setVisibility(Visibility.PRIVATE);
        MockMultipartFile file = new MockMultipartFile(
                "mediaFiles", "a.png", "image/png", "x".getBytes(StandardCharsets.UTF_8)
        );
        req.setMediaFiles(List.of(file));

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        when(postRepository.save(captor.capture())).thenAnswer(inv -> {
            Post p = inv.getArgument(0);
            p.setId(99L);
            return p;
        });

        PostResponse res = postService.createPost(req);

        Post saved = captor.getValue();
        assertEquals(Visibility.PRIVATE, saved.getVisibility());
        assertNotNull(saved.getMedia());
        assertEquals(1, saved.getMedia().size());
        assertNotNull(saved.getMedia().get(0).getMediaUrl());
        assertTrue(saved.getMedia().get(0).getMediaUrl().startsWith("/uploads/"));
        assertEquals(99L, res.getId());
    }

    @Test
    void updatePost_whenNotAuthor_shouldThrow() {
        User other = new User();
        other.setId(2);
        Post post = Post.builder().id(10L).author(other).media(new java.util.ArrayList<>()).build();

        when(postRepository.findById(10L)).thenReturn(Optional.of(post));

        PostRequest req = new PostRequest();
        req.setContent("x");
        req.setVisibility(Visibility.PRIVATE);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> postService.updatePost(10L, req));
        assertEquals("Bạn không có quyền chỉnh sửa bài viết này!", ex.getMessage());
        verify(postRepository, never()).save(any());
    }

    @Test
    void updatePost_whenVisibilityPublic_shouldForcePending_andReplaceMedia() {
        Post post = Post.builder()
                .id(10L)
                .author(currentUser)
                .media(new java.util.ArrayList<>())
                .build();
        post.setVisibility(Visibility.PRIVATE);

        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        PostRequest req = new PostRequest();
        req.setContent("  new  ");
        req.setVisibility(Visibility.PUBLIC);
        req.setMediaFiles(List.of(new MockMultipartFile(
                "mediaFiles", "a.png", "image/png", "x".getBytes(StandardCharsets.UTF_8)
        )));

        PostResponse res = postService.updatePost(10L, req);

        assertEquals("new", res.getContent());
        assertEquals(Visibility.PENDING, res.getVisibility());
        assertEquals(1, post.getMedia().size());
        assertTrue(post.getMedia().get(0).getMediaUrl().startsWith("/uploads/"));
    }

    @Test
    void deletePost_whenNotAuthorAndNotAdmin_shouldThrow() {
        User other = new User();
        other.setId(2);
        Post post = Post.builder().id(10L).author(other).build();

        when(postRepository.findById(10L)).thenReturn(Optional.of(post));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> postService.deletePost(10L));
        assertEquals("Bạn không có quyền xóa bài viết này!", ex.getMessage());
        verify(postRepository, never()).delete(any());
    }

    @Test
    void deletePost_whenAdmin_shouldDelete() {
        currentUser.setRole("ADMIN");
        when(userRepository.findByStudentCode("SV001")).thenReturn(Optional.of(currentUser));

        User other = new User();
        other.setId(2);
        Post post = Post.builder().id(10L).author(other).build();
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));

        String msg = postService.deletePost(10L);
        assertEquals("Xóa bài viết thành công", msg);
        verify(postRepository).delete(post);
    }

    @Test
    void approvePost_whenWasPending_shouldTrackActivity() {
        User author = new User();
        author.setId(5);
        Post post = Post.builder().id(10L).author(author).visibility(Visibility.PENDING).build();
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        postService.approvePost(10L);

        assertEquals(Visibility.PUBLIC, post.getVisibility());
        verify(vptlService).trackSocialActivity(5, "POST");
    }

    @Test
    void approvePost_whenNotPending_shouldNotDoubleTrack() {
        User author = new User();
        author.setId(5);
        Post post = Post.builder().id(10L).author(author).visibility(Visibility.PUBLIC).build();
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        postService.approvePost(10L);

        verify(vptlService, never()).trackSocialActivity(anyInt(), eq("POST"));
    }

    @Test
    void toggleLike_whenAlreadyLiked_shouldDeleteLike_andDecrement() {
        PostLike like = PostLike.builder().id(1L).build();
        when(postLikeRepository.findByPostIdAndUserId(10L, 1L)).thenReturn(Optional.of(like));

        postService.toggleLike(10L);

        verify(postLikeRepository).delete(like);
        verify(postRepository).decrementLikeCount(10L);
        verify(postRepository, never()).incrementLikeCount(anyLong());
        verify(evenPublisher, never()).publishEvent(any());
    }

    @Test
    void toggleLike_whenNotLiked_shouldSaveLike_increment_track_andPublishNotification() {
        when(postLikeRepository.findByPostIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        User author = new User();
        author.setId(2);
        Post post = Post.builder().id(10L).author(author).build();
        when(postRepository.getReferenceById(10L)).thenReturn(post);

        postService.toggleLike(10L);

        verify(postLikeRepository).save(any(PostLike.class));
        verify(postRepository).incrementLikeCount(10L);
        verify(vptlService).trackSocialActivity(1, "LIKE");

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(evenPublisher).publishEvent(captor.capture());
        assertTrue(captor.getValue() instanceof NotificationEvent);
    }

    @Test
    void sharePost_whenRootPrivate_shouldThrow() {
        Post root = Post.builder().id(10L).visibility(Visibility.PRIVATE).build();
        when(postRepository.findById(10L)).thenReturn(Optional.of(root));

        PostRequest req = new PostRequest();
        req.setContent("share");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> postService.sharePost(10L, req));
        assertEquals("Không thể chia sẻ bài viết riêng tư", ex.getMessage());
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void sharePost_shouldIncrementRootShareCount_saveRootAndShare_track_andPublish() {
        User author = new User();
        author.setId(2);

        Post root = Post.builder()
                .id(10L)
                .author(author)
                .visibility(Visibility.PUBLIC)
                .shareCount(0L)
                .media(new java.util.ArrayList<>())
                .build();
        when(postRepository.findById(10L)).thenReturn(Optional.of(root));

        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        PostRequest req = new PostRequest();
        req.setContent(" share ");
        req.setVisibility(Visibility.PUBLIC);

        PostResponse res = postService.sharePost(10L, req);

        assertNotNull(res);
        assertEquals(1L, root.getShareCount());
        verify(vptlService).trackSocialActivity(1, "SHARE");
        verify(evenPublisher).publishEvent(any(NotificationEvent.class));
    }

    @Test
    void getPostsByAuthor_shouldUseCurrentUserId() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 2);
        org.springframework.data.domain.Page<Post> page = new org.springframework.data.domain.PageImpl<>(List.of(
                Post.builder().id(1L).author(currentUser).media(new java.util.ArrayList<>()).build()
        ), pageable, 1);
        when(postRepository.findByAuthorId(eq(1), any())).thenReturn(page);

        var result = postService.getPostsByAuthor(0, 2);

        assertEquals(1, result.getTotalElements());
        verify(postRepository).findByAuthorId(eq(1), any());
    }
    // ==========================================
    // BỔ SUNG CÁC TEST CASE CÒN THIẾU
    // ==========================================

    @Test
    void getAllPostsForAdmin_shouldReturnMappedList() {
        Post p1 = Post.builder().id(1L).author(currentUser).media(new java.util.ArrayList<>()).build();
        Post p2 = Post.builder().id(2L).author(currentUser).media(new java.util.ArrayList<>()).build();
        
        when(postRepository.findAllWithAuthorAndMedia()).thenReturn(List.of(p1, p2));

        List<PostResponse> result = postService.getAllPostsForAdmin();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
        verify(postRepository).findAllWithAuthorAndMedia();
    }

    @Test
    void deletePost_whenIsAuthor_shouldDeleteSuccessfully() {
        // Test luồng tác giả tự xóa bài của mình
        Post post = Post.builder().id(10L).author(currentUser).build();
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));

        String msg = postService.deletePost(10L);

        assertEquals("Xóa bài viết thành công", msg);
        verify(postRepository).delete(post);
    }

    @Test
    void sharePost_whenSharingASharedPost_shouldMapToRootPost() {
        // Giả lập Root Post (Bài gốc)
        User rootAuthor = new User();
        rootAuthor.setId(2);
        Post rootPost = Post.builder()
                .id(5L)
                .author(rootAuthor)
                .visibility(Visibility.PUBLIC)
                .shareCount(10L)
                .media(new java.util.ArrayList<>())
                .build();

        // Giả lập Shared Post (Bài đã được share từ Root Post)
        User sharer = new User();
        sharer.setId(3);
        Post sharedPost = Post.builder()
                .id(10L)
                .author(sharer)
                .visibility(Visibility.PUBLIC)
                .originalPost(rootPost) // Bài này trỏ về bài gốc
                .media(new java.util.ArrayList<>())
                .build();

        when(postRepository.findById(10L)).thenReturn(Optional.of(sharedPost));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        PostRequest req = new PostRequest();
        req.setContent("Share lại bài của người đã share");

        PostResponse res = postService.sharePost(10L, req);

        // Đảm bảo bài viết mới được trỏ thẳng về rootPost (id 5L), không trỏ về bài trung gian (id 10L)
        assertNotNull(res);
        verify(postRepository).save(rootPost); // Đảm bảo shareCount của root được cập nhật
        assertEquals(11L, rootPost.getShareCount());
    }

    // ==========================================
    // CÁC NHÁNH EXCEPTION (NOT FOUND / IO)
    // ==========================================

    @Test
    void updatePost_whenPostNotFound_shouldThrow() {
        when(postRepository.findById(999L)).thenReturn(Optional.empty());
        
        PostRequest req = new PostRequest();
        // Thêm nội dung giả để vượt qua hàm validatePostPayload
        req.setContent("Nội dung hợp lệ để pass validation"); 
        
        RuntimeException ex = assertThrows(RuntimeException.class, () -> postService.updatePost(999L, req));
        assertEquals("Bài viết không tồn tại!", ex.getMessage());
    }

    @Test
    void deletePost_whenPostNotFound_shouldThrow() {
        when(postRepository.findById(999L)).thenReturn(Optional.empty());
        
        RuntimeException ex = assertThrows(RuntimeException.class, () -> postService.deletePost(999L));
        assertEquals("Bài viết không tồn tại!", ex.getMessage());
    }

    @Test
    void approvePost_whenPostNotFound_shouldThrow() {
        when(postRepository.findById(999L)).thenReturn(Optional.empty());
        
        RuntimeException ex = assertThrows(RuntimeException.class, () -> postService.approvePost(999L));
        assertEquals("Bài viết không tồn tại!", ex.getMessage());
    }

    @Test
    void sharePost_whenOriginalPostNotFound_shouldThrow() {
        when(postRepository.findById(999L)).thenReturn(Optional.empty());
        PostRequest req = new PostRequest();
        
        RuntimeException ex = assertThrows(RuntimeException.class, () -> postService.sharePost(999L, req));
        assertEquals("Bài viết gốc không tồn tại", ex.getMessage());
    }

    @Test
    void createPost_whenMediaUploadFails_shouldThrowRuntimeException() throws IOException {
        PostRequest req = new PostRequest();
        req.setContent("Test IO");
        req.setVisibility(Visibility.PRIVATE);
        
        // Mock một MultipartFile ném lỗi khi lấy InputStream
        org.springframework.web.multipart.MultipartFile badFile = mock(org.springframework.web.multipart.MultipartFile.class);
        when(badFile.getOriginalFilename()).thenReturn("bad.png");
        when(badFile.getInputStream()).thenThrow(new IOException("Disk error"));
        
        req.setMediaFiles(List.of(badFile));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> postService.createPost(req));
        assertTrue(ex.getMessage().contains("Lỗi lưu file: bad.png"));
    }
}

