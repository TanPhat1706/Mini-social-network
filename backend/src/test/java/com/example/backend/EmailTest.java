package com.example.backend;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

/**
 * Manual SMTP verification — not part of {@code mvn test}.
 * Enable locally when you need to verify real email delivery.
 */
@SpringBootTest
@ActiveProfiles("test")
@Disabled("Requires live Gmail SMTP; run manually when verifying email config")
class EmailTest {

    @Autowired
    private JavaMailSender mailSender;

    @Test
    void testSendEmail() {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom("offical.minisocialnetwork@gmail.com");
        msg.setTo("lhp141204@gmail.com");
        msg.setSubject("Test Email from Mini Social Network");
        msg.setText("This is a test email to verify SMTP configuration works correctly.");
        mailSender.send(msg);
    }
}
