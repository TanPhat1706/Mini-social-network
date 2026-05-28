package com.example.backend.Post;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import com.example.backend.User.User;
// ⭐️ LƯU Ý: Đã kiểm tra import UserRepository
import com.example.backend.User.UserRepository;

@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private final FeedService feedService;
    private final UserRepository userRepository;

    // Thay thế @RequiredArgsConstructor bằng Constructor tường minh
    @Autowired
    public FeedController(FeedService feedService, UserRepository userRepository) {
        this.feedService = feedService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<CursorPageResponse<PostResponse>> getNewsFeed(
            @RequestParam(required = false) Long lastPostId,
            @RequestParam(defaultValue = "10") int size
    ) {
        // 1. Lấy Authentication từ Context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 2. Kiểm tra: Nếu null HOẶC là người dùng ẩn danh thì đuổi thẳng (401)
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).build();
        }

        String studentCode = auth.getName();
        
        // 3. Tìm User với studentCode (đã đảm bảo không null ở bước trên)
        User currentUser = userRepository.findByStudentCode(studentCode)
                .orElseGet(() -> userRepository.findByEmail(studentCode).orElse(null));

        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        CursorPageResponse<PostResponse> result = feedService.getNewsFeed(currentUser.getId(), lastPostId, size);

        return ResponseEntity.ok(result);
    }
}