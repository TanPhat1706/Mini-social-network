package com.example.backend.User;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetServiceImpl passwordResetService;

    // Helper method để băm chuỗi SHA-256 dùng trong test
    private String sha256Hex(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }

    // ==========================================
    // KIỂM THỬ: asyncInitiateForgotPassword
    // ==========================================

    @Test
    @DisplayName("Bỏ qua và không làm gì khi email không tồn tại (chống User Enumeration)")
    void asyncInitiateForgotPassword_whenEmailNotFound_shouldDoNothing() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> passwordResetService.asyncInitiateForgotPassword("ghost@example.com"));

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Tạo token, băm SHA-256, lưu DB và gửi email khi email tồn tại")
    void asyncInitiateForgotPassword_whenEmailExists_shouldCreateTokenAndSendEmail() throws NoSuchAlgorithmException {
        User user = new User();
        user.setId(1);
        user.setEmail("user@example.com");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        passwordResetService.asyncInitiateForgotPassword("user@example.com");

        // Bắt đối tượng Token được lưu vào DB
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        
        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertEquals(1, savedToken.getUserId());
        assertNotNull(savedToken.getHashedToken());
        assertTrue(savedToken.getExpiryTime().isAfter(LocalDateTime.now())); // Đảm bảo thời gian hết hạn ở tương lai

        // Bắt clearToken được gửi qua email
        ArgumentCaptor<String> clearTokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq("user@example.com"), clearTokenCaptor.capture());
        
        String clearTokenSent = clearTokenCaptor.getValue();
        
        // Kiểm tra xem clearToken băm ra có đúng bằng hashedToken lưu trong DB không
        String expectedHashed = sha256Hex(clearTokenSent);
        assertEquals(expectedHashed, savedToken.getHashedToken());
    }

    @Test
    @DisplayName("Nuốt Exception để tránh leak thông tin khi có lỗi xảy ra lúc tạo token")
    void asyncInitiateForgotPassword_whenExceptionThrown_shouldSwallow() {
        when(userRepository.findByEmail(anyString())).thenThrow(new RuntimeException("Database down"));

        // Phương thức void không được văng lỗi ra ngoài
        assertDoesNotThrow(() -> passwordResetService.asyncInitiateForgotPassword("test@example.com"));
    }

    // ==========================================
    // KIỂM THỬ: resetPassword
    // ==========================================

    @Test
    @DisplayName("Ném ngoại lệ BadRequestException khi mật khẩu xác nhận không khớp")
    void resetPassword_whenPasswordsMismatch_shouldThrowBadRequestException() {
        PasswordResetRequest req = new PasswordResetRequest();
        req.setNewPassword("newPass123");
        req.setConfirmPassword("differentPass");
        req.setToken("some-token");

        BadRequestException ex = assertThrows(BadRequestException.class, () -> passwordResetService.resetPassword(req));
        assertEquals("Mật khẩu xác nhận không khớp", ex.getMessage());
    }

    @Test
    @DisplayName("Trả về false nếu Token truyền vào (sau khi băm) không tồn tại trong DB")
    void resetPassword_whenTokenNotFound_shouldReturnFalse() {
        PasswordResetRequest req = new PasswordResetRequest();
        req.setNewPassword("newPass");
        req.setConfirmPassword("newPass");
        req.setToken("invalid-token");

        when(tokenRepository.findByHashedToken(anyString())).thenReturn(Optional.empty());

        boolean result = passwordResetService.resetPassword(req);

        assertFalse(result);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Trả về false và xóa Token nếu Token đã hết hạn")
    void resetPassword_whenTokenExpired_shouldDeleteAndReturnFalse() throws NoSuchAlgorithmException {
        PasswordResetRequest req = new PasswordResetRequest();
        req.setNewPassword("newPass");
        req.setConfirmPassword("newPass");
        req.setToken("expired-token");

        PasswordResetToken prt = new PasswordResetToken();
        prt.setHashedToken(sha256Hex("expired-token"));
        // Cố tình set thời gian ở quá khứ
        prt.setExpiryTime(LocalDateTime.now().minusMinutes(5)); 

        when(tokenRepository.findByHashedToken(anyString())).thenReturn(Optional.of(prt));

        boolean result = passwordResetService.resetPassword(req);

        assertFalse(result);
        verify(tokenRepository).delete(prt); // Đảm bảo token rác bị xóa
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Trả về false và xóa Token nếu Token hợp lệ nhưng User ID không tồn tại")
    void resetPassword_whenUserNotFound_shouldDeleteAndReturnFalse() {
        PasswordResetRequest req = new PasswordResetRequest();
        req.setNewPassword("newPass");
        req.setConfirmPassword("newPass");
        req.setToken("valid-token");

        PasswordResetToken prt = new PasswordResetToken();
        prt.setUserId(999);
        prt.setExpiryTime(LocalDateTime.now().plusMinutes(10)); // Token còn hạn

        when(tokenRepository.findByHashedToken(anyString())).thenReturn(Optional.of(prt));
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        boolean result = passwordResetService.resetPassword(req);

        assertFalse(result);
        verify(tokenRepository).delete(prt);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cập nhật mật khẩu thành công, lưu User và xóa Token")
    void resetPassword_whenSuccess_shouldUpdatePasswordAndReturnTrue() {
        PasswordResetRequest req = new PasswordResetRequest();
        req.setNewPassword("newPass123!");
        req.setConfirmPassword("newPass123!");
        req.setToken("valid-token");

        PasswordResetToken prt = new PasswordResetToken();
        prt.setUserId(1);
        prt.setExpiryTime(LocalDateTime.now().plusMinutes(10)); // Token còn hạn

        User user = new User();
        user.setId(1);

        when(tokenRepository.findByHashedToken(anyString())).thenReturn(Optional.of(prt));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPass123!")).thenReturn("hashed_newPass123!");

        boolean result = passwordResetService.resetPassword(req);

        assertTrue(result);

        // Đảm bảo mật khẩu đã được cập nhật
        assertEquals("hashed_newPass123!", user.getPassword());
        assertNotNull(user.getLastPasswordResetAt());
        
        // Đảm bảo user được lưu và token bị xóa
        verify(userRepository).save(user);
        verify(tokenRepository).delete(prt);
    }
}