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
        String studentCode = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        // ⭐️ OK: currentUser.getId() là Integer, Service giờ đã nhận Integer
        Page<PostResponse> result = feedService.getNewsFeed(currentUser.getId(), pageable);

        return ResponseEntity.ok(result);
    }
}
