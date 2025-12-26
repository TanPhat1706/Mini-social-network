package com.example.backend.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections; // 2. Sửa import đúng (java.util)

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository; // 3. Khai báo dependency này để dùng ở getProfile

    // API Đăng ký
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            return ResponseEntity.ok(authService.register(req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // API Đăng nhập
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            String token = authService.login(req);
            // Trả về JSON: { "token": "..." }
            return ResponseEntity.ok(Collections.singletonMap("token", token));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    // API Lấy thông tin cá nhân
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        // Lấy studentCode từ Token (đã được filter xử lý)
        String studentCode = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(null); // Ẩn mật khẩu hash trước khi trả về
        return ResponseEntity.ok(user);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest req) {
        try {
            String studentCode = SecurityContextHolder.getContext().getAuthentication().getName();
            User updatedUser = authService.updateProfile(studentCode, req);
            updatedUser.setPassword(null); // Ẩn mật khẩu
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}