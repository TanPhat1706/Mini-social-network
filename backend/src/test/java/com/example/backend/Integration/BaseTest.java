package com.example.backend.Integration;

import com.example.backend.User.JwtUtil;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import com.example.backend.User.SecurityHistory; // 🟢 Thêm import
import com.example.backend.User.SecurityHistoryRepository; // 🟢 Thêm import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@SpringBootTest(classes = com.example.backend.BackendApplication.class) 
@AutoConfigureMockMvc
@ActiveProfiles("test") // "Vòng kim cô" ép dùng DB Test
@Transactional          // Dọn rác sau mỗi bài test
public abstract class BaseTest {

    @Autowired
    protected MockMvc mockMvc; 

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityHistoryRepository securityHistoryRepository; // 🟢 Dùng để lưu Session giả lập

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private String cachedAdminToken;
    private String cachedUserToken;

    protected String getAdminToken() {
        if (cachedAdminToken != null) {
            return cachedAdminToken;
        }

        // 🟢 Tạo SessionID ngẫu nhiên cho Admin
        String sessionId = UUID.randomUUID().toString();
        User admin = ensureTestUser("141204", "pht1@gmail.com", "ADMIN");
        ensureSessionExists(admin, sessionId); // Cấp phép session trong DB

        // Truyền sessionId vào
        cachedAdminToken = "Bearer " + jwtUtil.generateToken("141204", sessionId);
        return cachedAdminToken;
    }

    protected String getUserToken() {
        if (cachedUserToken != null) {
            return cachedUserToken;
        }

        // 🟢 Tạo SessionID ngẫu nhiên cho User
        String sessionId = UUID.randomUUID().toString();
        User student = ensureTestUser("1412", "pht@gmail.com", "STUDENT");
        ensureSessionExists(student, sessionId); // Cấp phép session trong DB

        // Truyền sessionId vào
        cachedUserToken = "Bearer " + jwtUtil.generateToken("1412", sessionId);
        return cachedUserToken;
    }

    // 🟢 ĐỔI TỪ void SANG User ĐỂ LẤY OBJECT ĐI LƯU SESSION
    private User ensureTestUser(String studentCode, String email, String role) {
        User user = userRepository.findByStudentCode(studentCode).orElseGet(User::new);
        user.setStudentCode(studentCode);
        user.setEmail(email);
        user.setFullName("Test " + role);
        user.setClassName("TEST");
        user.setRole(role);
        user.setActive(true);
        user.setPassword(passwordEncoder.encode("1412"));
        return userRepository.saveAndFlush(user); // Trả về user đã lưu
    }

    // 🟢 MỚI: HÀM TẠO LỊCH SỬ BẢO MẬT GIẢ LẬP ĐỂ VƯỢT QUA JWT FILTER
    private void ensureSessionExists(User user, String sessionId) {
        SecurityHistory history = new SecurityHistory();
        history.setUser(user);
        history.setSessionId(sessionId);
        history.setIsActive(true); // Quan trọng nhất: Cho phép qua Filter
        history.setIpAddress("127.0.0.1");
        history.setBrowser("MockMvc Test Browser");
        history.setDevice("Test Device");
        history.setStatus("SUCCESS");
        securityHistoryRepository.saveAndFlush(history);
    }
}