package com.example.backend.Post;

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
// ⭐️ LƯU Ý: Huynh kiểm tra xem UserRepository nằm ở package nào. 
// Nếu code báo lỗi import, hãy đổi thành com.example.backend.repository.UserRepository
import com.example.backend.User.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Page<PostResponse>> getNewsFeed(
            @RequestParam(defaultValue = "0") int page,
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

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PostResponse> result = feedService.getNewsFeed(currentUser.getId(), pageable);

        return ResponseEntity.ok(result);
    }
}
