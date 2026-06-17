package com.example.backend.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
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
    private SecurityHistoryRepository securityHistoryRepository;

    @Autowired
    private JwtUtil jwtUtil;
    
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
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest req, HttpServletRequest request) { // 🟢 THÊM
                                                                                                       // HttpServletRequest
                                                                                                       // Ở ĐÂY
        try {
            // Lấy IP từ Request (đề phòng chạy qua Nginx/Proxy)
            String ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
            }

            // Lấy chuỗi User-Agent để dành phân tích Browser/Device
            String userAgentString = request.getHeader("User-Agent");

            /// 🟢 SINH RA MỘT MÃ SESSION ĐỘC NHẤT
            String sessionId = java.util.UUID.randomUUID().toString();

            // Truyền sessionId vào
            String token = authService.login(req, sessionId);

            // Tìm lại user để lấy thông tin chi tiết
            User user = userRepository.findByStudentCodeOrEmail(req.getIdentifier(), req.getIdentifier())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 🟢 MỚI: GỌI SERVICE ĐỂ LƯU LỊCH SỬ BẢO MẬT SAU KHI LOGIN THÀNH CÔNG
            authService.saveSecurityHistory(user, ipAddress, userAgentString, "SUCCESS", sessionId);

            // Đóng gói kết quả trả về
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("id", user.getId());
            response.put("studentCode", user.getStudentCode());
            response.put("fullName", user.getFullName());
            response.put("role", user.getRole());
            response.put("avatarUrl", user.getAvatarUrl());
            response.put("level", user.getLevel());
            response.put("exp", user.getExp());
            response.put("vptlPoints", user.getVptlPoints());
            response.put("currentAvatarFrame", user.getCurrentAvatarFrame());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Tùy chọn: Nếu muốn lưu cả lịch sử đăng nhập thất bại thì gọi
            // saveSecurityHistory ở đây với status "FAILED"
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

// 2. CẬP NHẬT API GET LỊCH SỬ (Hỗ trợ Phân trang cho dữ liệu lớn)
    @GetMapping("/security-history")
    public ResponseEntity<?> getSecurityHistory(
            @RequestParam(defaultValue = "0") int page,   // 🟢 Nhận page từ Frontend
            @RequestParam(defaultValue = "5") int size,   // 🟢 Nhận size từ Frontend
            HttpServletRequest request) {
        try {
            String studentCode = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByStudentCode(studentCode).orElseThrow();

            // 🟢 Khởi tạo đối tượng phân trang của Spring
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

            // 🟢 Lấy dữ liệu theo phân trang từ Database (Chỉ lấy đúng số dòng cần thiết)
            org.springframework.data.domain.Page<SecurityHistory> historyPage = securityHistoryRepository
                    .findByUserIdOrderByLoginTimeDesc(user.getId(), pageable);

            // Lấy sessionId của Token hiện tại đang gọi API
            String authHeader = request.getHeader("Authorization");
            String currentSessionId = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                currentSessionId = jwtUtil.extractSessionId(authHeader.substring(7));
            }

            // Đóng gói lại dữ liệu List
            List<Map<String, Object>> responseList = new java.util.ArrayList<>();
            for (SecurityHistory h : historyPage.getContent()) {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", h.getId());
                map.put("ipAddress", h.getIpAddress());
                map.put("browser", h.getBrowser());
                map.put("device", h.getDevice());
                map.put("loginTime", h.getLoginTime());
                map.put("status", h.getStatus());
                map.put("isActive", h.getIsActive()); 
                map.put("isCurrentDevice", h.getSessionId() != null && h.getSessionId().equals(currentSessionId));
                responseList.add(map);
            }

            // 🟢 Bọc lại thành format chuẩn để Frontend đọc được content và totalElements
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("content", responseList);
            response.put("totalElements", historyPage.getTotalElements());
            response.put("totalPages", historyPage.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi lấy lịch sử bảo mật: " + e.getMessage());
        }
    }
    
    // 3. 🟢 MỚI: API ÉP ĐĂNG XUẤT THIẾT BỊ KHÁC
    @PostMapping("/security-history/{id}/revoke")
    public ResponseEntity<?> revokeSession(@PathVariable Integer id) {
        try {
            String studentCode = SecurityContextHolder.getContext().getAuthentication().getName();

            SecurityHistory history = securityHistoryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch sử"));

            // Kiểm tra bảo mật: Tránh trường hợp user này revoke session của user khác
            if (!history.getUser().getStudentCode().equals(studentCode)) {
                return ResponseEntity.status(403).body("Không có quyền thực hiện");
            }

            // ĐÁNH ĐẦU BẰNG FALSE -> FILTER SẼ CHẶN THIẾT BỊ NÀY Ở REQUEST TIẾP THEO
            history.setIsActive(false);
            securityHistoryRepository.save(history);

            return ResponseEntity.ok("Đã đăng xuất thiết bị thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }
    // 🟢 THÊM MỚI: API Gỡ ảnh bìa
    @DeleteMapping("/profile/cover") // (Lưu ý đường dẫn, nếu class đã có @RequestMapping("/api/auth") thì chỉ cần "/profile/cover")
    public ResponseEntity<UserResponse> removeCoverPhoto() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String studentCode = auth.getName();
        
        User currentUser = userRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Xóa URL ảnh bìa
        currentUser.setCoverPhotoUrl(null);
        userRepository.save(currentUser);

        // Chuyển đổi User sang UserResponse để trả về cho Frontend
        UserResponse response = UserResponse.builder()
                .id(currentUser.getId())
                .studentCode(currentUser.getStudentCode())
                .email(currentUser.getEmail())
                .fullName(currentUser.getFullName())
                .className(currentUser.getClassName())
                .role(currentUser.getRole())
                .avatarUrl(currentUser.getAvatarUrl())
                .coverPhotoUrl(currentUser.getCoverPhotoUrl()) // Lúc này sẽ là null
                .bio(currentUser.getBio())
                .active(currentUser.getActive())
                .createdAt(currentUser.getCreatedAt())
                .lastLogin(currentUser.getLastLogin())
                .level(currentUser.getLevel())
                .exp(currentUser.getExp())
                .vptlPoints(currentUser.getVptlPoints())
                .currentAvatarFrame(currentUser.getCurrentAvatarFrame())
                .currentNameColor(currentUser.getCurrentNameColor())
                .build();

        return ResponseEntity.ok(response);
    }
}