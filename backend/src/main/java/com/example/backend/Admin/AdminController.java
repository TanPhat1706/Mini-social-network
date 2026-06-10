package com.example.backend.Admin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort; // 🟢 THÊM IMPORT NÀY
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.backend.Post.PostRepository;
import com.example.backend.Post.PostResponse;
import com.example.backend.Post.PostService;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import com.example.backend.User.UserResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final PostService postService;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    // ==========================================
    // --- CÁC API VỀ BÀI VIẾT (POST) ---
    // ==========================================

    @GetMapping("/dashboard")
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
    public ResponseEntity<?> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            // 🟢 TỐI ƯU HIỆU NĂNG & LOGIC: Cố định sắp xếp giảm dần theo 'id'
            // ID auto-increment nên ID lớn nhất = bài viết mới nhất. Sort theo ID nhanh hơn sort theo createdAt.
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")); 
            
            Page<PostResponse> posts = postService.getAllPostsForAdmin(pageable);
            return ResponseEntity.ok(posts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to retrieve posts: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete-post/{post_id}")
    public ResponseEntity<String> deletePost(
            @PathVariable Long post_id, 
            @RequestParam(required = false) String reason) {
        try {
            // Lý tưởng nhất là: postService.deletePost(post_id, reason);
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

    // ==========================================
    // --- CÁC API VỀ NGƯỜI DÙNG (USER) ---
    // ==========================================

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<String> targetRoles = Arrays.asList("STUDENT", "TEACHER");
            List<User> users = userRepository.findByRoleIn(targetRoles);        

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