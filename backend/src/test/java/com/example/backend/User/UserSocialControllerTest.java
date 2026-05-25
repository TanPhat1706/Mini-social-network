package com.example.backend.User;

import com.example.backend.FriendRequest.FriendResponseDTO;
import com.example.backend.FriendRequest.FriendshipService;
import com.example.backend.Integration.BaseControllerTest;
import com.example.backend.Post.PostResponse;
import com.example.backend.Post.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = UserSocialController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
class UserSocialControllerTest extends BaseControllerTest {

    @MockBean
    private PostService postService;

    @MockBean
    private FriendshipService friendshipService;

    private Page<PostResponse> mockPostPage;
    private Page<FriendResponseDTO> mockFriendPage;

    @BeforeEach
    void setUp() {
        // 1. Dữ liệu giả cho Bài viết (Post)
        PostResponse mockPost = PostResponse.builder()
                .id(100L)
                .content("Hôm nay code Spring Boot vui quá!")
                .likeCount(50L)
                .build();
        mockPostPage = new PageImpl<>(List.of(mockPost));

        // 2. Dữ liệu giả cho Bạn bè (Friend)
        FriendResponseDTO mockFriend = FriendResponseDTO.builder()
                .id(2)
                .studentCode("SV_FRIEND")
                .fullName("Bạn Tốt Của Tôi")
                .build();
        mockFriendPage = new PageImpl<>(List.of(mockFriend));
    }

    // ==========================================
    // 1. TEST API LẤY DANH SÁCH BÀI VIẾT (POSTS)
    // ==========================================

    @Test
    @WithMockUser
    @DisplayName("Trả về danh sách bài viết phân trang khi truyền đúng mã sinh viên")
    void getPostsByStudentCode_whenValid_shouldReturn200AndPostPage() throws Exception {
        when(postService.getPostsByStudentCode(eq("SV001"), any(Pageable.class)))
                .thenReturn(mockPostPage);

        mockMvc.perform(get("/api/users/SV001/posts")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100))
                .andExpect(jsonPath("$.content[0].content").value("Hôm nay code Spring Boot vui quá!"))
                .andExpect(jsonPath("$.content[0].likeCount").value(50))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("Trả về HTTP 500 khi lấy bài viết của sinh viên không tồn tại")
    void getPostsByStudentCode_whenUserNotFound_shouldReturn500() throws Exception {
        when(postService.getPostsByStudentCode(eq("GHOST_SV"), any(Pageable.class)))
                .thenThrow(new RuntimeException("User with studentCode GHOST_SV not found"));

        mockMvc.perform(get("/api/users/GHOST_SV/posts"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Hệ thống đang gặp sự cố, vui lòng thử lại sau."));
    }

    // ==========================================
    // 2. TEST API LẤY DANH SÁCH BẠN BÈ (FRIENDS)
    // ==========================================

    @Test
    @WithMockUser
    @DisplayName("Trả về danh sách bạn bè phân trang khi truyền đúng mã sinh viên")
    void getFriendsByStudentCode_whenValid_shouldReturn200AndFriendPage() throws Exception {
        when(friendshipService.getFriendsByStudentCode(eq("SV001"), any(Pageable.class)))
                .thenReturn(mockFriendPage);

        mockMvc.perform(get("/api/users/SV001/friends")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].studentCode").value("SV_FRIEND"))
                .andExpect(jsonPath("$.content[0].fullName").value("Bạn Tốt Của Tôi"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("Trả về HTTP 500 khi lấy bạn bè của sinh viên không tồn tại")
    void getFriendsByStudentCode_whenUserNotFound_shouldReturn500() throws Exception {
        when(friendshipService.getFriendsByStudentCode(eq("GHOST_SV"), any(Pageable.class)))
                .thenThrow(new RuntimeException("User with studentCode GHOST_SV not found"));

        mockMvc.perform(get("/api/users/GHOST_SV/friends"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Hệ thống đang gặp sự cố, vui lòng thử lại sau."));
    }
}