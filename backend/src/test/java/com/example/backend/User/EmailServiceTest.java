package com.example.backend.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private final String mockFromAddress = "noreply@minisocial.com";

    @BeforeEach
    void setUp() {
        // Tiêm giá trị giả vào biến @Value("${spring.mail.username}")
        ReflectionTestUtils.setField(emailService, "fromAddress", mockFromAddress);
    }

    @Test
    @DisplayName("Gửi email thành công với cấu trúc SimpleMailMessage chính xác")
    void sendPasswordResetEmail_success_shouldSendMail() {
        // Arrange
        String toEmail = "student@example.com";
        String token = "dummy-token-123";

        // Act
        emailService.sendPasswordResetEmail(toEmail, token);

        // Assert: Bắt lấy đối tượng SimpleMailMessage truyền vào hàm send()
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage capturedMsg = captor.getValue();
        
        // Kiểm tra các trường thông tin của Email
        assertEquals(mockFromAddress, capturedMsg.getFrom());
        assertArrayEquals(new String[]{toEmail}, capturedMsg.getTo()); // getTo() trả về mảng String[]
        assertEquals("[Mini Social Network] Reset Your Password", capturedMsg.getSubject());
        
        // Đảm bảo nội dung email có chứa token
        assertNotNull(capturedMsg.getText());
        assertTrue(capturedMsg.getText().contains("token=" + token));
    }

    @Test
    @DisplayName("Không làm crash ứng dụng (swallow exception) khi việc gửi mail gặp lỗi")
    void sendPasswordResetEmail_whenMailExceptionThrown_shouldCatchAndLog() {
        // Arrange
        String toEmail = "student@example.com";
        String token = "dummy-token-123";

        // Giả lập lỗi từ Mail server (MailSendException là lớp con của MailException)
        doThrow(new MailSendException("SMTP connection failed"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert
        // Dùng assertDoesNotThrow để đảm bảo Service đã "nuốt" lỗi (catch exception) thành công và không ném ra ngoài
        assertDoesNotThrow(() -> emailService.sendPasswordResetEmail(toEmail, token));
        
        // Vẫn phải đảm bảo hàm send đã được gọi
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}