package com.example.backend.Notification;

import com.example.backend.Integration.BaseControllerTest;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = NotificationController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
class NotificationControllerTest extends BaseControllerTest {

    @MockBean
    private NotificationRepository notificationRepository;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private UserRepository userRepository;

    private User currentUser;
    private Notification mockNotification;
    private NotificationDTO mockDTO;

    @BeforeEach
    void setUp() {
        // 1. Mock User đang đăng nhập
        currentUser = new User();
        currentUser.setId(1);
        currentUser.setStudentCode("1412");
        currentUser.setFullName("Lê Hồng Phát");

        // 2. Mock Entity Notification (Lấy từ DB lên)
        mockNotification = Notification.builder()
                .id(100L)
                .receiver(currentUser)
                .isRead(false)
                .build();

        // 3. Mock DTO (Sau khi được Service map)
        mockDTO = NotificationDTO.builder()
                .id(100L)
                .message("đã thích bài viết của bạn.")
                .isRead(false)
                .targetUrl("/posts/50")
                .build();
    }

    // ==========================================
    // 1. TEST LẤY DANH SÁCH THÔNG BÁO (GET)
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    @DisplayName("Lấy danh sách thông báo thành công -> Trả về mảng DTO")
    void getNotifications_shouldReturnListOfNotificationDTOs() throws Exception {
        // Giả lập lấy User từ Security Context
        when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(currentUser));
        
        // Giả lập Repository trả về danh sách Notification
        when(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(mockNotification));
                
        // Giả lập Service chuyển Entity -> DTO
        when(notificationService.mapToDTO(any(Notification.class))).thenReturn(mockDTO);

        // Thực thi request
        mockMvc.perform(get("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")) 
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100L))
                .andExpect(jsonPath("$[0].message").value("đã thích bài viết của bạn."))
                .andExpect(jsonPath("$[0].read").value(false)); 

        // Xác minh các tầng dưới đã được gọi đúng luồng logic
        verify(userRepository).findByStudentCode("1412");
        verify(notificationRepository).findByReceiverIdOrderByCreatedAtDesc(1L);
        verify(notificationService).mapToDTO(mockNotification);
    }

    // ==========================================
    // 2. TEST ĐÁNH DẤU ĐÃ ĐỌC (PUT)
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    @DisplayName("Đánh dấu tất cả là đã đọc -> Gọi Service và trả về 200 OK")
    void markAllAsRead_shouldCallServiceAndReturn200() throws Exception {
        when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(currentUser));

        mockMvc.perform(put("/api/notifications/read-all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(userRepository).findByStudentCode("1412");
        verify(notificationService).markAllAsRead(1L);
    }

    // ==========================================
    // 3. TEST NGOẠI LỆ KHI KHÔNG TÌM THẤY USER
    // ==========================================
    @Test
    @WithMockUser(username = "ghost")
    @DisplayName("Bắn lỗi RuntimeException khi token hợp lệ nhưng DB không có User")
    void getCurrentUser_whenUserNotFound_shouldThrowException() throws Exception {
        // Ép trả về Optional.empty() để chạy vào nhánh .orElseThrow()
        when(userRepository.findByStudentCode("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/notifications"))
                // Kiểm tra xem Exception được bắn ra từ Controller có đúng class và message không
                .andExpect(result -> org.junit.jupiter.api.Assertions
                        .assertTrue(result.getResolvedException() instanceof RuntimeException))
                .andExpect(result -> org.junit.jupiter.api.Assertions
                        .assertEquals("Không tìm thấy người dùng (Token không hợp lệ?)", 
                                result.getResolvedException().getMessage()));
        
        verify(userRepository).findByStudentCode("ghost");
    }
}