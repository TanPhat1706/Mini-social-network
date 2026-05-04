package com.example.backend.Post;

import com.example.backend.Integration.BaseControllerTest;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
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

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = FeedController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
class FeedControllerTest extends BaseControllerTest {

    @MockBean
    private FeedService feedService;

    @MockBean
    private UserRepository userRepository;

    private User currentUser;
    private PostResponse mockPostResponse;

    @BeforeEach
    void setUp() {
        // 1. Tạo User giả lập (cho Security Context)
        currentUser = new User();
        currentUser.setId(1);
        currentUser.setStudentCode("1412");

        // 2. Tạo UserResponse giả lập
        UserResponse authorResponse = UserResponse.builder()
                .id(1)
                .fullName("Lê Hồng Phát")
                .studentCode("1412")
                .build();

        // 3. Tạo PostResponse - 🟢 CHỈNH LẠI CÁCH SET LIKED
        mockPostResponse = PostResponse.builder()
                .id(500L)
                .content("Hôm nay trời đẹp, đi code thôi sếp ơi!")
                .author(authorResponse)
                .build();

        // Gọi setter thủ công vì trường này không nằm trong Builder
        mockPostResponse.setLikedByCurrentUser(true);
    }

    // ==========================================
    // 1. TEST LẤY BẢNG TIN THÀNH CÔNG (GET /api/feed)
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void getNewsFeed_shouldReturnPageOfPosts() throws Exception {
        // Mock tìm thấy user
        when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(currentUser));

        // Mock dữ liệu Feed
        Page<PostResponse> page = new PageImpl<>(List.of(mockPostResponse));
        when(feedService.getNewsFeed(eq(1), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/feed")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // 🟢 ĐÃ SỬA: Phải dùng $.content[0].id vì đây là Page object
                .andExpect(jsonPath("$.content[0].id").value(500L))
                .andExpect(jsonPath("$.content[0].content").value("Hôm nay trời đẹp, đi code thôi sếp ơi!"));
    }

    // ==========================================
    // 2. TEST KHI KHÔNG CÓ BÀI VIẾT NÀO
    // ==========================================
    @Test
    @WithMockUser(username = "anonymousUser") // 🟢 MẸO: Ép nó vào trạng thái ẩn danh để test 401
    void getNewsFeed_unauthenticated_shouldFail() throws Exception {
        mockMvc.perform(get("/api/feed"))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // 3. TEST KHI CHƯA ĐĂNG NHẬP (Lỗi 401/403 tùy config)
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void getNewsFeed_whenEmpty_shouldReturnEmptyPage() throws Exception {
        when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(currentUser));
        
        // 🟢 ĐÃ SỬA: Đảm bảo trả về Page rỗng nhưng đúng cấu trúc
        Page<PostResponse> emptyPage = new PageImpl<>(List.of());
        when(feedService.getNewsFeed(eq(1), any(Pageable.class))).thenReturn(emptyPage);

        mockMvc.perform(get("/api/feed"))
                .andExpect(status().isOk())
                // Page rỗng vẫn có trường "content" là mảng rỗng []
                .andExpect(jsonPath("$.content").isEmpty());
    }
}