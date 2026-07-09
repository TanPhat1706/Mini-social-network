package com.example.backend.Presence;

import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PresenceService presenceService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1);
        mockUser.setStudentCode("1412");
        mockUser.setFullName("Lê Hồng Phát");
        mockUser.setLastActiveAt(LocalDateTime.of(2026, 6, 18, 10, 0));
    }

    @Test
    @DisplayName("Khi gọi markUserAsOnline -> Cập nhật RAM, user chuyển thành ONLINE")
    void markUserAsOnline_shouldSetUserAsOnline() {
        presenceService.markUserAsOnline("1412");

        UserPresenceDTO presence = presenceService.getUserPresence("1412");

        assertTrue(presence.isOnline());
        assertNotNull(presence.getLastSeen());
    }

    @Test
    @DisplayName("Khi gọi markUserAsOffline và User TỒN TẠI -> Xóa khỏi RAM, cập nhật lastActiveAt xuống DB")
    void markUserAsOffline_whenUserExists_shouldUpdateDB() {
        // Đưa user vào trạng thái online trước
        presenceService.markUserAsOnline("1412");
        assertTrue(presenceService.getUserPresence("1412").isOnline());

        // Giả lập DB trả về user
        when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(mockUser));

        // Gọi hàm test
        presenceService.markUserAsOffline("1412");

        // 1. Phải thành Offline (xóa khỏi RAM)
        assertFalse(presenceService.getUserPresence("1412").isOnline());

        // 2. Phải gọi save() để lưu DB
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());

        // 3. lastActiveAt phải được cập nhật thành thời điểm hiện tại (mới hơn thời điểm cũ)
        User savedUser = userCaptor.getValue();
        assertTrue(savedUser.getLastActiveAt().isAfter(LocalDateTime.of(2026, 6, 18, 10, 0)));
    }

    @Test
    @DisplayName("Khi gọi markUserAsOffline mà User KHÔNG TỒN TẠI -> Xóa RAM, không lỗi, không save DB")
    void markUserAsOffline_whenUserNotFound_shouldNotThrowException() {
        when(userRepository.findByStudentCode("GHOST")).thenReturn(Optional.empty());

        // Gọi hàm test
        assertDoesNotThrow(() -> presenceService.markUserAsOffline("GHOST"));

        // Đảm bảo không có lệnh save nào gọi tới DB
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("getUserPresence khi Offline VÀ có trong DB -> Trả về isOnline = false và lấy lastSeen từ DB")
    void getUserPresence_whenOfflineAndInDB_shouldReturnDBLastSeen() {
        // Mock DB
        when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(mockUser));

        UserPresenceDTO presence = presenceService.getUserPresence("1412");

        assertFalse(presence.isOnline());
        assertEquals(LocalDateTime.of(2026, 6, 18, 10, 0), presence.getLastSeen());
    }

    @Test
    @DisplayName("getUserPresence khi Offline VÀ KHÔNG có trong DB -> Trả về isOnline = false, lastSeen = null")
    void getUserPresence_whenOfflineAndNotInDB_shouldReturnNullLastSeen() {
        when(userRepository.findByStudentCode("GHOST")).thenReturn(Optional.empty());

        UserPresenceDTO presence = presenceService.getUserPresence("GHOST");

        assertFalse(presence.isOnline());
        assertNull(presence.getLastSeen());
    }
}