package com.example.backend.Comment;

import com.example.backend.Enum.NotificationType;
import com.example.backend.Event.NotificationEvent;
import com.example.backend.Post.Post;
import com.example.backend.Post.PostRepository;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import com.example.backend.VPTLpoint.VptlService;
import jakarta.persistence.EntityNotFoundException;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CommentLikeRepository commentLikeRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private VptlService vptlService;

    @InjectMocks
    private CommentService commentService;

    private User currentUser;
    private Post mockPost;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1);
        currentUser.setStudentCode("SV001");
        currentUser.setFullName("Lê Hồng Phát");

        User postAuthor = new User();
        postAuthor.setId(2);

        mockPost = new Post();
        mockPost.setId(10L);
        mockPost.setAuthor(postAuthor);

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("SV001", null));
        
        lenient().when(userRepository.findByStudentCode("SV001")).thenReturn(Optional.of(currentUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==========================================
    // 0. TEST BỔ SUNG: getCurrentUser (Exception Lambda)
    // ==========================================
    @Test
    @DisplayName("Bắn lỗi nếu Token hợp lệ nhưng DB không chứa user")
    void getCurrentUser_whenNotFoundInDb_shouldThrow() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("GHOST", null));
        when(userRepository.findByStudentCode("GHOST")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> commentService.createComment(new CommentRequest()));
        assertEquals("Không tìm thấy người dùng (Token không hợp lệ?)", ex.getMessage());
    }

    // ==========================================
    // 1. TẠO BÌNH LUẬN (createComment)
    // ==========================================

    @Test
    void createComment_whenPostNotFound_shouldThrow() {
        CommentRequest req = new CommentRequest();
        req.setPostId(99L);
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> commentService.createComment(req));
        assertEquals("Post not found", ex.getMessage());
    }

    @Test
    void createComment_whenParentNotFound_shouldThrow() {
        CommentRequest req = new CommentRequest();
        req.setPostId(10L);
        req.setParentCommentId(50L); 
        
        when(postRepository.findById(10L)).thenReturn(Optional.of(mockPost));
        when(commentRepository.findById(50L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> commentService.createComment(req));
        assertEquals("Parent comment not found", ex.getMessage());
    }

    @Test
    void createComment_RootComment_shouldSaveAndPublishEvent() {
        CommentRequest req = new CommentRequest();
        req.setPostId(10L);
        req.setContent("Bài viết hay quá!");
        // isAnonymous null -> Mặc định false

        when(postRepository.findById(10L)).thenReturn(Optional.of(mockPost));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(100L);
            c.setCreatedAt(LocalDateTime.now());
            return c;
        });

        CommentResponse res = commentService.createComment(req);

        assertEquals(100L, res.getId());
        assertEquals("Bài viết hay quá!", res.getContent());
        assertNull(res.getParentId()); 

        verify(postRepository).incrementCommentCount(10L);
        verify(vptlService).trackSocialActivity(1, "COMMENT");
        verify(eventPublisher).publishEvent(any(NotificationEvent.class));
    }

    @Test
    void createComment_ReplyComment_shouldIncrementReplyCountAndSave() {
        CommentRequest req = new CommentRequest();
        req.setPostId(10L);
        req.setContent("Đồng ý với bạn!");
        req.setParentCommentId(50L);
        req.setIsAnonymous(false); // Test nhánh false rõ ràng

        // 🟢 ĐÃ SỬA: Tạo tác giả giả lập cho bình luận cha để không bị NullPointerException
        User parentAuthor = new User();
        parentAuthor.setId(2); // Khác với currentUser.getId() = 1 để kích hoạt nhánh gửi thông báo

        Comment parent = Comment.builder()
                .id(50L)
                .author(parentAuthor) // Bơm tác giả vào đây!
                .build();

        when(postRepository.findById(10L)).thenReturn(Optional.of(mockPost));
        when(commentRepository.findById(50L)).thenReturn(Optional.of(parent));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(101L);
            c.setCreatedAt(LocalDateTime.now());
            return c;
        });

        CommentResponse res = commentService.createComment(req);

        assertEquals(50L, res.getParentId());
        verify(commentRepository).incrementReplyCount(50L); 
        verify(postRepository).incrementCommentCount(10L);
        
        // 🟢 BỔ SUNG: Kiểm chứng xem event thông báo REPLY_COMMENT có được bắn ra không
        verify(eventPublisher, atLeastOnce()).publishEvent(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("Tạo bình luận ẨN DANH -> Trả về thông tin Author bị làm mờ (mapToResponse true branch)")
    void createComment_whenAnonymous_shouldMaskAuthor() {
        CommentRequest req = new CommentRequest();
        req.setPostId(10L);
        req.setContent("Mình muốn góp ý ẩn danh...");
        req.setIsAnonymous(true); // 🟢 Bật cờ ẩn danh

        when(postRepository.findById(10L)).thenReturn(Optional.of(mockPost));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(100L);
            return c;
        });

        CommentResponse res = commentService.createComment(req);

        assertEquals("Người dùng ẩn danh", res.getAuthor().getFullName());
        assertEquals(0, res.getAuthor().getId());
        assertEquals("Hidden", res.getAuthor().getStudentCode());
        assertTrue(res.getAuthor().getAvatarUrl().contains("Anonymous"));
    }

    // ==========================================
    // 2. CẬP NHẬT BÌNH LUẬN (updateComment)
    // ==========================================

    @Test
    @DisplayName("Bắn lỗi khi ID comment cần Update không tồn tại")
    void updateComment_whenCommentNotFound_shouldThrow() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> commentService.updateComment(99L, "New"));
        assertEquals("Comment not found", ex.getMessage());
    }

    @Test
    void updateComment_whenNotAuthor_shouldThrow() {
        User otherAuthor = new User();
        otherAuthor.setId(99);
        Comment comment = Comment.builder().id(50L).author(otherAuthor).build();

        when(commentRepository.findById(50L)).thenReturn(Optional.of(comment));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> commentService.updateComment(50L, "New"));
        assertEquals("Bạn không có quyền sửa comment này", ex.getMessage());
    }

    @Test
    void updateComment_whenAuthor_shouldUpdateAndReturn() {
        Comment comment = Comment.builder().id(50L).author(currentUser).content("Old").build();
        when(commentRepository.findById(50L)).thenReturn(Optional.of(comment));
        when(commentLikeRepository.findByCommentIdAndUserId(50L, 1L)).thenReturn(Optional.empty());

        CommentResponse res = commentService.updateComment(50L, "Mới update");

        assertEquals("Mới update", res.getContent());
        assertFalse(res.isLikedByCurrentUser());
    }

    // ==========================================
    // 3. XÓA BÌNH LUẬN (deleteComment)
    // ==========================================

    @Test
    @DisplayName("Bắn lỗi khi ID comment cần Xóa không tồn tại")
    void deleteComment_whenCommentNotFound_shouldThrow() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> commentService.deleteComment(99L));
        assertEquals("Comment not found", ex.getMessage());
    }

    @Test
    void deleteComment_whenNotAuthorized_shouldThrow() {
        User otherAuthor = new User(); otherAuthor.setId(99);
        User otherPostOwner = new User(); otherPostOwner.setId(88);
        
        Post post = new Post(); post.setId(10L); post.setAuthor(otherPostOwner);
        Comment comment = Comment.builder().id(50L).author(otherAuthor).post(post).build();

        when(commentRepository.findById(50L)).thenReturn(Optional.of(comment));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> commentService.deleteComment(50L));
        assertEquals("Không có quyền xóa comment này", ex.getMessage());
    }

    @Test
    void deleteComment_whenIsAuthor_noParent_shouldDelete() {
        Comment comment = Comment.builder().id(50L).author(currentUser).post(mockPost).build();
        when(commentRepository.findById(50L)).thenReturn(Optional.of(comment));

        commentService.deleteComment(50L);

        verify(postRepository).decrementCommentCount(10L); 
        verify(commentRepository).delete(comment);
    }

    @Test
    @DisplayName("Xóa reply -> Giảm replyCount của Parent (> 0)")
    void deleteComment_whenHasParentWithReplies_shouldDecrementParentReplyCount() {
        Comment parent = Comment.builder().id(20L).replyCount(5L).build();
        Comment comment = Comment.builder().id(50L).author(currentUser).post(mockPost).parent(parent).build();

        when(commentRepository.findById(50L)).thenReturn(Optional.of(comment));

        commentService.deleteComment(50L);

        assertEquals(4L, parent.getReplyCount()); // Bị trừ đi 1
        verify(commentRepository).save(parent);
        verify(postRepository).decrementCommentCount(10L);
        verify(commentRepository).delete(comment);
    }

    @Test
    @DisplayName("Xóa reply -> Không làm gì nếu Parent có replyCount = 0 (phòng lỗi dữ liệu âm)")
    void deleteComment_whenHasParentWithNoReplies_shouldNotDecrement() {
        Comment parent = Comment.builder().id(20L).replyCount(0L).build();
        Comment comment = Comment.builder().id(50L).author(currentUser).post(mockPost).parent(parent).build();

        when(commentRepository.findById(50L)).thenReturn(Optional.of(comment));

        commentService.deleteComment(50L);

        assertEquals(0L, parent.getReplyCount()); // Vẫn là 0
        verify(commentRepository, never()).save(parent); // Không gọi save thừa thãi
        verify(commentRepository).delete(comment);
    }

    // ==========================================
    // 4. LẤY DANH SÁCH & TOGGLE LIKE
    // ==========================================

    @Test
    @DisplayName("Test lấy danh sách Root Comments + Kiểm tra Liked")
    void getCommentsByPost_shouldMapToResponseWithLikes() {
        PageRequest pageable = PageRequest.of(0, 10);
        Comment c1 = Comment.builder().id(50L).author(currentUser).build();
        Comment c2 = Comment.builder().id(60L).author(currentUser).build();
        
        when(commentRepository.findRootCommentsByPostId(10L, pageable))
                .thenReturn(new PageImpl<>(List.of(c1, c2), pageable, 2));
        
        when(commentLikeRepository.findCommentIdsLikedByUser(1L, List.of(50L, 60L)))
                .thenReturn(Set.of(60L)); 

        Page<CommentResponse> result = commentService.getCommentsByPost(10L, pageable);

        assertEquals(2, result.getContent().size());
        assertFalse(result.getContent().get(0).isLikedByCurrentUser()); 
        assertTrue(result.getContent().get(1).isLikedByCurrentUser());  
    }

    @Test
    @DisplayName("Khi Page trả về danh sách RỖNG -> KHÔNG gọi Query Like dư thừa (mapToPageResponse nhánh if)")
    void getCommentsByPost_whenNoComments_shouldReturnEmptyPage_andNotQueryLikes() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(commentRepository.findRootCommentsByPostId(10L, pageable)).thenReturn(Page.empty(pageable));

        Page<CommentResponse> result = commentService.getCommentsByPost(10L, pageable);

        assertTrue(result.isEmpty());
        // 🟢 Đảm bảo hàm findCommentIdsLikedByUser KHÔNG bị gọi nếu danh sách comment rỗng
        verify(commentLikeRepository, never()).findCommentIdsLikedByUser(anyLong(), anyList());
    }

    @Test
    @DisplayName("Bổ sung Test cho hàm getReplies (Coverage 0% -> 100%)")
    void getReplies_shouldMapToResponse() {
        PageRequest pageable = PageRequest.of(0, 10);
        Comment parent = Comment.builder().id(20L).author(currentUser).build();
        Comment reply = Comment.builder().id(50L).author(currentUser).parent(parent).build();

        when(commentRepository.findRepliesByParentId(20L, pageable))
                .thenReturn(new PageImpl<>(List.of(reply), pageable, 1));
        
        when(commentLikeRepository.findCommentIdsLikedByUser(eq(1L), anyList()))
                .thenReturn(Set.of());

        Page<CommentResponse> result = commentService.getReplies(20L, pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(50L, result.getContent().get(0).getId());
        assertEquals(20L, result.getContent().get(0).getParentId());
    }

    @Test
    void toggleLike_whenAlreadyLiked_shouldRemoveLike_andDecrement() {
        CommentLike like = new CommentLike();
        when(commentLikeRepository.findByCommentIdAndUserId(50L, 1L)).thenReturn(Optional.of(like));

        commentService.toggleLike(50L);

        verify(commentLikeRepository).delete(like);
        verify(commentRepository).decrementLikeCount(50L);
        verify(commentRepository, never()).incrementLikeCount(anyLong());
    }

    @Test
    void toggleLike_whenNotLiked_shouldAddLike_increment_andTrackActivity() {
        when(commentLikeRepository.findByCommentIdAndUserId(50L, 1L)).thenReturn(Optional.empty());
        
        Comment comment = new Comment(); comment.setId(50L);
        when(commentRepository.getReferenceById(50L)).thenReturn(comment);
        when(userRepository.getReferenceById(1L)).thenReturn(currentUser);

        commentService.toggleLike(50L);

        verify(commentLikeRepository).save(any(CommentLike.class));
        verify(commentRepository).incrementLikeCount(50L);
        verify(vptlService).trackSocialActivity(1, "LIKE");
    }
}