package com.example.backend.Post;

import com.example.backend.Integration.BaseControllerTest;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import com.example.backend.User.UserResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
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

                // 3. Tạo PostResponse
                mockPostResponse = PostResponse.builder()
                                .id(500L)
                                .content("Hôm nay trời đẹp, đi code thôi sếp ơi!")
                                .author(authorResponse)
                                .currentUserReaction("LIKE")
                                .build();
        }

        // ==========================================
        // 1. TEST LẤY BẢNG TIN THÀNH CÔNG (GET /api/feed)
        // ==========================================
        @Test
        @WithMockUser(username = "1412")
        void getNewsFeed_shouldReturnCursorPage() throws Exception {
                when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(currentUser));

                CursorPageResponse<PostResponse> cursorResponse = new CursorPageResponse<>(
                                List.of(mockPostResponse),
                                500L, // nextCursor
                                false // hasNext
                );

                when(feedService.getNewsFeed(eq(1), nullable(Long.class), anyInt()))
                                .thenReturn(cursorResponse);

                mockMvc.perform(get("/api/feed")
                                .param("size", "10")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].id").value(500L))
                                .andExpect(jsonPath("$.content[0].content")
                                                .value("Hôm nay trời đẹp, đi code thôi sếp ơi!"))
                                .andExpect(jsonPath("$.nextCursor").value(500L))
                                .andExpect(jsonPath("$.hasNext").value(false));
        }

        // ==========================================
        // 2. TEST KHI KHÔNG ĐĂNG NHẬP (Nhánh "anonymousUser")
        // ==========================================
        @Test
        @WithMockUser(username = "anonymousUser")
        void getNewsFeed_unauthenticated_shouldFail() throws Exception {
                mockMvc.perform(get("/api/feed"))
                                .andExpect(status().isUnauthorized());
        }

        // ==========================================
        // 3. TEST KHI BẢNG TIN TRỐNG
        // ==========================================
        @Test
        @WithMockUser(username = "1412")
        void getNewsFeed_whenEmpty_shouldReturnEmptyCursorPage() throws Exception {
                when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(currentUser));

                CursorPageResponse<PostResponse> emptyCursorResponse = new CursorPageResponse<>(
                                List.of(), null, false);

                when(feedService.getNewsFeed(eq(1), nullable(Long.class), anyInt()))
                                .thenReturn(emptyCursorResponse);

                mockMvc.perform(get("/api/feed"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isEmpty())
                                .andExpect(jsonPath("$.nextCursor").doesNotExist())
                                .andExpect(jsonPath("$.hasNext").value(false));
        }

        // ==========================================
        // 4. TEST TÌM USER THEO EMAIL (Nhánh fallback orElseGet)
        // ==========================================
        @Test
        @WithMockUser(username = "phat@example.com")
        @DisplayName("Nếu tìm bằng StudentCode thất bại, chuyển sang tìm bằng Email")
        void getNewsFeed_whenFoundByEmail_shouldReturnOk() throws Exception {
                when(userRepository.findByStudentCode("phat@example.com")).thenReturn(Optional.empty());
                when(userRepository.findByEmail("phat@example.com")).thenReturn(Optional.of(currentUser));

                CursorPageResponse<PostResponse> emptyCursorResponse = new CursorPageResponse<>(List.of(), null, false);
                when(feedService.getNewsFeed(eq(1), nullable(Long.class), anyInt())).thenReturn(emptyCursorResponse);

                mockMvc.perform(get("/api/feed"))
                                .andExpect(status().isOk());
        }

        // ==========================================
        // 5. TEST USER BỊ XÓA KHỎI DB (Nhánh currentUser == null)
        // ==========================================
        @Test
        @WithMockUser(username = "ghost")
        @DisplayName("Token hợp lệ nhưng User đã bị xóa khỏi DB -> 401")
        void getNewsFeed_whenUserNotFoundInDb_shouldReturn401() throws Exception {
                when(userRepository.findByStudentCode("ghost")).thenReturn(Optional.empty());
                when(userRepository.findByEmail("ghost")).thenReturn(Optional.empty());

                mockMvc.perform(get("/api/feed"))
                                .andExpect(status().isUnauthorized());
        }

        // ==========================================
        // 6. TEST AUTHENTICATION LÀ NULL (Nhánh auth == null)
        // ==========================================
        @Test
        @DisplayName("SecurityContext không có Authentication -> 401")
        void getNewsFeed_whenAuthIsNull_shouldReturn401() throws Exception {
                // Clear context để mô phỏng hoàn toàn không có đăng nhập (auth == null)
                SecurityContextHolder.clearContext();

                mockMvc.perform(get("/api/feed"))
                                .andExpect(status().isUnauthorized());
        }
}