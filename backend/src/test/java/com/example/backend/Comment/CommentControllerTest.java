package com.example.backend.Comment;

import com.example.backend.Integration.BaseControllerTest;
import com.example.backend.User.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = CommentController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
class CommentControllerTest extends BaseControllerTest {

    @MockBean
    private CommentService commentService;

    private CommentResponse mockResponse;
    private CommentRequest mockRequest;

    @BeforeEach
    void setUp() {
        // Chuẩn bị Mock Request
        mockRequest = new CommentRequest();
        mockRequest.setPostId(100L);
        mockRequest.setContent("Bài viết hay quá sếp ơi!");
        // mockRequest.setParentCommentId(null);

        // Chuẩn bị Mock Response
        UserResponse authorResponse = UserResponse.builder()
                .id(1)
                .fullName("Lê Hồng Phát")
                .studentCode("1412")
                .build();

        mockResponse = CommentResponse.builder()
                .id(1L)
                .content("Bài viết hay quá sếp ơi!")
                .createdAt(LocalDateTime.now())
                .likeCount(0L)
                .replyCount(0L)
                .parentId(null)
                .isLikedByCurrentUser(false)
                .author(authorResponse)
                .build();
    }

    // ==========================================
    // 1. TEST TẠO COMMENT (POST)
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void createComment_shouldReturn201AndCommentResponse() throws Exception {
        when(commentService.createComment(any(CommentRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockRequest))
                        .characterEncoding("UTF-8"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.content").value("Bài viết hay quá sếp ơi!"))
                .andExpect(jsonPath("$.author.studentCode").value("1412"));
    }

    // ==========================================
    // 2. TEST CẬP NHẬT COMMENT (PUT)
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void updateComment_shouldReturn200AndUpdatedComment() throws Exception {
        CommentRequest updateRequest = new CommentRequest();
        updateRequest.setContent("Đã sửa: Bài viết cực kỳ hay!");
        
        CommentResponse updatedResponse = CommentResponse.builder()
                .id(1L)
                .content("Đã sửa: Bài viết cực kỳ hay!")
                .author(mockResponse.getAuthor())
                .build();

        // Service chỉ nhận vào ID và Content dạng String
        when(commentService.updateComment(eq(1L), any(String.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Đã sửa: Bài viết cực kỳ hay!"));
    }

    // ==========================================
    // 3. TEST XÓA COMMENT (DELETE)
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void deleteComment_shouldReturn204NoContent() throws Exception {
        mockMvc.perform(delete("/api/comments/1"))
                .andExpect(status().isNoContent());

        // Xác minh service đã được gọi đúng 1 lần với ID = 1
        verify(commentService).deleteComment(1L);
    }

    // ==========================================
    // 4. TEST LẤY COMMENT CỦA POST (GET)
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void getCommentsByPost_shouldReturnPageOfComments() throws Exception {
        Page<CommentResponse> pageResponse = new PageImpl<>(List.of(mockResponse));
        when(commentService.getCommentsByPost(eq(100L), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/comments/post/100")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].content").value("Bài viết hay quá sếp ơi!"));
    }

    // ==========================================
    // 5. TEST LẤY DANH SÁCH REPLY (GET)
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void getReplies_shouldReturnPageOfReplies() throws Exception {
        CommentResponse replyResponse = CommentResponse.builder()
                .id(2L)
                .content("Chuẩn luôn bác!")
                .parentId(1L)
                .build();
        Page<CommentResponse> pageResponse = new PageImpl<>(List.of(replyResponse));
        
        when(commentService.getReplies(eq(1L), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/comments/1/replies")
                        .param("page", "0")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(2L))
                .andExpect(jsonPath("$.content[0].parentId").value(1L));
    }

    // ==========================================
    // 6. TEST THẢ TIM / BỎ THẢ TIM (TOGGLE LIKE)
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void toggleLike_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/comments/1/like"))
                .andExpect(status().isOk());

        // Xác minh service đã nhận lệnh toggle like cho comment ID = 1
        verify(commentService).toggleLike(1L);
    }
}