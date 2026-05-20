package com.example.backend.User;

import com.example.backend.FriendRequest.FriendResponseDTO;
import com.example.backend.FriendRequest.FriendshipService;
import com.example.backend.Post.PostResponse;
import com.example.backend.Post.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserSocialController {
    private final PostService postService;
    private final FriendshipService friendshipService;

    @GetMapping("/{studentCode}/posts")
    public ResponseEntity<Page<PostResponse>> getPostsByStudentCode(
            @PathVariable String studentCode,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(postService.getPostsByStudentCode(studentCode, pageable));
    }

    @GetMapping("/{studentCode}/friends")
    public ResponseEntity<Page<FriendResponseDTO>> getFriendsByStudentCode(
            @PathVariable String studentCode,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(friendshipService.getFriendsByStudentCode(studentCode, pageable));
    }
}
