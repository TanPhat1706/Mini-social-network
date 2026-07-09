package com.example.backend.Comment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.Enum.NotificationType;
import com.example.backend.Enum.ReactionType;
import com.example.backend.Event.NotificationEvent;
import com.example.backend.Post.Post;
import com.example.backend.Post.PostRepository;
import com.example.backend.PostReaction.ReactionUserResponse;
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
    private final CommentReactionRepository commentReactionRepository;
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

        Boolean isAnon = request.getIsAnonymous() != null ? request.getIsAnonymous() : false;

        Comment comment = Comment.builder()
                .content(request.getContent())
                .author(currentUser)
                .post(post)
                .parent(parentComment)
                .reactionCount(0L)
                .replyCount(0L)
                .isAnonymous(isAnon)
                .build();

        Comment savedComment = commentRepository.save(comment);
        postRepository.incrementCommentCount(post.getId());
        vptlService.trackSocialActivity(currentUser.getId(), "COMMENT");

        if (parentComment != null) {
            User parentAuthor = parentComment.getAuthor();
            if (!parentAuthor.getId().equals(currentUser.getId())) {
                evenPublisher.publishEvent(new NotificationEvent(
                        currentUser, parentAuthor, NotificationType.REPLY_COMMENT, post.getId(), "COMMENT",
                        "đã phản hồi bình luận của bạn", null, isAnon));
            }
            if (!post.getAuthor().getId().equals(parentAuthor.getId())
                    && !post.getAuthor().getId().equals(currentUser.getId())) {
                evenPublisher.publishEvent(new NotificationEvent(
                        currentUser, post.getAuthor(), NotificationType.COMMENT_POST, post.getId(), "COMMENT",
                        "đã bình luận về bài viết của bạn", null, isAnon));
            }
        } else {
            if (!post.getAuthor().getId().equals(currentUser.getId())) {
                evenPublisher.publishEvent(new NotificationEvent(
                        currentUser, post.getAuthor(), NotificationType.COMMENT_POST, post.getId(), "COMMENT",
                        "đã bình luận về bài viết của bạn", null, isAnon));
            }
        }

        // Comment mới tạo nên map reactionCounts bằng rỗng
        return mapToResponse(savedComment, null, new HashMap<>());
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

        ReactionType currentReaction = commentReactionRepository
                .findByCommentIdAndUserId(commentId, Long.valueOf(currentUserId))
                .map(CommentReaction::getReactionType)
                .orElse(null);

        // Lấy reaction counts cho 1 comment
        Map<String, Long> countsMap = new HashMap<>();
        List<CommentReactionRepository.SingleReactionCountProjection> counts = commentReactionRepository
                .countReactionsByCommentId(commentId);
        for (var c : counts) {
            countsMap.put(c.getReactionType().name(), c.getCount());
        }

        return mapToResponse(comment, currentReaction, countsMap);
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
        Long currentUserId = Long.valueOf(currentUser.getId());
        Page<Comment> replies = commentRepository.findRepliesByParentId(parentCommentId, pageable);
        return mapToPageResponse(replies, currentUserId);
    }

    public Page<ReactionUserResponse> getReactionsByCommentId(Long commentId, ReactionType type, int page, int size) {
        if (commentId == null) {
            throw new IllegalArgumentException("commentId is required");
        }

        if (!commentRepository.existsById(commentId)) {
            throw new RuntimeException("Comment not found");
        }

        Pageable pageable = PageRequest.of(page, size);
        return commentReactionRepository.findUsersReactionByCommentId(commentId, type, pageable);
    }

    // 🟢 Tối ưu N+1: Lấy toàn bộ reaction user và count thống kê trong 2 query
    private Page<CommentResponse> mapToPageResponse(Page<Comment> comments, Long currentUserId) {
        List<Long> commentIds = comments.getContent().stream().map(Comment::getId).toList();

        // 1. Lấy trạng thái đã react của user hiện tại
        Map<Long, ReactionType> userReactions = Map.of();
        if (currentUserId != null && !commentIds.isEmpty()) {
            List<CommentReaction> reactions = commentReactionRepository
                    .findReactionsByUserIdAndCommentIds(currentUserId, commentIds);
            userReactions = reactions.stream()
                    .collect(Collectors.toMap(
                            reaction -> reaction.getComment().getId(), CommentReaction::getReactionType,
                            (existing, replacement) -> existing));
        }

        // 2. Lấy thống kê số lượng các loại cảm xúc (reactionCounts)
        Map<Long, Map<String, Long>> allReactionCounts = new HashMap<>();
        if (!commentIds.isEmpty()) {
            List<CommentReactionRepository.ReactionCountProjection> counts = commentReactionRepository
                    .countReactionsByCommentIds(commentIds);
            for (var dto : counts) {
                allReactionCounts
                        .computeIfAbsent(dto.getCommentId(), k -> new HashMap<>())
                        .put(dto.getReactionType().name(), dto.getCount());
            }
        }

        final Map<Long, ReactionType> finalUserReactions = userReactions;
        return comments.map(c -> {
            Map<String, Long> countsMap = allReactionCounts.getOrDefault(c.getId(), new HashMap<>());
            return mapToResponse(c, finalUserReactions.get(c.getId()), countsMap);
        });
    }

    @Transactional
    public void reactToComment(Long commentId, ReactionType reactionType) {
        User currentUser = getCurrentUser();
        Long userId = Long.valueOf(currentUser.getId());
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found"));
        Optional<CommentReaction> existingReaction = commentReactionRepository.findByCommentIdAndUserId(commentId,
                userId);

        Integer receiverId = comment.getAuthor().getId();
        User realReceiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new EntityNotFoundException("Receiver not found"));

        if (existingReaction.isPresent()) {
            CommentReaction currentReaction = existingReaction.get();

            if (currentReaction.getReactionType() == reactionType) {
                commentReactionRepository.delete(currentReaction);
                commentRepository.decrementReactionCount(commentId);
            } else {
                currentReaction.setReactionType(reactionType);
                commentReactionRepository.save(currentReaction);
                evenPublisher.publishEvent(new NotificationEvent(
                        currentUser, realReceiver, NotificationType.LIKE_COMMENT, commentId, "COMMENT",
                        "đã thay đổi cảm xúc về bình luận của bạn",
                        reactionType,
                        false));
            }
        } else {

            User user = userRepository.getReferenceById(userId);
            CommentReaction reaction = CommentReaction.builder()
                    .comment(comment)
                    .user(user)
                    .reactionType(reactionType)
                    .build();
            commentReactionRepository.save(reaction);
            commentRepository.incrementReactionCount(commentId);

            vptlService.trackSocialActivity(currentUser.getId(), "LIKE");
            evenPublisher.publishEvent(new NotificationEvent(
                    currentUser, realReceiver, NotificationType.LIKE_COMMENT, commentId, "COMMENT",
                    "đã bày tỏ cảm xúc về bình luận của bạn",
                    reactionType,
                    false));
        }
    }

    // 🟢 Cập nhật lại MapToResponse để nhận map reactionCounts
    private CommentResponse mapToResponse(Comment comment, ReactionType userReactionType,
            Map<String, Long> reactionCountsMap) {
        UserResponse authorDTO;

        if (Boolean.TRUE.equals(comment.getIsAnonymous())) {
            authorDTO = UserResponse.builder()
                    .id(0)
                    .fullName("Người dùng ẩn danh")
                    .avatarUrl("https://ui-avatars.com/api/?name=Anonymous&background=808080&color=fff")
                    .studentCode("Hidden")
                    .currentAvatarFrame(null)
                    .currentNameColor(null)
                    .build();
        } else {
            authorDTO = UserResponse.builder()
                    .id(comment.getAuthor().getId())
                    .fullName(comment.getAuthor().getFullName())
                    .avatarUrl(comment.getAuthor().getAvatarUrl())
                    .studentCode(comment.getAuthor().getStudentCode())
                    .currentAvatarFrame(comment.getAuthor().getCurrentAvatarFrame())
                    .currentNameColor(comment.getAuthor().getCurrentNameColor())
                    .build();
        }

        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .reactionCount(comment.getReactionCount())
                .reactionCounts(reactionCountsMap) // 🟢 SET MAP VÀO ĐÂY
                .replyCount(comment.getReplyCount())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .currentUserReaction(userReactionType)
                .author(authorDTO)
                .build();
    }
}