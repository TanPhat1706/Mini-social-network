package com.example.backend.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            return ResponseEntity.ok(authService.register(req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ⭐️ CHIÊU THỨ 1: Sửa API Login trả về full thông tin
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            // Gọi service để lấy token
            String token = authService.login(req);
            
            // Tìm lại user để lấy thông tin chi tiết
            User user = userRepository.findByStudentCodeOrEmail(req.getIdentifier(), req.getIdentifier())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Đóng gói kết quả trả về
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("id", user.getId());
            response.put("studentCode", user.getStudentCode());
            response.put("fullName", user.getFullName());
            response.put("role", user.getRole()); // Quan trọng nhất cái này
            response.put("avatarUrl", user.getAvatarUrl());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        String studentCode = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }
}