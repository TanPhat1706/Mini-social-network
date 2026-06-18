package com.example.backend.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    // 1. Thêm biến này để Spring Boot lấy URL từ cấu hình
    // Nếu không tìm thấy biến môi trường, nó sẽ tự động dùng mặc định là localhost:5173
    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public void sendPasswordResetEmail(String toEmail, String clearTextToken) {
        
        // 2. Thay thế link gán cứng bằng biến frontendUrl
        String link = frontendUrl + "/reset-password?token=" + clearTextToken;

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress); // MUST match the authenticated Gmail account
        msg.setTo(toEmail);
        msg.setSubject("[Mini Social Network] Reset Your Password");
        msg.setText("If you requested a password reset, click the link below:\n" +
                    link +
                    "\n\nIf you did not request this, you can safely ignore this email.");

        try {
            mailSender.send(msg);
        } catch (MailException ex) {
            // Log and swallow — transient mail errors should not crash async worker
            log.error("Failed to send password reset email to {}", toEmail, ex);
        }
    }
}