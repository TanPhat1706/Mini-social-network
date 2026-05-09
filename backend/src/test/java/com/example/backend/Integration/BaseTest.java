package com.example.backend.Integration;

import com.example.backend.User.JwtUtil;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private String cachedAdminToken;
    private String cachedUserToken;

    protected String getAdminToken() {
        if (cachedAdminToken != null) {
            return cachedAdminToken;
        }

        ensureTestUser("141204", "pht1@gmail.com", "ADMIN");
        cachedAdminToken = "Bearer " + jwtUtil.generateToken("141204");
        return cachedAdminToken;
    }

    protected String getUserToken() {
        if (cachedUserToken != null) {
            return cachedUserToken;
        }

        ensureTestUser("1412", "pht@gmail.com", "STUDENT");
        cachedUserToken = "Bearer " + jwtUtil.generateToken("1412");
        return cachedUserToken;
    }

    private void ensureTestUser(String studentCode, String email, String role) {
        User user = userRepository.findByStudentCode(studentCode).orElseGet(User::new);
        user.setStudentCode(studentCode);
        user.setEmail(email);
        user.setFullName("Test " + role);
        user.setClassName("TEST");
        user.setRole(role);
        user.setActive(true);
        user.setPassword(passwordEncoder.encode("1412"));
        userRepository.saveAndFlush(user);
    }
}
