package com.example.backend.Controller;

import com.example.backend.Enum.Visibility;
import com.example.backend.Post.Post;
import com.example.backend.Post.PostRepository;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/internal/test")
public class InternalTestController {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    // API Ẩn để bơm dữ liệu giả
    @PostMapping("/seed-posts")
    @Transactional
    public ResponseEntity<String> seedPosts(@RequestParam(defaultValue = "10000") int count, 
                                            @RequestParam String studentCode) {
        
        // Tìm user chuyên dùng để test (để rác không dính vào user thật)
        User testUser = userRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User Test"));

        List<Post> batch = new ArrayList<>();
        
        for (int i = 1; i <= count; i++) {
            Post post = new Post();
            post.setAuthor(testUser);
            post.setSharer(testUser);
            post.setContent("Test Load Bài viết rác số " + i);
            // 🛡️ QUAN TRỌNG: Đặt PRIVATE để ẩn khỏi Feed của người dùng thật
            post.setVisibility(Visibility.PRIVATE); 
            post.setReactionCounts(null);
            post.setCommentCount(0L);
            post.setShareCount(0L);
            batch.add(post);

            // Lưu theo lô 1000 bài để tránh tràn RAM của ECS
            if (i % 1000 == 0) {
                postRepository.saveAll(batch);
                batch.clear();
            }
        }
        // Lưu phần còn dư
        if (!batch.isEmpty()) {
            postRepository.saveAll(batch);
        }

        return ResponseEntity.ok("✅ Đã bơm thành công " + count + " bài viết PRIVATE cho user " + studentCode);
    }

    // API Ẩn để dọn rác sau khi test xong
    @DeleteMapping("/cleanup-posts")
    @Transactional
    public ResponseEntity<String> cleanupPosts(@RequestParam String studentCode) {
        User testUser = userRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User Test"));
        
        // Xóa tất cả bài viết của User Test
        postRepository.deleteByAuthorId(testUser.getId());
        
        return ResponseEntity.ok("🧹 Đã dọn sạch dữ liệu rác của user " + studentCode);
    }
}