package com.example.backend.Admin;

import com.example.backend.Integration.BaseControllerTest;
import com.example.backend.Post.PostRepository;
import com.example.backend.Post.PostResponse;
import com.example.backend.Post.PostService;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(
        value = AdminController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
class AdminControllerTest extends BaseControllerTest {

    @MockBean
    private PostService postService;

    @MockBean
    private PostRepository postRepository;

    @MockBean
    private UserRepository userRepository;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1);
        mockUser.setFullName("Lê Hồng Phát");
        mockUser.setStudentCode("1412");
        mockUser.setRole("STUDENT");
        mockUser.setActive(true);
    }

    // ==========================================
    // 1. DASHBOARD & STATS
    // ==========================================
    @Test
    @WithMockUser(roles = "ADMIN")
    void getAdminDashboard_shouldReturnString() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string("Admin Dashboard"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserStats_shouldReturnMap() throws Exception {
        when(userRepository.countByActive(true)).thenReturn(10L);
        when(userRepository.countByActive(false)).thenReturn(2L);

        mockMvc.perform(get("/api/admin/users-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeUsers").value(10))
                .andExpect(jsonPath("$.pendingUsers").value(2));
    }

    // ==========================================
    // 2. QUẢN LÝ BÀI VIẾT (POSTS)
    // ==========================================
    @Test
    @WithMockUser(roles = "ADMIN")
    void approvePost_shouldReturnSuccess() throws Exception {
        doNothing().when(postService).approvePost(100L);

        mockMvc.perform(post("/api/admin/approve-post/100"))
                .andExpect(status().isOk())
                .andExpect(content().string("Duyệt bài thành công!")); // Đảm bảo AdminController trả về đúng chuỗi này
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllPosts_shouldReturnPage() throws Exception {
        // SỬA: Tạo Mock cho Pageable và bọc List thành PageImpl
        PostResponse pr = PostResponse.builder().id(100L).content("Test Admin").build();
        Page<PostResponse> postPage = new PageImpl<>(List.of(pr));
        
        when(postService.getAllPostsForAdmin(any(Pageable.class))).thenReturn(postPage);

        mockMvc.perform(get("/api/admin/posts")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                // Lưu ý: Nếu AdminController trả về Page, cấu trúc JSON sẽ có content: [ {id: 100} ]
                // Nếu Controller của bạn không map lại mà ném thẳng đối tượng Page ra ngoài:
                .andExpect(jsonPath("$.content[0].id").value(100L));
                // Nếu Controller tự trích xuất List ra thì dùng: jsonPath("$[0].id").value(100L)
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deletePost_shouldReturnSuccess() throws Exception {
        // SỬA: Bổ sung giả lập dữ liệu trả về cho hàm deletePost
        when(postService.deletePost(100L)).thenReturn("Xóa bài thành công.");

        mockMvc.perform(delete("/api/admin/delete-post/100")
                        .param("reason", "Vi phạm tiêu chuẩn cộng đồng"))
                .andExpect(status().isOk())
                .andExpect(content().string("Xóa bài thành công."));

        verify(postService).deletePost(100L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void countPosts_shouldReturnNumber() throws Exception {
        when(postRepository.countPosts()).thenReturn(50L);

        mockMvc.perform(get("/api/admin/posts-count"))
                .andExpect(status().isOk())
                .andExpect(content().string("50"));
    }

    // ==========================================
    // 3. QUẢN LÝ NGƯỜI DÙNG (USERS)
    // ==========================================
    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_shouldReturnList() throws Exception {
        when(userRepository.findByRoleIn(anyList())).thenReturn(List.of(mockUser));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentCode").value("1412"))
                .andExpect(jsonPath("$[0].fullName").value("Lê Hồng Phát"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void banUser_shouldReturnSuccess() throws Exception {
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        mockMvc.perform(post("/api/admin/ban-user/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Đã khóa tài khoản")));
        
        verify(userRepository).save(any(User.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void approveUser_shouldReturnSuccess() throws Exception {
        mockUser.setActive(false);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        mockMvc.perform(post("/api/admin/approve-user/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Đã duyệt/mở khóa")));

        verify(userRepository).save(any(User.class));
    }
}