package com.example.backend.Post;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
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
import org.springframework.web.multipart.MultipartFile;

import com.example.backend.Enum.ReactionType;
import com.example.backend.PostReaction.ReactionUserResponse;
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
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(postService.getPostsByAuthor(page, size));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse createPost(@ModelAttribute @Valid PostRequest request) {
        return postService.createPost(request);
    }

    @PostMapping(value = "/upload-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadMedia(@RequestParam("file") MultipartFile file) {
        String fileUrl = postService.uploadFileToS3(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(fileUrl);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long id,
            @ModelAttribute @Valid PostRequest request) {
        System.out.println("Updating post with ID: " + id);
        return ResponseEntity.ok(postService.updatePost(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/react")
    public ResponseEntity<?> reactToPost(@PathVariable Long postId, @RequestBody ReactRequest request) {
        postService.reactToPost(postId, request.getReactionType());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{postId}/share")
    public ResponseEntity<PostResponse> sharePost(
            @PathVariable Long postId,
            @RequestBody PostRequest request) {
        return ResponseEntity.ok(postService.sharePost(postId, request));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable @NonNull Long postId) {
        // Logic lấy bài viết theo ID
        // Nhớ check quyền xem (Visibility) nếu cần
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Bài viết không tồn tại"));

        // Map sang DTO response y hệt như list
        return ResponseEntity.ok(postService.mapToPostResponse(post));
    }

    @GetMapping("/{postId}/reactions")
    public ResponseEntity<Page<ReactionUserResponse>> getReactionsByPostId(
            @PathVariable Long postId,
            @RequestParam(required = false) ReactionType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(postService.getReactionsByPostId(postId, type, page, size));
    }
}
