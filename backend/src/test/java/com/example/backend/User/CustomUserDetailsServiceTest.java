package com.example.backend.User;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("Ném ngoại lệ UsernameNotFoundException khi không tìm thấy User trong Database")
    void loadUserByUsername_whenUserNotFound_shouldThrowException() {
        // Arrange
        String notFoundStudentCode = "GHOST_SV";
        when(userRepository.findByStudentCode(notFoundStudentCode)).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername(notFoundStudentCode));

        assertEquals("User not found", ex.getMessage());
        
        // Đảm bảo hàm findByStudentCode thực sự được gọi
        verify(userRepository, times(1)).findByStudentCode(notFoundStudentCode);
    }

    @Test
    @DisplayName("Trả về UserDetails hợp lệ và map đúng quyền với tiền tố ROLE_ khi tìm thấy User")
    void loadUserByUsername_whenUserFound_shouldReturnUserDetailsAndMapRole() {
        // Arrange
        String studentCode = "SV001";
        
        User mockUser = new User();
        mockUser.setStudentCode(studentCode);
        mockUser.setPassword("hashed_password_123");
        mockUser.setRole("STUDENT"); // Giả lập Role từ DB

        when(userRepository.findByStudentCode(studentCode)).thenReturn(Optional.of(mockUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(studentCode);

        // Assert
        assertNotNull(userDetails);
        assertEquals(studentCode, userDetails.getUsername());
        assertEquals("hashed_password_123", userDetails.getPassword());
        
        // Kiểm tra Spring Security Authority xem đã tự động gắn tiền tố "ROLE_" vào chưa
        boolean hasRoleStudent = userDetails.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_STUDENT"));
        assertTrue(hasRoleStudent, "Authority phải chứa ROLE_STUDENT");

        verify(userRepository, times(1)).findByStudentCode(studentCode);
    }
}