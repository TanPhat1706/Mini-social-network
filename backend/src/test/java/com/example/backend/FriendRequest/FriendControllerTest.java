package com.example.backend.FriendRequest;

import com.example.backend.Integration.BaseControllerTest;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = FriendController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
class FriendControllerTest extends BaseControllerTest {

    @MockBean
    private FriendshipService friendshipService;

    @MockBean
    private UserRepository userRepository;

    private User currentUser;
    private User targetUser;

    @BeforeEach
    void setUp() {
        // Mock người dùng hiện tại (Current User)
        currentUser = new User();
        currentUser.setId(1);
        currentUser.setStudentCode("1412");
        currentUser.setFullName("Lê Hồng Phát");

        // Mock đối tượng đích (Target User)
        targetUser = new User();
        targetUser.setId(2);
        targetUser.setStudentCode("GUEST002");
        targetUser.setFullName("Người Ấy");
    }

    /**
     * 🟢 MẸO: Hàm này dùng để Mock logic xác thực getCurrentUserId() trong Controller
     * Nó ép Controller khi chạy sẽ nhận diện được "1412" là User mang ID số 1.
     */
    private void mockSecurityContextFallback() {
        when(userRepository.findByEmail("1412")).thenReturn(Optional.empty());
        when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(currentUser));
    }

    // ==========================================
    // 1. TEST GỬI LỜI MỜI (POST /add/{id})
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void sendRequest_shouldReturnSuccessMessage() throws Exception {
        mockSecurityContextFallback();
        when(friendshipService.sendRequest(eq(1), eq(2))).thenReturn("Đã gửi lời mời");

        mockMvc.perform(post("/api/auth/friends/add/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã gửi lời mời"));

        verify(friendshipService).sendRequest(1, 2);
    }

    // ==========================================
    // 2. TEST CHẤP NHẬN LỜI MỜI (POST /accept/{id})
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void acceptRequest_shouldReturnSuccessMessage() throws Exception {
        mockSecurityContextFallback();
        when(friendshipService.acceptRequest(eq(1), eq(2))).thenReturn("Đã trở thành bạn bè");

        mockMvc.perform(post("/api/auth/friends/accept/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã trở thành bạn bè"));
    }

    // ==========================================
    // 3. TEST HỦY/XÓA BẠN BÈ (DELETE /remove/{id})
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void removeFriendship_shouldReturnSuccessMessage() throws Exception {
        mockSecurityContextFallback();
        when(friendshipService.removeFriendship(eq(1), eq(2))).thenReturn("Đã hủy kết bạn");

        mockMvc.perform(delete("/api/auth/friends/remove/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã hủy kết bạn"));
    }

    // ==========================================
    // 4. TEST TRẠNG THÁI BẠN BÈ (GET /status/{id})
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void getStatus_shouldReturnFriendshipDTO() throws Exception {
        mockSecurityContextFallback();
        
        FriendshipDTO dto = new FriendshipDTO("PENDING", 1);
        when(friendshipService.getFriendshipStatus(eq(1), eq(2))).thenReturn(dto);

        mockMvc.perform(get("/api/auth/friends/status/2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.actionUserId").value(1));
    }

    // ==========================================
    // 5. TEST GỢI Ý KẾT BẠN (GET /suggested)
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void getSuggested_shouldReturnPageOfUsers() throws Exception {
        mockSecurityContextFallback();
        
        Page<User> suggestedPage = new PageImpl<>(List.of(targetUser));
        when(friendshipService.getSuggestedFriends(eq(1), eq(0), eq(5))).thenReturn(suggestedPage);

        mockMvc.perform(get("/api/auth/friends/suggested")
                        .param("page", "0")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(2))
                .andExpect(jsonPath("$.content[0].studentCode").value("GUEST002"));
    }

    // ==========================================
    // 6. TEST DANH SÁCH LỜI MỜI ĐẾN (GET /requests)
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void getRequests_shouldReturnListOfUsers() throws Exception {
        mockSecurityContextFallback();
        
        when(friendshipService.getFriendRequests(eq(1))).thenReturn(List.of(targetUser));

        mockMvc.perform(get("/api/auth/friends/requests")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].studentCode").value("GUEST002"));
    }

    // ==========================================
    // 7. TEST DANH SÁCH BẠN BÈ (GET /list)
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void getFriendsList_shouldReturnListOfUsers() throws Exception {
        mockSecurityContextFallback();
        
        when(friendshipService.getUserFriends(eq(1))).thenReturn(List.of(targetUser));

        mockMvc.perform(get("/api/auth/friends/list")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].fullName").value("Người Ấy"));
    }
}