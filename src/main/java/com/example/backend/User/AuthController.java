package com.example.backend.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
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

    @GetMapping("/profile/{id}")
    public ResponseEntity<?> getProfile(@PathVariable Long id) {
        User user = userRepository.getReferenceById(id);
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProfile(
            @ModelAttribute UpdateProfileRequest req, // Dùng ModelAttribute để map các field text (fullName, bio...)
            @RequestParam(value = "file", required = false) MultipartFile file // Nhận file ảnh (không bắt buộc)
    ) {
        try {
            String studentCode = SecurityContextHolder.getContext().getAuthentication().getName();
            
            // Gọi Service với tham số file mới thêm
            User updatedUser = authService.updateProfile(studentCode, req, file);
            
            updatedUser.setPassword(null); 
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> searchUsers(@RequestParam("name") String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(authService.searchUsers(query)); 
    }
}