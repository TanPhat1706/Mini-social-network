package com.example.backend.Comment;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.Enum.NotificationType;
import com.example.backend.Event.NotificationEvent;
import com.example.backend.Post.Post;
import com.example.backend.Post.PostRepository;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import com.example.backend.User.UserResponse;
import com.example.backend.VPTLpoint.VptlService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final ApplicationEventPublisher evenPublisher;
    private final VptlService vptlService;

    private User getCurrentUser() {
        String studentCode = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng (Token không hợp lệ?)"));
    }

    @Transactional
    public CommentResponse createComment(CommentRequest request) {
        User currentUser = getCurrentUser();

        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new EntityNotFoundException("Post not found"));
        
        Comment parentComment = null;
        if (request.getParentCommentId() != null) {
            parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new EntityNotFoundException("Parent comment not found"));
            commentRepository.incrementReplyCount(parentComment.getId());
        }

        Comment comment = Comment.builder()
                .content(request.getContent())
                .author(currentUser)
                .post(post)
                .parent(parentComment)
                .likeCount(0L)
                .replyCount(0L)
                .build();
        
        Comment savedComment = commentRepository.save(comment);
        postRepository.incrementCommentCount(post.getId());
        vptlService.trackSocialActivity(currentUser.getId(), "COMMENT");

        evenPublisher.publishEvent(new NotificationEvent(
                currentUser, post.getAuthor(), NotificationType.COMMENT_POST, post.getId(), "COMMENT", "đã thích bài viết của bạn"
        ));

        return mapToResponse(savedComment, false);
    }

    @Transactional
    public CommentResponse updateComment(Long commentId, String newContent) {
        User currentUser = getCurrentUser();
        Integer currentUserId = currentUser.getId();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found"));

        if (!comment.getAuthor().getId().equals(currentUserId)) {
            throw new RuntimeException("Bạn không có quyền sửa comment này");
        }

        comment.setContent(newContent);
        boolean isLiked = commentLikeRepository.findByCommentIdAndUserId(commentId, Long.valueOf(currentUserId)).isPresent();
        
        return mapToResponse(comment, isLiked);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        User currentUser = getCurrentUser();
        Integer currentUserId = currentUser.getId();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found"));

        boolean isAuthor = comment.getAuthor().getId().equals(currentUserId);
        boolean isPostOwner = comment.getPost().getAuthor().getId().equals(currentUserId);

        if (!isAuthor && !isPostOwner) {
            throw new RuntimeException("Không có quyền xóa comment này");
        }

        postRepository.decrementCommentCount(comment.getPost().getId());

        if (comment.getParent() != null) {
            Comment parent = comment.getParent();
            if (parent.getReplyCount() > 0) {
                 parent.setReplyCount(parent.getReplyCount() - 1);
                 commentRepository.save(parent);
            }
        }

        commentRepository.delete(comment);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByPost(Long postId, Pageable pageable) {
        User currentUser = getCurrentUser();
        Long currentUserId = Long.valueOf(currentUser.getId());
        Page<Comment> comments = commentRepository.findRootCommentsByPostId(postId, pageable);
        return mapToPageResponse(comments, currentUserId);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getReplies(Long parentCommentId, Pageable pageable) {
        User currentUser = getCurrentUser();
        System.out.println("Current User in Service: " + currentUser);
        Long currentUserId = Long.valueOf(currentUser.getId());
        Page<Comment> replies = commentRepository.findRepliesByParentId(parentCommentId, pageable);
        return mapToPageResponse(replies, currentUserId);
    }

    private Page<CommentResponse> mapToPageResponse(Page<Comment> comments, Long currentUserId) {
        List<Long> commentIds = comments.getContent().stream().map(Comment::getId).toList();
        
        Set<Long> likedCommentIds = new HashSet<>();
        if (currentUserId != null && !commentIds.isEmpty()) {
            likedCommentIds = commentLikeRepository.findCommentIdsLikedByUser(currentUserId, commentIds);
        }

        final Set<Long> finalLikedIds = likedCommentIds;
        return comments.map(c -> mapToResponse(c, finalLikedIds.contains(c.getId())));
    }

    @Transactional
    public void toggleLike(Long commentId) {
        User currentUser = getCurrentUser();
        Long userId = Long.valueOf(currentUser.getId());
        Optional<CommentLike> existingLike = commentLikeRepository.findByCommentIdAndUserId(commentId, userId);

        if (existingLike.isPresent()) {
            commentLikeRepository.delete(existingLike.get());
            commentRepository.decrementLikeCount(commentId);
        } else {
            Comment comment = commentRepository.getReferenceById(commentId);
            User user = userRepository.getReferenceById(userId);
            
            CommentLike like = CommentLike.builder().comment(comment).user(user).build();
            commentLikeRepository.save(like);
            commentRepository.incrementLikeCount(commentId);
            vptlService.trackSocialActivity(currentUser.getId(), "LIKE");
        }
    }

    private CommentResponse mapToResponse(Comment comment, boolean isLiked) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .likeCount(comment.getLikeCount())
                .replyCount(comment.getReplyCount())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .isLikedByCurrentUser(isLiked)
                .author(UserResponse.builder()
                        .id(comment.getAuthor().getId())
                        .fullName(comment.getAuthor().getFullName())
                        .avatarUrl(comment.getAuthor().getAvatarUrl())
                        .studentCode(comment.getAuthor().getStudentCode())
                        .build())
                .build();
    }
}
