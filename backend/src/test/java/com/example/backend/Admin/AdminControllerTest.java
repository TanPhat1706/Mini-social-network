package com.example.backend.Admin;

import com.example.backend.Integration.BaseControllerTest;
import com.example.backend.Post.PostRepository;
import com.example.backend.Post.PostResponse;
import com.example.backend.Post.PostService;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
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
                .andExpect(content().string("Duyệt bài thành công!")); 
    }

    // 🟢 MỚI: TEST NHÁNH CATCH LỖI DUYỆT BÀI
    @Test
    @WithMockUser(roles = "ADMIN")
    void approvePost_whenException_shouldReturn400() throws Exception {
        doThrow(new RuntimeException("Bài viết không tồn tại")).when(postService).approvePost(999L);

        mockMvc.perform(post("/api/admin/approve-post/999"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Lỗi: Bài viết không tồn tại"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllPosts_shouldReturnPage() throws Exception {
        PostResponse pr = PostResponse.builder().id(100L).content("Test Admin").build();
        Page<PostResponse> postPage = new PageImpl<>(List.of(pr));
        
        when(postService.getAllPostsForAdmin(any(Pageable.class))).thenReturn(postPage);

        mockMvc.perform(get("/api/admin/posts")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100L));
    }

    // 🟢 MỚI: TEST NHÁNH CATCH LỖI LẤY DANH SÁCH BÀI VIẾT
    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllPosts_whenException_shouldReturn400() throws Exception {
        when(postService.getAllPostsForAdmin(any(Pageable.class))).thenThrow(new RuntimeException("DB Connection Error"));

        mockMvc.perform(get("/api/admin/posts"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Failed to retrieve posts: DB Connection Error"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deletePost_shouldReturnSuccess() throws Exception {
        doNothing().when(postService).deletePost(100L);

        mockMvc.perform(delete("/api/admin/delete-post/100")
                        .param("reason", "Vi phạm tiêu chuẩn cộng đồng"))
                .andExpect(status().isOk())
                .andExpect(content().string("Xóa bài thành công."));

        verify(postService).deletePost(100L);
    }

    // 🟢 MỚI: TEST NHÁNH CATCH LỖI XÓA BÀI VIẾT
    @Test
    @WithMockUser(roles = "ADMIN")
    void deletePost_whenException_shouldReturn400() throws Exception {
        doThrow(new RuntimeException("Không tìm thấy")).when(postService).deletePost(999L);

        mockMvc.perform(delete("/api/admin/delete-post/999"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Lỗi xóa bài: Không tìm thấy"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void countPosts_shouldReturnNumber() throws Exception {
        when(postRepository.countPosts()).thenReturn(50L);

        mockMvc.perform(get("/api/admin/posts-count"))
                .andExpect(status().isOk())
                .andExpect(content().string("50"));
    }

    // 🟢 MỚI: TEST NHÁNH CATCH LỖI ĐẾM BÀI VIẾT
    @Test
    @WithMockUser(roles = "ADMIN")
    void countPosts_whenException_shouldReturn400() throws Exception {
        when(postRepository.countPosts()).thenThrow(new RuntimeException("Lỗi máy chủ"));

        mockMvc.perform(get("/api/admin/posts-count"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("0")); // Logic catch return 0L
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

    // 🟢 MỚI: TEST NHÁNH CATCH LỖI LẤY DANH SÁCH NGƯỜI DÙNG
    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_whenException_shouldReturn400() throws Exception {
        when(userRepository.findByRoleIn(anyList())).thenThrow(new RuntimeException("DB Error"));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Lỗi tải danh sách người dùng: DB Error"));
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

    // 🟢 MỚI: TEST NHÁNH LAMBDA KHÔNG TÌM THẤY USER KHI KHÓA TÀI KHOẢN
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Ban User - Bắn lỗi 400 do không tìm thấy User")
    void banUser_whenUserNotFound_shouldReturn400() throws Exception {
        when(userRepository.findById(999)).thenReturn(Optional.empty()); // Kích hoạt orElseThrow

        mockMvc.perform(post("/api/admin/ban-user/999"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Lỗi: Không tìm thấy người dùng!"));
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

    // 🟢 MỚI: TEST NHÁNH LAMBDA KHÔNG TÌM THẤY USER KHI DUYỆT TÀI KHOẢN
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Approve User - Bắn lỗi 400 do không tìm thấy User")
    void approveUser_whenUserNotFound_shouldReturn400() throws Exception {
        when(userRepository.findById(999)).thenReturn(Optional.empty()); // Kích hoạt orElseThrow

        mockMvc.perform(post("/api/admin/approve-user/999"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Lỗi: Không tìm thấy người dùng!"));
    }
}