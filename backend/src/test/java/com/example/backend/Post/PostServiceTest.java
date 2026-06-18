package com.example.backend.Post;

import com.example.backend.Enum.MediaType;
import com.example.backend.Enum.Visibility;
import com.example.backend.Event.NotificationEvent;
import com.example.backend.PostMedia.PostMedia;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import com.example.backend.VPTLpoint.VptlService;
import com.example.backend.Storage.FileStorageService;
import com.example.backend.Moderation.AutoModerationService;
import com.example.backend.Enum.ReactionType;
import com.example.backend.PostReaction.PostReaction;
import com.example.backend.PostReaction.PostReactionRepository;
import com.example.backend.PostReaction.ReactionUserResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostReactionRepository postReactionRepository;

    @Mock
    private ApplicationEventPublisher evenPublisher;

    @Mock
    private VptlService vptlService;

    // 🟢 THÊM MOCK CHO BOT KIỂM DUYỆT
    @Mock
    private AutoModerationService autoModerationService;

    @InjectMocks
    private PostService postService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1);
        currentUser.setStudentCode("SV001");
        currentUser.setRole("STUDENT");
        currentUser.setFullName("User 1");

        mockSecurityContext("SV001");

        lenient().when(userRepository.findByStudentCode("SV001")).thenReturn(Optional.of(currentUser));
        
        // 🟢 GIẢ LẬP MẶC ĐỊNH: Các bài test thông thường coi như không có từ cấm
        lenient().when(autoModerationService.containsBadWord(anyString())).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext(String studentCode) {
        if (studentCode == null) {
            SecurityContextHolder.clearContext();
            return;
        }
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn(studentCode);

        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    // ==========================================
    // 1. NHÓM TEST API TẠO BÀI (CREATE)
    // ==========================================

    @Test
    @DisplayName("Tạo bài viết không nội dung, không media -> Ném lỗi 400")
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

    // 🟢 ĐÃ CẬP NHẬT: Test Bot phát hiện nội dung độc hại
    @Test
    @DisplayName("Tạo bài viết CHỨA TỪ CẤM -> Bot ép về PENDING chờ duyệt")
    void createPost_whenToxicContent_shouldForcePending() {
        PostRequest req = new PostRequest();
        req.setContent("Đây là một từ bị cấm");
        req.setVisibility(Visibility.PUBLIC);

        // Giả lập bot phát hiện từ cấm
        when(autoModerationService.containsBadWord(anyString())).thenReturn(true);
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        PostResponse res = postService.createPost(req);

        assertNotNull(res);
        assertEquals(Visibility.PENDING, res.getVisibility(), "Bài viết phải bị giam ở chế độ PENDING");
        verify(vptlService, never()).trackSocialActivity(anyInt(), anyString());
    }

    // 🟢 ĐÃ CẬP NHẬT: Test Bot cho qua nội dung sạch
    @Test
    @DisplayName("Tạo bài viết NỘI DUNG SẠCH -> Bot cho qua PUBLIC và cộng điểm")
    void createPost_whenCleanContent_shouldApproveToPublic() {
        PostRequest req = new PostRequest();
        req.setContent("Bài viết trong sáng");
        req.setVisibility(Visibility.PUBLIC);

        // Giả lập bot KHÔNG phát hiện từ cấm
        when(autoModerationService.containsBadWord(anyString())).thenReturn(false);
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        PostResponse res = postService.createPost(req);

        assertNotNull(res);
        assertEquals(Visibility.PUBLIC, res.getVisibility(), "Bài viết được tự động lên sóng PUBLIC");
        verify(vptlService).trackSocialActivity(currentUser.getId(), "POST");
    }

    @Test
    @DisplayName("Tạo bài viết KHÔNG có media")
    void createPost_withoutMedia() {
        PostRequest req = new PostRequest();
        req.setContent("Just text");
        req.setVisibility(Visibility.PRIVATE);
        req.setMediaFiles(null);

        when(postRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PostResponse res = postService.createPost(req);
        assertTrue(res.getMedia().isEmpty());
    }

    @Test
    @DisplayName("Tạo bài viết với 4 loại Media khác nhau")
    void createPost_withVariousMediaTypes() {
        MockMultipartFile nullTypeFile = new MockMultipartFile("file", "test", null, "data".getBytes());
        MockMultipartFile videoFile = new MockMultipartFile("file", "test", "video/mp4", "data".getBytes());
        MockMultipartFile audioFile = new MockMultipartFile("file", "test", "audio/mp3", "data".getBytes());
        MockMultipartFile imageFile = new MockMultipartFile("file", "test", "image/png", "data".getBytes());

        PostRequest req = new PostRequest();
        req.setContent("Test Media");
        req.setVisibility(Visibility.PRIVATE);
        req.setMediaFiles(List.of(nullTypeFile, videoFile, audioFile, imageFile));

        when(fileStorageService.storeFile(any(MultipartFile.class), eq("posts"))).thenReturn("url");
        when(postRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PostResponse res = postService.createPost(req);

        assertEquals(4, res.getMedia().size());
        assertEquals("IMAGE", res.getMedia().get(0).getType());
        assertEquals("VIDEO", res.getMedia().get(1).getType());
        assertEquals("AUDIO", res.getMedia().get(2).getType());
        assertEquals("IMAGE", res.getMedia().get(3).getType());
    }

    @Test
    @DisplayName("Chuẩn hóa content bị null")
    void createPost_withNullContent() {
        PostRequest req = new PostRequest();
        req.setContent(null);
        req.setVisibility(Visibility.PRIVATE);
        req.setMediaFiles(List.of(new MockMultipartFile("file", "data".getBytes())));

        when(postRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PostResponse res = postService.createPost(req);
        assertEquals("", res.getContent());
    }

    @Test
    void createPost_whenMediaUploadFails_shouldThrowRuntimeException() {
        PostRequest req = new PostRequest();
        req.setContent("Test IO");
        req.setVisibility(Visibility.PRIVATE);

        MultipartFile badFile = mock(MultipartFile.class);
        when(fileStorageService.storeFile(any(MultipartFile.class), eq("posts")))
                .thenThrow(new RuntimeException("S3 upload failed"));
        req.setMediaFiles(List.of(badFile));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> postService.createPost(req));
        assertEquals("S3 upload failed", ex.getMessage());
    }

    // ==========================================
    // 2. NHÓM TEST SỬA VÀ XÓA (UPDATE & DELETE)
    // ==========================================

    @Test
    @DisplayName("Lỗi Update khi Post không tồn tại")
    void updatePost_whenPostNotFound_shouldThrow() {
        when(postRepository.findById(999L)).thenReturn(Optional.empty());
        PostRequest req = new PostRequest();
        req.setContent("Nội dung");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> postService.updatePost(999L, req));
        assertEquals("Bài viết không tồn tại!", ex.getMessage());
    }

    @Test
    @DisplayName("Không cho phép sửa bài viết của người khác")
    void updatePost_whenNotAuthor_shouldThrow() {
        User other = new User();
        other.setId(2);
        Post post = Post.builder().id(10L).author(other).media(new ArrayList<>()).build();
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));

        PostRequest req = new PostRequest();
        req.setContent("x");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> postService.updatePost(10L, req));
        assertEquals("Bạn không có quyền chỉnh sửa bài viết này!", ex.getMessage());
    }

    // 🟢 ĐÃ CẬP NHẬT: Test sửa bài lòi ra từ cấm
    @Test
    @DisplayName("Sửa bài viết lòi ra TỪ CẤM -> Bot ép về PENDING")
    void updatePost_whenToxicContent_shouldForcePending() {
        Post post = Post.builder().id(10L).author(currentUser).media(new ArrayList<>()).build();
        post.setVisibility(Visibility.PUBLIC);

        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(autoModerationService.containsBadWord(anyString())).thenReturn(true);
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        PostRequest req = new PostRequest();
        req.setContent("Đã sửa thêm từ cấm vào đây");
        req.setVisibility(Visibility.PUBLIC);

        PostResponse res = postService.updatePost(10L, req);

        assertEquals(Visibility.PENDING, res.getVisibility(), "Bài viết bị phát hiện từ cấm phải về PENDING");
    }

    // 🟢 ĐÃ CẬP NHẬT: Test sửa bài an toàn
    @Test
    @DisplayName("Sửa bài viết nội dung SẠCH -> Lưu đúng trạng thái mong muốn")
    void updatePost_whenCleanContent_shouldUpdateNormally() {
        Post post = Post.builder().id(10L).author(currentUser).media(new ArrayList<>()).build();
        post.setVisibility(Visibility.PUBLIC);

        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        PostRequest req = new PostRequest();
        req.setContent("Nội dung sạch sẽ");
        req.setVisibility(Visibility.CLASS); 
        req.setMediaFiles(null); 

        PostResponse res = postService.updatePost(10L, req);

        assertEquals(Visibility.CLASS, res.getVisibility(), "Phải về đúng trạng thái CLASS do nội dung sạch");
        assertTrue(post.getMedia().isEmpty());
    }

    @Test
    void deletePost_whenPostNotFound_shouldThrow() {
        when(postRepository.findById(999L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> postService.deletePost(999L));
        assertEquals("Bài viết không tồn tại!", ex.getMessage());
    }

    @Test
    void deletePost_whenNotAuthorAndNotAdmin_shouldThrow() {
        User other = new User();
        other.setId(2);
        Post post = Post.builder().id(10L).author(other).build();
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> postService.deletePost(10L));
        assertEquals("Bạn không có quyền xóa bài viết này!", ex.getMessage());
    }

    @Test
    void deletePost_whenAdmin_shouldDelete() {
        currentUser.setRole("ADMIN");
        User other = new User();
        other.setId(2);
        Post post = Post.builder().id(10L).author(other).build();
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));

        postService.deletePost(10L);
        verify(postRepository).delete(post);
    }

    @Test
    void deletePost_whenIsAuthor_shouldDeleteSuccessfully() {
        Post post = Post.builder().id(10L).author(currentUser).build();
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));

        postService.deletePost(10L);
        verify(postRepository).delete(post);
    }

