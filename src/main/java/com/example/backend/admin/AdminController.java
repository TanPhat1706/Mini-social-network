package com.example.backend.admin;

import java.util.Arrays;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.backend.Post.PostService;
import com.example.backend.Post.PostResponse;
import com.example.backend.Post.PostRepository;
import com.example.backend.User.User;           // Import đúng folder User
import com.example.backend.User.UserRepository; // Import đúng folder User
import com.example.backend.User.UserResponse;   // Import đúng folder User
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private PostService postService;
    
    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    // --- CÁC API VỀ BÀI VIẾT (POST) ---

    @GetMapping("/dashboard") // Đệ thêm mapping cho hàm này để không bị lỗi 404
    public ResponseEntity<String> getAdminDashboard() {
        return ResponseEntity.ok("Admin Dashboard");
    }

    @PostMapping("/approve-post/{post_id}")
    public ResponseEntity<String> approvePost(@PathVariable Long post_id) {
        try {
            postService.approvePost(post_id);
            return ResponseEntity.ok("Duyệt bài thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    @GetMapping("/posts")
    public ResponseEntity<?> getAllPosts() {
        try {
            List<PostResponse> posts = postService.getAllPostsForAdmin();
            return ResponseEntity.ok(posts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to retrieve posts: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete-post/{post_id}")
    public ResponseEntity<String> deletePost(@PathVariable Long post_id, @RequestParam String reason) {
        try {
            postService.deletePost(post_id);
            return ResponseEntity.ok("Xóa bài thành công.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi xóa bài: " + e.getMessage());
        }
    }

    @GetMapping("/posts-count")
    public ResponseEntity<Long> countPosts() {
        try {
            long count = postRepository.countPosts();
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(0L);
        }
    }

    // --- CÁC API VỀ NGƯỜI DÙNG (USER) ---

    // 1. Lấy danh sách toàn bộ User (Đã tối ưu dùng Builder)
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<String> targetRoles = Arrays.asList("STUDENT", "TEACHER");
            List<User> users = userRepository.findByRoleIn(targetRoles);        

            // ⭐️ SỬA ĐỔI: Dùng Builder thay vì set thủ công để đồng bộ với UserResponse mới
            List<UserResponse> userResponses = users.stream().map(u -> UserResponse.builder()
                .id(u.getId())
                .studentCode(u.getStudentCode())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .className(u.getClassName())
                .role(u.getRole())
                .avatarUrl(u.getAvatarUrl())
                .bio(u.getBio())
                .active(u.getActive())
                .createdAt(u.getCreatedAt())
                .lastLogin(u.getLastLogin())
                .build()
            ).collect(Collectors.toList());

            return ResponseEntity.ok(userResponses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi tải danh sách người dùng: " + e.getMessage());
        }
    }

    // 2. Ban User (Khóa tài khoản)
    @PostMapping("/ban-user/{user_id}")
    public ResponseEntity<String> banUser(@PathVariable Integer user_id) {
        try {
            User user = userRepository.findById(user_id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));
            
            user.setActive(false);
            userRepository.save(user);
            
            return ResponseEntity.ok("Đã khóa tài khoản sinh viên: " + user.getFullName());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    // 3. Approve User (Mở khóa tài khoản)
    @PostMapping("/approve-user/{user_id}")
    public ResponseEntity<String> approveUser(@PathVariable Integer user_id) {
        try {
            User user = userRepository.findById(user_id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

            user.setActive(true);
            userRepository.save(user);
            
            return ResponseEntity.ok("Đã duyệt/mở khóa tài khoản: " + user.getFullName());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }
    @GetMapping("/users-stats")
    public ResponseEntity<Map<String, Long>> getUserStats() {
        long activeCount = userRepository.countByActive(true);
        long pendingCount = userRepository.countByActive(false);

        Map<String, Long> stats = new HashMap<>();
        stats.put("activeUsers", activeCount);
        stats.put("pendingUsers", pendingCount);

        return ResponseEntity.ok(stats);
    }

}