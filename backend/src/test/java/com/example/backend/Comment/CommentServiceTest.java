package com.example.backend.Comment;

import com.example.backend.Enum.NotificationType;
import com.example.backend.Enum.ReactionType;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    private CommentReactionRepository commentReactionRepository;
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

    @Test
    @DisplayName("Bắn lỗi nếu Token hợp lệ nhưng DB không chứa user")
    void getCurrentUser_whenNotFoundInDb_shouldThrow() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("GHOST", null));
        when(userRepository.findByStudentCode("GHOST")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> commentService.createComment(new CommentRequest()));
        assertEquals("Không tìm thấy người dùng (Token không hợp lệ?)", ex.getMessage());
    }

    // ==========================================
    // 1. TẠO BÌNH LUẬN (createComment) & CÁC NHÁNH ĐIỀU KIỆN
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
    @DisplayName("Tạo bình luận gốc: User tự bình luận bài của chính mình -> Bỏ qua Event")
    void createComment_RootComment_SelfPost_shouldSaveButNotPublishEvent() {
        mockPost.setAuthor(currentUser); // Bài của chính user 1

        CommentRequest req = new CommentRequest();
        req.setPostId(10L);
        req.setContent("Tự khen bài mình");
        req.setIsAnonymous(null); 

        when(postRepository.findById(10L)).thenReturn(Optional.of(mockPost));
        when(commentRepository.save(any())).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(100L);
            return c;
        });

        commentService.createComment(req);

        // 🟢 FIX LỖI: Dùng any(NotificationEvent.class) thay vì any()
        verify(eventPublisher, never()).publishEvent(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("Tạo bình luận Reply: Bình thường -> Bắn 2 event (Reply cho chủ cmt & Comment_Post cho chủ bài)")
    void createComment_ReplyComment_shouldIncrementReplyCountAndSave() {
        CommentRequest req = new CommentRequest();
        req.setPostId(10L);
        req.setContent("Đồng ý với bạn!");
        req.setParentCommentId(50L);
        req.setIsAnonymous(false);

        User parentAuthor = new User();
        parentAuthor.setId(3); // Khác currentUser (1) và postAuthor (2)

        Comment parent = Comment.builder().id(50L).author(parentAuthor).build();

        when(postRepository.findById(10L)).thenReturn(Optional.of(mockPost));
        when(commentRepository.findById(50L)).thenReturn(Optional.of(parent));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(101L);
            return c;
        });

        commentService.createComment(req);

        verify(commentRepository).incrementReplyCount(50L); 
        // 🟢 FIX LỖI: Dùng any(NotificationEvent.class)
        verify(eventPublisher, times(2)).publishEvent(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("Tạo Reply: User tự reply comment của mình -> KHÔNG gửi thông báo Reply")
    void createComment_Reply_SelfReply_shouldNotNotifyReply() {
        CommentRequest req = new CommentRequest();
        req.setPostId(10L); req.setContent("Tự reply"); req.setParentCommentId(50L);
        
        Comment parent = Comment.builder().id(50L).author(currentUser).build();
        
        when(postRepository.findById(10L)).thenReturn(Optional.of(mockPost));
        when(commentRepository.findById(50L)).thenReturn(Optional.of(parent));
        when(commentRepository.save(any())).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(101L);
            return c;
        });

        commentService.createComment(req);

        // 🟢 FIX LỖI: Dùng any(NotificationEvent.class)
        verify(eventPublisher, times(1)).publishEvent(any(NotificationEvent.class)); 
    }

    @Test
    @DisplayName("Tạo Reply: Chủ bài viết đi reply comment -> KHÔNG tự gửi thông báo Comment cho mình")
    void createComment_Reply_ByPostOwner_shouldNotNotifyPostOwner() {
        mockPost.setAuthor(currentUser); 

        CommentRequest req = new CommentRequest();
        req.setPostId(10L); req.setContent("Chủ thớt rep"); req.setParentCommentId(50L);
        
        User parentAuthor = new User(); parentAuthor.setId(3);
        Comment parent = Comment.builder().id(50L).author(parentAuthor).build();

        when(postRepository.findById(10L)).thenReturn(Optional.of(mockPost));
        when(commentRepository.findById(50L)).thenReturn(Optional.of(parent));
        when(commentRepository.save(any())).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(101L);
            return c;
        });

        commentService.createComment(req);

        // 🟢 FIX LỖI: Dùng any(NotificationEvent.class)
        verify(eventPublisher, times(1)).publishEvent(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("Tạo bình luận ẨN DANH -> Trả về thông tin Author bị làm mờ")
    void createComment_whenAnonymous_shouldMaskAuthor() {
        CommentRequest req = new CommentRequest();
        req.setPostId(10L);
        req.setContent("Mình muốn góp ý ẩn danh...");
        req.setIsAnonymous(true); 

        when(postRepository.findById(10L)).thenReturn(Optional.of(mockPost));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(100L);
            return c;
        });

        CommentResponse res = commentService.createComment(req);

        assertEquals("Người dùng ẩn danh", res.getAuthor().getFullName());
        assertEquals(0, res.getAuthor().getId());
    }

    // ==========================================
    // 2. CẬP NHẬT BÌNH LUẬN (updateComment)
    // ==========================================

    @Test
    @DisplayName("Bắn lỗi khi ID comment cần Update không tồn tại (Quét Lambda)")
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
    void updateComment_whenAuthor_shouldUpdateAndReturn_WithProjection() {
        Comment comment = Comment.builder().id(50L).author(currentUser).content("Old").build();
        when(commentRepository.findById(50L)).thenReturn(Optional.of(comment));
        
        CommentReaction reaction = CommentReaction.builder().reactionType(ReactionType.LOVE).build();
        when(commentReactionRepository.findByCommentIdAndUserId(50L, 1L)).thenReturn(Optional.of(reaction));
        
        CommentReactionRepository.SingleReactionCountProjection proj = mock(CommentReactionRepository.SingleReactionCountProjection.class);
        when(proj.getReactionType()).thenReturn(ReactionType.LOVE);
        when(proj.getCount()).thenReturn(5L);

        when(commentReactionRepository.countReactionsByCommentId(50L)).thenReturn(List.of(proj));

        CommentResponse res = commentService.updateComment(50L, "Mới update");

        assertEquals(ReactionType.LOVE, res.getCurrentUserReaction()); 
        assertEquals(5L, res.getReactionCounts().get("LOVE"));
    }

    // ==========================================
    // 3. XÓA BÌNH LUẬN (deleteComment)
    // ==========================================

    @Test
    @DisplayName("Bắn lỗi khi ID comment cần Xóa không tồn tại (Quét Lambda)")
    void deleteComment_whenCommentNotFound_shouldThrow() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> commentService.deleteComment(99L));
        assertEquals("Comment not found", ex.getMessage());
    }

    @Test
    @DisplayName("Xóa comment gốc -> Không bị lỗi NullPointer ở cụm Parent")
    void deleteComment_whenIsAuthor_noParent_shouldDelete() {
        Comment comment = Comment.builder().id(50L).author(currentUser).post(mockPost).parent(null).build();
        when(commentRepository.findById(50L)).thenReturn(Optional.of(comment));

        commentService.deleteComment(50L);

        verify(postRepository).decrementCommentCount(10L); 
        verify(commentRepository).delete(comment);
    }

    @Test
    @DisplayName("Xóa comment: Người xóa là Chủ Bài Viết -> Cho phép xóa")
    void deleteComment_whenIsPostOwner_shouldDelete() {
        mockPost.setAuthor(currentUser); 
        
        User commentAuthor = new User(); commentAuthor.setId(99); 
        Comment comment = Comment.builder().id(50L).author(commentAuthor).post(mockPost).build();
        
        when(commentRepository.findById(50L)).thenReturn(Optional.of(comment));

        assertDoesNotThrow(() -> commentService.deleteComment(50L));
        verify(commentRepository).delete(comment);
    }

    @Test
    @DisplayName("Xóa reply -> Giảm replyCount của Parent (> 0)")
    void deleteComment_whenHasParentWithReplies_shouldDecrementParentReplyCount() {
        Comment parent = Comment.builder().id(20L).replyCount(5L).author(new User()).build();
        Comment comment = Comment.builder().id(50L).author(currentUser).post(mockPost).parent(parent).build();

        when(commentRepository.findById(50L)).thenReturn(Optional.of(comment));

        commentService.deleteComment(50L);

        assertEquals(4L, parent.getReplyCount());
        verify(commentRepository).save(parent);
    }

    @Test
    @DisplayName("Xóa reply -> Parent có replyCount = 0 (Chống âm dữ liệu)")
    void deleteComment_whenParentHasZeroReplies_shouldNotDecrement() {
        Comment parent = Comment.builder().id(20L).replyCount(0L).author(new User()).build();
        Comment comment = Comment.builder().id(50L).author(currentUser).post(mockPost).parent(parent).build();

        when(commentRepository.findById(50L)).thenReturn(Optional.of(comment));

        commentService.deleteComment(50L);

        assertEquals(0L, parent.getReplyCount()); 
        verify(commentRepository, never()).save(parent);
    }

    // ==========================================
    // 4. LẤY DANH SÁCH BÌNH LUẬN VÀ REPLIES
    // ==========================================

    @Test
    @DisplayName("Lấy danh sách Root Comments: Danh sách RỖNG -> Bỏ qua truy vấn Map")
    void getCommentsByPost_whenEmpty_shouldReturnEmptyPage_andSkipQueries() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(commentRepository.findRootCommentsByPostId(10L, pageable)).thenReturn(Page.empty(pageable));

        Page<CommentResponse> result = commentService.getCommentsByPost(10L, pageable);

        assertTrue(result.isEmpty());
        verify(commentReactionRepository, never()).findReactionsByUserIdAndCommentIds(anyLong(), any());
        verify(commentReactionRepository, never()).countReactionsByCommentIds(any());
    }

    @Test
    @DisplayName("Lấy danh sách Replies: Thành công với Projection Đếm Cảm Xúc")
    void getReplies_shouldMapToResponseWithReactions() {
        PageRequest pageable = PageRequest.of(0, 10);
        Comment parent = Comment.builder().id(20L).author(currentUser).build();
        Comment reply = Comment.builder().id(50L).author(currentUser).parent(parent).build();

        when(commentRepository.findRepliesByParentId(20L, pageable))
                .thenReturn(new PageImpl<>(List.of(reply), pageable, 1));
        
        CommentReaction reaction = CommentReaction.builder().comment(reply).reactionType(ReactionType.HAHA).build();
        when(commentReactionRepository.findReactionsByUserIdAndCommentIds(1L, List.of(50L)))
                .thenReturn(List.of(reaction, reaction)); 

        CommentReactionRepository.ReactionCountProjection countMock = mock(CommentReactionRepository.ReactionCountProjection.class);
        when(countMock.getCommentId()).thenReturn(50L);
        when(countMock.getReactionType()).thenReturn(ReactionType.HAHA);
        when(countMock.getCount()).thenReturn(2L);

        when(commentReactionRepository.countReactionsByCommentIds(List.of(50L)))
                .thenReturn(List.of(countMock));

        Page<CommentResponse> result = commentService.getReplies(20L, pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(ReactionType.HAHA, result.getContent().get(0).getCurrentUserReaction());
        assertEquals(2L, result.getContent().get(0).getReactionCounts().get("HAHA"));
    }

    // ==========================================
    // 5. TEST TÍNH NĂNG THẢ CẢM XÚC (React To Comment) & GET REACTIONS
    // ==========================================

    @Test
    void getReactionsByCommentId_whenInvalid_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> commentService.getReactionsByCommentId(null, ReactionType.LIKE, 0, 10));
        
        when(commentRepository.existsById(99L)).thenReturn(false);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> commentService.getReactionsByCommentId(99L, ReactionType.LIKE, 0, 10));
        assertEquals("Comment not found", ex.getMessage());
    }

    @Test
    void getReactionsByCommentId_shouldReturnPagedUsers() {
        when(commentRepository.existsById(10L)).thenReturn(true);
        Pageable pageable = PageRequest.of(0, 10);
        when(commentReactionRepository.findUsersReactionByCommentId(10L, ReactionType.LIKE, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        Page<?> result = commentService.getReactionsByCommentId(10L, ReactionType.LIKE, 0, 10);
        assertNotNull(result);
    }

    @Test
    @DisplayName("React To Comment - Comment Not Found (Quét Lambda)")
    void reactToComment_whenCommentNotFound_shouldThrow() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> commentService.reactToComment(99L, ReactionType.LIKE));
        assertEquals("Comment not found", ex.getMessage());
    }

    @Test
    @DisplayName("React To Comment: Bắn lỗi nếu Receiver (Tác giả comment) bị xóa khỏi DB (Quét Lambda)")
    void reactToComment_whenReceiverNotFound_shouldThrow() {
        User author = new User(); author.setId(99);
        Comment comment = Comment.builder().id(50L).author(author).build();
        when(commentRepository.findById(50L)).thenReturn(Optional.of(comment));
        
        when(userRepository.findById(99)).thenReturn(Optional.empty()); 

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> commentService.reactToComment(50L, ReactionType.LIKE));
        assertEquals("Receiver not found", ex.getMessage());
    }

    @Test
    @DisplayName("Thả lại cảm xúc cũ -> Xóa cảm xúc (Unlike/Unreact)")
    void reactToComment_whenAlreadyReactedSameType_shouldRemoveReaction_andDecrement() {
        Comment comment = Comment.builder().id(50L).author(new User()).build();
        when(commentRepository.findById(50L)).thenReturn(Optional.of(comment));
        
        when(userRepository.findById(any())).thenReturn(Optional.of(new User()));

        CommentReaction reaction = CommentReaction.builder().reactionType(ReactionType.LIKE).build();
        when(commentReactionRepository.findByCommentIdAndUserId(50L, 1L)).thenReturn(Optional.of(reaction));

        commentService.reactToComment(50L, ReactionType.LIKE);

        verify(commentReactionRepository).delete(reaction);
        verify(commentRepository).decrementReactionCount(50L);
    }

    @Test
    @DisplayName("Đổi cảm xúc -> Cập nhật loại cảm xúc và bắn thông báo")
    void reactToComment_whenChangeReactionType_shouldUpdateReaction_andNotify() {
        Comment comment = Comment.builder().id(50L).author(currentUser).build(); 
        when(commentRepository.findById(50L)).thenReturn(Optional.of(comment));
        when(userRepository.findById(1)).thenReturn(Optional.of(currentUser));

        CommentReaction reaction = CommentReaction.builder().reactionType(ReactionType.LIKE).build();
        when(commentReactionRepository.findByCommentIdAndUserId(50L, 1L)).thenReturn(Optional.of(reaction));

        commentService.reactToComment(50L, ReactionType.LOVE); 

        assertEquals(ReactionType.LOVE, reaction.getReactionType());
        verify(commentReactionRepository).save(reaction);
        verify(eventPublisher).publishEvent(any(NotificationEvent.class)); // ĐÃ FIX
    }

    @Test
    @DisplayName("Chưa có cảm xúc -> Thêm mới, tăng đếm, và bắn thông báo")
    void reactToComment_whenNewReaction_shouldAddReaction_increment_andTrackActivity() {
        Comment comment = Comment.builder().id(50L).author(currentUser).build();
        when(commentRepository.findById(50L)).thenReturn(Optional.of(comment));
        when(userRepository.findById(1)).thenReturn(Optional.of(currentUser));
        when(userRepository.getReferenceById(1L)).thenReturn(currentUser);

        when(commentReactionRepository.findByCommentIdAndUserId(50L, 1L)).thenReturn(Optional.empty());

        commentService.reactToComment(50L, ReactionType.HAHA);

        verify(commentReactionRepository).save(any(CommentReaction.class));
        verify(commentRepository).incrementReactionCount(50L);
        verify(eventPublisher).publishEvent(any(NotificationEvent.class)); // ĐÃ FIX
    }
}