// ==========================================
    // 3. NHÓM TEST TƯƠNG TÁC (REACTIONS, SHARE, APPROVE)
    // ==========================================

    @Test
    void approvePost_whenPostNotFound_shouldThrow() {
        when(postRepository.findById(999L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> postService.approvePost(999L));
        assertEquals("Bài viết không tồn tại!", ex.getMessage());
    }

    @Test
    void approvePost_whenWasPending_shouldTrackActivity() {
        User author = new User();
        author.setId(5);
        Post post = Post.builder().id(10L).author(author).visibility(Visibility.PENDING).build();
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));

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

        postService.approvePost(10L);
        verify(vptlService, never()).trackSocialActivity(anyInt(), eq("POST"));
    }

    @Test
    @DisplayName("Lỗi React khi Post không tồn tại")
    void reactToPost_whenPostNotFound_shouldThrow() {
        when(postRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> postService.reactToPost(999L, ReactionType.LIKE));
        assertEquals("Bài viết không tồn tại!", ex.getMessage());
    }

    @Test
    @DisplayName("Khi thả lại cảm xúc cũ -> Xóa cảm xúc (Unlike), cập nhật ngầm định vào đệm RAM")
    void reactToPost_whenSameReaction_shouldDelete_andUpdateBuffer() {
        Post post = Post.builder().id(10L).build();
        PostReaction reaction = PostReaction.builder().id(1L).reactionType(ReactionType.LIKE).build();
        
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(postReactionRepository.findByPostIdAndUserId(10L, 1L)).thenReturn(Optional.of(reaction));

        postService.reactToPost(10L, ReactionType.LIKE);

        verify(postReactionRepository).delete(reaction);
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("Khi đổi cảm xúc (VD: LIKE -> LOVE) -> Cập nhật DB, thông báo và lưu RAM đệm")
    void reactToPost_whenChangeReaction_shouldUpdate_andNotify() {
        User author = new User(); author.setId(2);
        Post post = Post.builder().id(10L).author(author).build();
        PostReaction existingReaction = PostReaction.builder().id(1L).reactionType(ReactionType.LIKE).build();

        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(postReactionRepository.findByPostIdAndUserId(10L, 1L)).thenReturn(Optional.of(existingReaction));

        postService.reactToPost(10L, ReactionType.LOVE);

        assertEquals(ReactionType.LOVE, existingReaction.getReactionType(), "Cảm xúc phải được đổi thành LOVE");
        verify(postReactionRepository).save(existingReaction);
        verify(evenPublisher).publishEvent(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("Khi chưa có cảm xúc -> Tạo mới, cộng điểm VPTL, thông báo và lưu đệm RAM")
    void reactToPost_whenNewReaction_shouldSave_track_andPublishNotification() {
        User author = new User(); author.setId(2);
        Post post = Post.builder().id(10L).author(author).build();

        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(postReactionRepository.findByPostIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        postService.reactToPost(10L, ReactionType.HAHA);

        verify(postReactionRepository).save(any(PostReaction.class));
        verify(vptlService).trackSocialActivity(1, "REACTION_HAHA");
        verify(evenPublisher).publishEvent(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("Background Job sẽ lấy dữ liệu từ RAM đệm và lưu vào Database (reactionCounts)")
    void syncLikesToDatabase_shouldUpdateDB() {
        User author = new User(); author.setId(2);
        Post post = Post.builder().id(10L).author(author).build();
        Map<String, Integer> initialCounts = new HashMap<>();
        initialCounts.put("LIKE", 5); // Bài viết đang có 5 LIKE
        post.setReactionCounts(initialCounts);

        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(postReactionRepository.findByPostIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        // Thực hiện tương tác để ghi vào ConcurrentHashMap (RAM)
        postService.reactToPost(10L, ReactionType.LIKE);
        
        // Gọi hàm đồng bộ Job
        postService.syncLikesToDatabase();

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(postCaptor.capture());
        
        // Tổng LIKE = 5 (cũ) + 1 (mới trong RAM) = 6
        assertEquals(6, postCaptor.getValue().getReactionCounts().get("LIKE"));
    }

    @Test
    void getReactionsByPostId_whenInvalid_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> postService.getReactionsByPostId(null, ReactionType.LIKE, 0, 10));
        
        when(postRepository.existsById(999L)).thenReturn(false);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> postService.getReactionsByPostId(999L, ReactionType.LIKE, 0, 10));
        assertEquals("Post not found", ex.getMessage());
    }

    @Test
    void getReactionsByPostId_shouldReturnPagedUsers() {
        when(postRepository.existsById(10L)).thenReturn(true);
        Pageable pageable = PageRequest.of(0, 10);
        Page<ReactionUserResponse> mockPage = new PageImpl<>(List.of());
        when(postReactionRepository.findUsersReationByPostId(10L, ReactionType.LIKE, pageable)).thenReturn(mockPage);

        Page<ReactionUserResponse> result = postService.getReactionsByPostId(10L, ReactionType.LIKE, 0, 10);
        assertNotNull(result);
        verify(postReactionRepository).findUsersReationByPostId(10L, ReactionType.LIKE, pageable);
    }

    @Test
    void sharePost_whenOriginalPostNotFound_shouldThrow() {
        when(postRepository.findById(999L)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> postService.sharePost(999L, new PostRequest()));
        assertEquals("Bài viết gốc không tồn tại", ex.getMessage());
    }

    @Test
    void sharePost_whenRootPrivate_shouldThrow() {
        Post root = Post.builder().id(10L).visibility(Visibility.PRIVATE).build();
        when(postRepository.findById(10L)).thenReturn(Optional.of(root));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> postService.sharePost(10L, new PostRequest()));
        assertEquals("Không thể chia sẻ bài viết riêng tư", ex.getMessage());
    }

    @Test
    void sharePost_shouldIncrementRootShareCount_saveRootAndShare_track_andPublish() {
        User author = new User();
        author.setId(2);
        Post root = Post.builder().id(10L).author(author).visibility(Visibility.PUBLIC).shareCount(0L)
                .media(new ArrayList<>()).build();
        when(postRepository.findById(10L)).thenReturn(Optional.of(root));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        PostRequest req = new PostRequest();
        req.setContent(" share ");
        PostResponse res = postService.sharePost(10L, req);

        assertNotNull(res);
        assertEquals(1L, root.getShareCount());
        verify(vptlService).trackSocialActivity(1, "SHARE");
        verify(evenPublisher).publishEvent(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("Share bài của người share -> Map về Root")
    void sharePost_whenSharingASharedPost_shouldMapToRootPost() {
        User rootAuthor = new User();
        rootAuthor.setId(2);
        Post rootPost = Post.builder().id(5L).author(rootAuthor).visibility(Visibility.PUBLIC).shareCount(10L)
                .media(new ArrayList<>()).build();

        User sharer = new User();
        sharer.setId(3);
        Post sharedPost = Post.builder().id(10L).author(sharer).visibility(Visibility.PUBLIC).originalPost(rootPost)
                .media(new ArrayList<>()).build();

        when(postRepository.findById(10L)).thenReturn(Optional.of(sharedPost));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        PostRequest req = new PostRequest();
        req.setContent("Share lại");
        PostResponse res = postService.sharePost(10L, req);

        assertNotNull(res);
        assertEquals(11L, rootPost.getShareCount());
    }
    // ==========================================
    // 4. NHÓM TEST TRUY VẤN (GET)
    // ==========================================

    @Test
    void getPostsByAuthor_shouldUseCurrentUserId() {
        Pageable pageable = PageRequest.of(0, 2);
        Page<Post> page = new PageImpl<>(List.of(
                Post.builder().id(1L).author(currentUser).media(new ArrayList<>()).build()), pageable, 1);
        when(postRepository.findByAuthorId(eq(1), any())).thenReturn(page);

        var result = postService.getPostsByAuthor(0, 2);

        assertEquals(1, result.getTotalElements());
        verify(postRepository).findByAuthorId(eq(1), any());
    }

    @Test
    @DisplayName("Lấy tất cả bài viết cho Admin có phân trang")
    void getAllPostsForAdmin_shouldReturnMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Post p1 = Post.builder().id(1L).author(currentUser).media(new ArrayList<>()).build();
        Post p2 = Post.builder().id(2L).author(currentUser).media(new ArrayList<>()).build();
        Page<Post> pagedPosts = new PageImpl<>(List.of(p1, p2), pageable, 2);

        when(postRepository.findAllWithAuthorAndMedia(pageable)).thenReturn(pagedPosts);

        Page<PostResponse> result = postService.getAllPostsForAdmin(pageable);

        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getTotalElements());
    }

    @Test
    @DisplayName("Lấy danh sách bài viết theo mã SV (isSelfPost = true)")
    void getPostsByStudentCode_whenSelfPost_shouldReturnPage() {
        Post post = new Post();
        post.setId(10L);
        post.setAuthor(currentUser);
        post.setMedia(new ArrayList<>());

        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.existsByStudentCode("SV001")).thenReturn(true);
        when(postRepository.findByAuthorStudentCode(eq("SV001"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(post)));

        Page<PostResponse> result = postService.getPostsByStudentCode("SV001", pageable);

        assertNotNull(result);
        assertTrue(result.getContent().get(0).isSelfPost());
    }

    @Test
    @DisplayName("Lấy bài theo mã SV khi KHÔNG đăng nhập (Auth = null)")
    void getPostsByStudentCode_whenAuthNull_shouldReturnSelfPostFalse() {
        mockSecurityContext(null); // Gỡ token

        when(userRepository.existsByStudentCode("SV002")).thenReturn(true);

        User author2 = new User();
        author2.setStudentCode("SV002");
        Post post = Post.builder().id(1L).author(author2).media(new ArrayList<>()).build();

        when(postRepository.findPublicByAuthorStudentCode(eq("SV002"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(post)));

        PageRequest pageable = PageRequest.of(0, 10);
        Page<PostResponse> result = postService.getPostsByStudentCode("SV002", pageable);

        assertFalse(result.getContent().get(0).isSelfPost(), "Vì chưa đăng nhập nên isSelfPost phải luôn là false");
    }

    @Test
    @DisplayName("Lấy bài theo mã SV tự động thêm Sort nếu chưa có")
    void getPostsByStudentCode_whenUnsortedPageable_shouldAddSort() {
        when(userRepository.existsByStudentCode("SV001")).thenReturn(true);
        when(postRepository.findByAuthorStudentCode(eq("SV001"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Pageable unsortedPageable = PageRequest.of(0, 10);
        postService.getPostsByStudentCode("SV001", unsortedPageable);

        ArgumentCaptor<Pageable> pageCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(postRepository).findByAuthorStudentCode(eq("SV001"), pageCaptor.capture());

        assertTrue(pageCaptor.getValue().getSort().isSorted());
    }

    @Test
    @DisplayName("Lấy bài theo mã SV khi Pageable ĐÃ CÓ Sort -> Không ghi đè Sort")
    void getPostsByStudentCode_whenAlreadySorted_shouldKeepSort() {
        when(userRepository.existsByStudentCode("SV001")).thenReturn(true);
        when(postRepository.findByAuthorStudentCode(eq("SV001"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Pageable sortedPageable = PageRequest.of(0, 10, Sort.by("id").descending());
        postService.getPostsByStudentCode("SV001", sortedPageable);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(postRepository).findByAuthorStudentCode(eq("SV001"), captor.capture());
        assertEquals(Sort.by("id").descending(), captor.getValue().getSort());
    }

    @Test
    void getPostsByStudentCode_whenUserNotFound_shouldThrow() {
        when(userRepository.existsByStudentCode("GHOST")).thenReturn(false);
        Pageable pageable = PageRequest.of(0, 10);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> postService.getPostsByStudentCode("GHOST", pageable));
        assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    @DisplayName("getCurrentUser ném lỗi khi Token hỏng hoặc không có trong DB")
    void getCurrentUser_whenUserNotInDB_shouldThrowException() {
        mockSecurityContext("HACKER");
        when(userRepository.findByStudentCode("HACKER")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> postService.createPost(new PostRequest()));
        assertTrue(ex.getMessage().contains("Token không hợp lệ"));
    }

    @Test
    void uploadFileToS3_shouldCallStorageService() {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(fileStorageService.storeFile(mockFile, "posts")).thenReturn("/url");

        String url = postService.uploadFileToS3(mockFile);
        assertEquals("/url", url);
    }

    // ==========================================
    // 5. NHÓM BỔ SUNG TEST CHO CÁC HÀM MAPPER NỘI BỘ
    // ==========================================

    @Test
    @DisplayName("Map Post có OriginalPost -> Kích hoạt buildFlattenedPostResponse và stream Media")
    void mapToPostResponse_withOriginalPost_shouldBuildFlattened() {
        User originalAuthor = new User();
        originalAuthor.setId(2);
        originalAuthor.setFullName("Tác giả gốc");

        PostMedia media = new PostMedia();
        media.setId(1L);
        media.setMediaType(MediaType.IMAGE);
        media.setMediaUrl("http://image.url");

        Post originalPost = Post.builder()
                .id(5L)
                .author(originalAuthor)
                .media(List.of(media)) 
                .build();

        Post sharePost = Post.builder()
                .id(10L)
                .author(currentUser)
                .originalPost(originalPost)
                .media(new ArrayList<>())
                .build();

        PostResponse res = postService.mapToPostResponse(sharePost);

        assertNotNull(res.getOriginalPost());
        assertEquals(5L, res.getOriginalPost().getId());
        assertEquals(1, res.getOriginalPost().getMedia().size(), "Phải map được media của Original Post");
        assertEquals("IMAGE", res.getOriginalPost().getMedia().get(0).getType());
    }

    @Test
    @DisplayName("Map Post tự trỏ OriginalPost = chính nó -> Xóa OriginalPost để tránh đệ quy vô hạn")
    void mapToPostResponse_withSelfOriginalPost_shouldSetOriginalPostNull() {
        Post post = Post.builder()
                .id(10L)
                .author(currentUser)
                .media(new ArrayList<>())
                .build();
        post.setOriginalPost(post); 

        PostResponse res = postService.mapToPostResponse(post);

        assertNull(res.getOriginalPost(), "Nếu tự trỏ, hệ thống phải clear thành null");
    }
}