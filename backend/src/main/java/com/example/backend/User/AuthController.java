package com.example.backend.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest req) {
        try {
            return ResponseEntity.ok(authService.register(req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Phase 1: Request initiation - must not block or reveal account existence
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        try {
            String email = body.getOrDefault("email", "");
            // Launch async processing and immediately return the fixed response
            passwordResetService.asyncInitiateForgotPassword(email);
            Map<String, String> resp = new HashMap<>();
            resp.put("message", "If the email matches our records, a reset link has been sent");
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            // Do not leak details
            Map<String, String> resp = new HashMap<>();
            resp.put("message", "If the email matches our records, a reset link has been sent");
            return ResponseEntity.ok(resp);
        }
    }

    // Phase 3: Verify token and update password
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid PasswordResetRequest req) {
        try {
            boolean ok = passwordResetService.resetPassword(req);
            if (ok) {
                Map<String, String> resp = new HashMap<>();
                resp.put("message", "Password updated successfully");
                return ResponseEntity.ok(resp);
            } else {
                Map<String, String> resp = new HashMap<>();
                resp.put("message", "Invalid or expired token");
                return ResponseEntity.status(400).body(resp);
            }
        } catch (BadRequestException ex) {
            Map<String, String> resp = new HashMap<>();
            resp.put("message", ex.getMessage());
            return ResponseEntity.status(400).body(resp);
        } catch (Exception ex) {
            Map<String, String> resp = new HashMap<>();
            resp.put("message", "Invalid or expired token");
            return ResponseEntity.status(400).body(resp);
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody @Valid ChangePasswordRequest req) {
        try {
            String studentCode = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByStudentCode(studentCode)
                    .orElseThrow(() -> new BadRequestException("Người dùng không hợp lệ"));

            authService.changePassword(user.getId(), req);

            Map<String, String> resp = new HashMap<>();
            resp.put("message", "Mật khẩu đã được cập nhật thành công");
            return ResponseEntity.ok(resp);
        } catch (BadRequestException ex) {
            return ResponseEntity.status(400).body(Map.of("message", ex.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(400).body(Map.of("message", "Lỗi khi thay đổi mật khẩu"));
        }
    }

    // ⭐️ CHIÊU THỨ 1: Sửa API Login trả về full thông tin
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest req) {
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
            // 🟢 MỚI: TRẢ VỀ ĐIỂM VÀ LEVEL NGAY KHI LOGIN
            response.put("level", user.getLevel());
            response.put("exp", user.getExp());
            response.put("vptlPoints", user.getVptlPoints());
            response.put("currentAvatarFrame", user.getCurrentAvatarFrame());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    // API phục vụ thanh tìm kiếm trên Header
    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> searchUsers(@RequestParam("name") String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(authService.searchUsers(query));
    }

    // --- CẬP NHẬT: LẤY PROFILE ĐẦY ĐỦ (BAO GỒM ẢNH BÌA) ---
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        String studentCode = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(toUserResponse(user));
    }

    // --- MỚI: API CẬP NHẬT THÔNG TIN & ẢNH ---
    // Dùng @RequestParam để nhận từng trường trong FormData
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProfile(
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "bio", required = false) String bio,
            @RequestParam(value = "className", required = false) String className,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar,
            @RequestParam(value = "cover", required = false) MultipartFile cover) {
        try {
            // Lấy user hiện tại từ Token
            String studentCode = SecurityContextHolder.getContext().getAuthentication().getName();

            User updatedUser = authService.updateProfile(studentCode, fullName, bio, className, avatar, cover);

            return ResponseEntity.ok(toUserResponse(updatedUser));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi cập nhật: " + e.getMessage());
        }
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .studentCode(user.getStudentCode())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .coverPhotoUrl(user.getCoverPhotoUrl())
                .bio(user.getBio())
                .className(user.getClassName())
                .role(user.getRole())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .level(user.getLevel())
                .exp(user.getExp())
                .vptlPoints(user.getVptlPoints())
                .currentAvatarFrame(user.getCurrentAvatarFrame())
                .currentNameColor(user.getCurrentNameColor())
                .build();
    }
}