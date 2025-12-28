package com.example.backend.Post;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.example.backend.User.User;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;
    private final PostRepository postRepository;

    @GetMapping("/my-posts")
    public ResponseEntity<Page<PostResponse>> getMyPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(postService.getPostsByAuthor(page, size));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse createPost(@ModelAttribute @Valid PostRequest request) {
        return postService.createPost(request);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PostResponse updatePost(@PathVariable Long id, @ModelAttribute @Valid PostRequest request) {
        return postService.updatePost(id, request);
    }

    @DeleteMapping("/{id}")
    public String deletePost(@PathVariable Long id) {
        return postService.deletePost(id);
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long postId, @AuthenticationPrincipal User user) {
        postService.toggleLike(postId);
        return ResponseEntity.ok("Thành công");
    }

    @PostMapping("/{postId}/share")
    public ResponseEntity<PostResponse> sharePost(
            @PathVariable Long postId, 
            @RequestBody PostRequest request
    ) {
        return ResponseEntity.ok(postService.sharePost(postId, request));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable Long postId) {
        // Logic lấy bài viết theo ID
        // Nhớ check quyền xem (Visibility) nếu cần
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Bài viết không tồn tại"));
            
        // Map sang DTO response y hệt như list
        return ResponseEntity.ok(postService.mapToPostResponse(post));
    }
}
