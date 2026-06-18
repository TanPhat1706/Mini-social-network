package com.example.backend.Post;

import com.example.backend.Integration.BaseControllerTest;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import com.example.backend.User.UserResponse;
import com.example.backend.Enum.Visibility;
import com.example.backend.Enum.ReactionType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        value = PostController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
class PostControllerTest extends BaseControllerTest {

    @MockBean
    private PostService postService;

    @MockBean
    private PostRepository postRepository;

    @MockBean
    private UserRepository userRepository;

    private User currentUser;
    private PostResponse mockPostResponse;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1);
        currentUser.setStudentCode("1412");

        UserResponse authorDto = UserResponse.builder()
                .id(1)
                .fullName("Lê Hồng Phát")
                .studentCode("1412")
                .build();

        mockPostResponse = PostResponse.builder()
                .id(100L)
                .content("Nội dung bài viết test")
                .visibility(Visibility.PUBLIC)
                .author(authorDto)
                .build();
    }

    // ==========================================
    // 1. TEST LẤY BÀI VIẾT CỦA TÔI (GET)
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void getMyPosts_shouldReturnPage() throws Exception {
        Page<PostResponse> page = new PageImpl<>(List.of(mockPostResponse));
        when(postService.getPostsByAuthor(anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/posts/my-posts")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100L));
    }

    // ==========================================
    // 2. TEST TẠO BÀI VIẾT (MULTIPART POST)
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void createPost_shouldReturn201() throws Exception {
        MockMultipartFile file = new MockMultipartFile("mediaFiles", "test.jpg", 
                MediaType.IMAGE_JPEG_VALUE, "image data".getBytes());
        
        when(postService.createPost(any(PostRequest.class))).thenReturn(mockPostResponse);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/posts")
                        .file(file)
                        .param("content", "Nội dung bài viết test")
                        .param("visibility", "PUBLIC")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Nội dung bài viết test"));
    }

    // ==========================================
    // 3. TEST UPLOAD MEDIA ĐƠN LẺ LÊN S3
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    @DisplayName("Upload file lẻ lên S3 thành công -> Trả về URL")
    void uploadMedia_shouldReturnUrlAnd201() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "image.png",
                MediaType.IMAGE_PNG_VALUE, "png data".getBytes());

        // Giả lập service upload thành công và trả về URL
        when(postService.uploadFileToS3(any(MultipartFile.class))).thenReturn("https://aws.s3.com/image.png");

        mockMvc.perform(multipart("/api/posts/upload-media")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(content().string("https://aws.s3.com/image.png"));
    }

    // ==========================================
    // 4. TEST CẬP NHẬT BÀI VIẾT (MULTIPART PUT)
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void updatePost_shouldReturn200() throws Exception {
        MockMultipartFile file = new MockMultipartFile("mediaFiles", "update.jpg", 
                MediaType.IMAGE_JPEG_VALUE, "updated data".getBytes());

        when(postService.updatePost(eq(100L), any(PostRequest.class))).thenReturn(mockPostResponse);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/posts/100")
                        .file(file)
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .param("content", "Nội dung đã sửa")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());
    }

    // ==========================================
    // 5. TEST XÓA BÀI VIẾT (DELETE)
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void deletePost_shouldReturnSuccessString() throws Exception {
        doNothing().when(postService).deletePost(100L);

        mockMvc.perform(delete("/api/posts/100"))
                .andExpect(status().isNoContent());
    }

    // ==========================================
    // 6. TEST THẢ CẢM XÚC (POST REACTION)
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void reactToPost_shouldReturnOk() throws Exception {
        // Giả lập DTO ReactRequest được Client gửi lên dưới dạng JSON
        String requestJson = "{\"reactionType\":\"LIKE\"}";

        mockMvc.perform(post("/api/posts/100/react")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        // Verify rằng Controller đã gọi đúng hàm reactToPost của Service với tham số chuẩn xác
        verify(postService).reactToPost(100L, ReactionType.LIKE);
    }

    // ==========================================
    // 7. TEST CHIA SẺ BÀI VIẾT (POST SHARE)
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void sharePost_shouldReturnNewPost() throws Exception {
        PostRequest shareRequest = new PostRequest();
        shareRequest.setContent("Cảm nghĩ khi chia sẻ");

        when(postService.sharePost(eq(100L), any(PostRequest.class))).thenReturn(mockPostResponse);

        mockMvc.perform(post("/api/posts/100/share")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shareRequest)))
                .andExpect(status().isOk());
    }

    // ==========================================
    // 8. TEST LẤY CHI TIẾT BÀI VIẾT (GET BY ID)
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void getPostById_shouldReturnPost() throws Exception {
        Post postEntity = new Post();
        postEntity.setId(100L);
        postEntity.setAuthor(currentUser);

        when(postRepository.findById(100L)).thenReturn(Optional.of(postEntity));
        when(postService.mapToPostResponse(any(Post.class))).thenReturn(mockPostResponse);

        mockMvc.perform(get("/api/posts/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L));
    }

    @Test
    @WithMockUser(username = "1412")
    @DisplayName("Ném lỗi RuntimeException khi bài viết không tồn tại")
    void getPostById_whenNotFound_shouldThrowException() throws Exception {
        when(postRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/posts/999"))
                // Kiểm tra exception được ném ra và catch được nguyên nhân gốc
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof RuntimeException))
                .andExpect(result -> assertEquals("Bài viết không tồn tại", result.getResolvedException().getMessage()));
    }
}