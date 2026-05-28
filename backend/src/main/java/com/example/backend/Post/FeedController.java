package com.example.backend.Post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.User.User;
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).build();
        }

        String identifier = auth.getName(); // JWT thường lưu studentCode hoặc username ở đây
        
        // Dùng Short-circuit an toàn: Tìm theo studentCode, nếu không có mới tìm email
        User currentUser = userRepository.findByStudentCode(identifier)
                .orElseGet(() -> userRepository.findByEmail(identifier).orElse(null));

        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        // Ép kiểu ID sang Long để đồng bộ với Database và tầng Service
        Page<PostResponse> result = feedService.getNewsFeed(Long.valueOf(currentUser.getId()), pageable);

        return ResponseEntity.ok(result);
    }
}