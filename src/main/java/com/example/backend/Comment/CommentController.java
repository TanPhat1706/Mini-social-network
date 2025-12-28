package com.example.backend.Comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.User.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentResponse> createComment(@Valid @RequestBody CommentRequest request, 
        @AuthenticationPrincipal User user
    ) {
        CommentResponse response = commentService.createComment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal User user) {
        CommentResponse response = commentService.updateComment(id, request.getContent());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id, @AuthenticationPrincipal User user) {
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<Page<CommentResponse>> getCommentsByPost(
            @PathVariable Long postId,
            @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal User user) {
        Page<CommentResponse> comments = commentService.getCommentsByPost(postId, pageable);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/{commentId}/replies")
    public ResponseEntity<Page<CommentResponse>> getReplies(
            @PathVariable Long commentId,
            @PageableDefault(size = 5) Pageable pageable,
            @AuthenticationPrincipal User user) {
        Page<CommentResponse> replies = commentService.getReplies(commentId, pageable);
        return ResponseEntity.ok(replies);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Void> toggleLike(@PathVariable Long id, @AuthenticationPrincipal User user) {        
        commentService.toggleLike(id);
        return ResponseEntity.ok().build();
    }
}
