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

        // Giả lập SecurityContext
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("SV001", null));
        
        // Lenient để tránh lỗi UnnecessaryStubbingException cho các test không cần gọi hàm getCurrentUser()
        lenient().when(userRepository.findByStudentCode("SV001")).thenReturn(Optional.of(currentUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
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
        req.setParentCommentId(50L); // Nhắn gửi tới comment cha không tồn tại
        
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
        assertNull(res.getParentId()); // Là Root comment

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

        Comment parent = Comment.builder().id(50L).build();

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
        verify(commentRepository).incrementReplyCount(50L); // Đảm bảo đếm reply được kích hoạt
        verify(postRepository).incrementCommentCount(10L);
    }

    // ==========================================
    // 2. CẬP NHẬT BÌNH LUẬN (updateComment)
    // ==========================================

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

        verify(postRepository).decrementCommentCount(10L); // Giảm count của Post
        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteComment_whenIsPostOwner_withParent_shouldDecrementReplyAndDelete() {
        // Tác giả comment là người khác, nhưng người đang tương tác là Chủ bài viết (currentUser = 1)
        User commentAuthor = new User(); commentAuthor.setId(99);
        mockPost.setAuthor(currentUser); // Ép currentUser làm chủ bài viết

        Comment parent = Comment.builder().id(20L).replyCount(5L).build();
        Comment comment = Comment.builder().id(50L).author(commentAuthor).post(mockPost).parent(parent).build();

        when(commentRepository.findById(50L)).thenReturn(Optional.of(comment));

        commentService.deleteComment(50L);

        assertEquals(4L, parent.getReplyCount()); // Kiểm tra Reply count đã giảm
        verify(commentRepository).save(parent); // Lưu lại parent
        verify(postRepository).decrementCommentCount(10L);
        verify(commentRepository).delete(comment);
    }

    // ==========================================
    // 4. LẤY DANH SÁCH & TOGGLE LIKE
    // ==========================================

    @Test
    void getCommentsByPost_shouldMapToResponseWithLikes() {
        PageRequest pageable = PageRequest.of(0, 10);
        Comment c1 = Comment.builder().id(50L).author(currentUser).build();
        Comment c2 = Comment.builder().id(60L).author(currentUser).build();
        
        when(commentRepository.findRootCommentsByPostId(10L, pageable))
                .thenReturn(new PageImpl<>(List.of(c1, c2), pageable, 2));
        
        when(commentLikeRepository.findCommentIdsLikedByUser(1L, List.of(50L, 60L)))
                .thenReturn(Set.of(60L)); // User chỉ like comment 60

        Page<CommentResponse> result = commentService.getCommentsByPost(10L, pageable);

        assertEquals(2, result.getContent().size());
        assertFalse(result.getContent().get(0).isLikedByCurrentUser()); // c1
        assertTrue(result.getContent().get(1).isLikedByCurrentUser());  // c2
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