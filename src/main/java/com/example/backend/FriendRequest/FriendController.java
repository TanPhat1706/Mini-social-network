package com.example.backend.FriendRequest;

import com.example.backend.User.User;
import com.example.backend.User.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
// SỬA 1: Đổi đường dẫn khớp với Frontend (bỏ /api/auth đi vì FE em gọi trực tiếp /friends)
@RequestMapping("/api/auth/friends")
public class FriendController {

    @Autowired
    FriendshipService friendshipService;
    @Autowired
    UserRepository userRepository;

    // SỬA 2: Hàm lấy User ID an toàn (tránh lỗi NullPointerException nếu token khác kiểu)
    private Integer getCurrentUserId() {
        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // Thử tìm bằng Email trước
        User user = userRepository.findByEmail(principal).orElse(null);
        
        // Nếu không thấy thì tìm bằng StudentCode
        if (user == null) {
            user = userRepository.findByStudentCode(principal)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }
        return user.getId();
    }

    @PostMapping("/add/{targetId}")
    public ResponseEntity<?> send(@PathVariable Integer targetId) {
        return ResponseEntity.ok(
            Collections.singletonMap("message", friendshipService.sendRequest(getCurrentUserId(), targetId))
        );
    }

    @PostMapping("/accept/{targetId}")
    public ResponseEntity<?> accept(@PathVariable Integer targetId) {
        return ResponseEntity.ok(
            Collections.singletonMap("message", friendshipService.acceptRequest(getCurrentUserId(), targetId))
        );
    }

    @DeleteMapping("/remove/{targetId}")
    public ResponseEntity<?> remove(@PathVariable Integer targetId) {
        return ResponseEntity.ok(
            Collections.singletonMap("message", friendshipService.removeFriendship(getCurrentUserId(), targetId))
        );
    }

    @GetMapping("/status/{targetId}")
    public ResponseEntity<?> getStatus(@PathVariable Integer targetId) {
        return ResponseEntity.ok(friendshipService.getFriendshipStatus(getCurrentUserId(), targetId));
    }

    @GetMapping("/suggested")
    public ResponseEntity<?> getSuggested() {
        return ResponseEntity.ok(friendshipService.getSuggestedFriends(getCurrentUserId()));
    }

    @GetMapping("/requests")
    public ResponseEntity<?> getRequests() {
        System.out.println("Current User ID: " + getCurrentUserId());
        return ResponseEntity.ok(friendshipService.getFriendRequests(getCurrentUserId()));
    }

    @GetMapping("/list")
    public ResponseEntity<?> getFriendsList() {
        return ResponseEntity.ok(friendshipService.getUserFriends(getCurrentUserId()));
    }
}