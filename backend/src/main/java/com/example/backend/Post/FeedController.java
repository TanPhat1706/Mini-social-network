package com.example.backend.Post;

import org.springframework.beans.factory.annotation.Autowired;
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).build();
        }

        String identifier = auth.getName(); // JWT thường lưu studentCode hoặc username ở đây
        
        User currentUser = userRepository.findByStudentCode(identifier)
                .orElseGet(() -> userRepository.findByEmail(identifier).orElse(null));

        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        CursorPageResponse<PostResponse> result = feedService.getNewsFeed(currentUser.getId(), lastPostId, size);

        return ResponseEntity.ok(result);
    }
}