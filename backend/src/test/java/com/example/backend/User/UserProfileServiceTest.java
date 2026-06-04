package com.example.backend.User;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    @DisplayName("Ném ngoại lệ khi User mục tiêu không tồn tại hoặc không Active")
    void getProfile_whenTargetUserNotFoundOrInactive_shouldThrowException() {
        // Arrange
        String targetStudentCode = "GHOST_SV";
        when(userRepository.findByStudentCodeAndActive(targetStudentCode, true))
                .thenReturn(Optional.empty());

        // Act & Assert
        UserProfileNotFoundException ex = assertThrows(UserProfileNotFoundException.class, 
                () -> userProfileService.getProfile("ANY_VIEWER", targetStudentCode));
                
        assertEquals("Target user not found or inactive", ex.getMessage());
        verify(userRepository, times(1)).findByStudentCodeAndActive(targetStudentCode, true);
    }

    @Test
    @DisplayName("Trả về Profile với isSelfProfile = true khi người xem chính là chủ tài khoản")
    void getProfile_whenViewerIsTarget_shouldReturnSelfProfile() {
        // Arrange
        String studentCode = "SV001";
        User mockUser = createMockUser(studentCode);
        
        when(userRepository.findByStudentCodeAndActive(studentCode, true))
                .thenReturn(Optional.of(mockUser));

        // Act
        UserProfileResponse response = userProfileService.getProfile(studentCode, studentCode);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSelfProfile()); // Kịch bản quan trọng nhất
        
        // Kiểm tra format ngày tháng (từ 15/05/2023 -> 05/2023)
        assertEquals("05/2023", response.joinedAt());
        
        // Kiểm tra mapping các trường dữ liệu
        assertEquals(1, response.userId());
        assertEquals("SV001", response.username());
        assertEquals("Nguyễn Văn A", response.fullName());
        assertEquals("nva@example.com", response.email());
        assertEquals("/avatar.png", response.avatarUrl());
        assertEquals("/cover.png", response.coverPhotoUrl());
        assertEquals("Hello World", response.bio());
        assertEquals("K17CNTT", response.className());
        assertEquals("2023-05-15T10:30", response.createdAt());
        assertEquals("frame_gold", response.currentAvatarFrame());
        assertEquals("#FF0000", response.currentNameColor());
    }

    @Test
    @DisplayName("Trả về Profile với isSelfProfile = false khi người xem khác chủ tài khoản")
    void getProfile_whenViewerIsNotTarget_shouldReturnOtherProfile() {
        // Arrange
        String viewerCode = "SV_KHACH";
        String targetCode = "SV001";
        User mockUser = createMockUser(targetCode);

        when(userRepository.findByStudentCodeAndActive(targetCode, true))
                .thenReturn(Optional.of(mockUser));

        // Act
        UserProfileResponse response = userProfileService.getProfile(viewerCode, targetCode);

        // Assert
        assertNotNull(response);
        assertFalse(response.isSelfProfile()); // Khác mã SV -> isSelfProfile = false
    }

    @Test
    @DisplayName("Trả về Profile với isSelfProfile = false khi người xem là null (chưa đăng nhập)")
    void getProfile_whenViewerIsNull_shouldReturnOtherProfile() {
        // Arrange
        String targetCode = "SV001";
        User mockUser = createMockUser(targetCode);

        when(userRepository.findByStudentCodeAndActive(targetCode, true))
                .thenReturn(Optional.of(mockUser));

        // Act
        UserProfileResponse response = userProfileService.getProfile(null, targetCode);

        // Assert
        assertNotNull(response);
        assertFalse(response.isSelfProfile()); // viewer null -> isSelfProfile = false
    }

    // ==========================================
    // HELPER METHOD: Khởi tạo User mẫu
    // ==========================================
    private User createMockUser(String studentCode) {
        User user = new User();
        user.setId(1);
        user.setStudentCode(studentCode);
        user.setFullName("Nguyễn Văn A");
        user.setEmail("nva@example.com");
        user.setAvatarUrl("/avatar.png");
        user.setCoverPhotoUrl("/cover.png");
        user.setBio("Hello World");
        user.setClassName("K17CNTT");
        user.setCurrentAvatarFrame("frame_gold");
        user.setCurrentNameColor("#FF0000");
        // Giả lập ngày tạo tài khoản là 15/05/2023
        user.setCreatedAt(LocalDateTime.of(2023, 5, 15, 10, 30)); 
        return user;
    }
}
