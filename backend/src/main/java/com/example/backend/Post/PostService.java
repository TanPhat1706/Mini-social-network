package com.example.backend.Post;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap; // 👈 Import Thread-safe Map
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled; // 👈 Import Scheduled
// import org.springframework.dao.DataIntegrityViolationException; // 👈 Import bắt lỗi

import com.example.backend.Enum.MediaType;
import com.example.backend.Enum.NotificationType;
import com.example.backend.Enum.ReactionType;
import com.example.backend.Enum.Visibility;
import com.example.backend.Event.NotificationEvent;
import com.example.backend.PostMedia.MediaResponse;
import com.example.backend.PostMedia.PostMedia;
import com.example.backend.PostReaction.PostReaction;
import com.example.backend.PostReaction.PostReactionRepository;
import com.example.backend.PostReaction.ReactionUserResponse;
import com.example.backend.Storage.FileStorageService;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import com.example.backend.User.UserResponse;
import com.example.backend.VPTLpoint.VptlService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostReactionRepository postReactionRepository;
    private final ApplicationEventPublisher evenPublisher;
    private final FileStorageService fileStorageService;
    private final VptlService vptlService;

    // ⭐️ BỘ ĐỆM RAM: Lưu trữ số lượt Like thay đổi (+1 hoặc -1) của từng bài viết
    private final ConcurrentHashMap<String, Integer> reactionBuffer = new ConcurrentHashMap<>();
    // 🛡️ KHIÊN CHỐNG SPAM: Lưu trạng thái xem User này có đang thao tác Like trên
    // Post này không
    private final ConcurrentHashMap<String, Boolean> actionLock = new ConcurrentHashMap<>();

    private User getCurrentUser() {
        String studentCode = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng (Token không hợp lệ?)"));
    }

    private String getCurrentViewerStudentCode() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    @Transactional
    public PostResponse createPost(PostRequest request) {
        User currentUser = getCurrentUser();
        System.out.println("Creating post for user: " + currentUser.getStudentCode());
        validatePostPayload(request);

        Post post = new Post();
        post.setContent(normalizeContent(request.getContent()));
        post.setAuthor(currentUser);
        post.setSharer(currentUser);
        post.setOriginalPost(null);

        // ⭐️ CHIÊU 1: CẦM CHẾ THUẬT (Kiểm soát Visibility)
        // Nếu user chọn PUBLIC -> Ép về PENDING để Admin duyệt.
        // Nếu user chọn CLASS/PRIVATE -> Cho phép đăng ngay.
        if (request.getVisibility() == Visibility.PUBLIC) {
            post.setVisibility(Visibility.PENDING);
        } else {
            post.setVisibility(request.getVisibility());
        }

        // Xử lý lưu media files nếu có

        if (request.getMediaFiles() != null && !request.getMediaFiles().isEmpty()) {
            List<PostMedia> mediaList = new ArrayList<>();
            for (MultipartFile file : request.getMediaFiles()) {
                String fileUrl = fileStorageService.storeFile(file, "posts");
                PostMedia media = new PostMedia();
                media.setPost(post);
                media.setMediaUrl(fileUrl);
                media.setMediaType(detectMediaType(file));
                mediaList.add(media);
            }
            post.setMedia(mediaList);
        }

        Post savedPost = postRepository.save(post);
        if (savedPost.getVisibility() == Visibility.PUBLIC) {
            vptlService.trackSocialActivity(currentUser.getId(), "POST");
        }
        return mapToPostResponse(savedPost);
    }

    @Transactional
    public PostResponse updatePost(Long postId, PostRequest request) {
        User currentUser = getCurrentUser();
        validatePostPayload(request);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Bài viết không tồn tại!"));

        if (!post.getAuthor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa bài viết này!");
        }

        post.setContent(normalizeContent(request.getContent()));

        Visibility oldVisibility = post.getVisibility();
        Visibility newVisibility = request.getVisibility();

        if (newVisibility != oldVisibility) {
            if (newVisibility == Visibility.PUBLIC) {
                post.setVisibility(Visibility.PENDING);
            } else {
                post.setVisibility(newVisibility);
            }

            if (newVisibility == Visibility.PRIVATE && oldVisibility == Visibility.PUBLIC) {
                // Xu ly cac tinh huong khi tu PUBLIC -> PRIVATE (an cac like, comment, share)
            }
        }

        List<PostMedia> currentMediaList = post.getMedia();
        List<Long> retainedMediaIds = request.getRetainedMediaIds() != null ? request.getRetainedMediaIds()
                : new ArrayList<>();
        List<PostMedia> mediaToRemove = currentMediaList.stream()
                .filter(media -> !retainedMediaIds.contains(media.getId()))
                .collect(Collectors.toList());

        for (PostMedia media : mediaToRemove) {
            fileStorageService.deleteFile(media.getMediaUrl());
        }

        if (request.getMediaFiles() != null && !request.getMediaFiles().isEmpty()) {
            for (MultipartFile file : request.getMediaFiles()) {
                String fileUrl = fileStorageService.storeFile(file, "posts");
                PostMedia media = new PostMedia();
                media.setPost(post);
                media.setMediaUrl(fileUrl);
                media.setMediaType(detectMediaType(file));
                currentMediaList.add(media);
            }
        }

        Post updatedPost = postRepository.save(post);
        return mapToPostResponse(updatedPost);
    }

    @Transactional
    public void deletePost(Long postId) {
        User currentUser = getCurrentUser();

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Bài viết không tồn tại!"));

        boolean isAdmin = "ADMIN".equals(currentUser.getRole());
        if (!post.getAuthor().getId().equals(currentUser.getId()) && !isAdmin) {
            throw new RuntimeException("Bạn không có quyền xóa bài viết này!");
        }

        if (post.getMedia() != null) {
            for (PostMedia media : post.getMedia()) {
                fileStorageService.deleteFile(media.getMediaUrl());
            }
        }

        postReactionRepository.deleteAllByPostId(postId);
        postRepository.unlinkSharedPosts(postId);
        postRepository.delete(post);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getAllPostsForAdmin(Pageable pageable) {
        Page<Post> posts = postRepository.findAllWithAuthorAndMedia(pageable);
        return posts.map(this::mapToPostResponse);
    }

    @Transactional
    public void approvePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Bài viết không tồn tại!"));
        boolean wasPending = post.getVisibility() == Visibility.PENDING;
        post.setVisibility(Visibility.PUBLIC); // Admin duyệt -> Thành PUBLIC
        postRepository.save(post);
        if (wasPending) {
            vptlService.trackSocialActivity(post.getAuthor().getId(), "POST");
        }
    }

    @Transactional
    public void reactToPost(Long postId, ReactionType requestedReaction) {
        User currentUser = getCurrentUser();
        Long userId = Long.valueOf(currentUser.getId());

        String lockKey = postId + "_" + userId;

        if (actionLock.putIfAbsent(lockKey, true) != null) {
            System.out.println("🛡️ Đã chặn Auto-Clicker từ user: " + currentUser.getStudentCode());
            return;
        }

        try {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("Bài viết không tồn tại!"));

            Optional<PostReaction> existingReactionOpt = postReactionRepository.findByPostIdAndUserId(postId, userId);

            if (existingReactionOpt.isPresent()) {
                PostReaction existingReaction = existingReactionOpt.get();
                ReactionType currentReaction = existingReaction.getReactionType();

                if (currentReaction == requestedReaction) {
                    postReactionRepository.delete(existingReaction);
                    updateBuffer(postId, currentReaction, -1);;
                } else {
                    existingReaction.setReactionType(requestedReaction);
                    postReactionRepository.save(existingReaction);
                    updateBuffer(postId, currentReaction, -1);
                    updateBuffer(postId, requestedReaction, 1);
                    evenPublisher.publishEvent(new NotificationEvent(
                            currentUser, post.getAuthor(), NotificationType.LIKE_POST, postId, "POST",
                            "đã thay đổi cảm xúc về bài viết của bạn",
                            false));
                }
            } else {
                PostReaction newReaction = PostReaction.builder()
                        .post(post)
                        .user(currentUser)
                        .reactionType(requestedReaction)
                        .build();

                postReactionRepository.save(newReaction);
                updateBuffer(postId, requestedReaction, 1);

                vptlService.trackSocialActivity(currentUser.getId(), "REACTION_" + requestedReaction.name());
                evenPublisher.publishEvent(new NotificationEvent(
                        currentUser, post.getAuthor(), NotificationType.LIKE_POST, postId, "POST",
                        "đã bày tỏ cảm xúc về bài viết của bạn",
                        false));
            }
        } finally {
            actionLock.remove(lockKey);
        }
    }

    public Page<ReactionUserResponse> getReactionsByPostId(Long postId, ReactionType reactionType, int page, int size) {
        if (postId == null) {
            throw new IllegalArgumentException("postId is required");
        }

        if (!postRepository.existsById(postId)) {
            throw new RuntimeException("Post not found");
        }

        Pageable pageable = PageRequest.of(page, size);
        return postReactionRepository.findUsersReationByPostId(postId, reactionType, pageable);
    }

    private void updateBuffer(Long postId, ReactionType type, int delta) {
        String bufferKey = postId + "_" + type.name();
        reactionBuffer.merge(bufferKey, delta, Integer::sum);
    }

    // ⭐️ BACKGROUND JOB: Cứ mỗi 5 giây, lấy dữ liệu từ RAM đổ một lần xuống DB
    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void syncLikesToDatabase() {
        if (reactionBuffer.isEmpty()) {
            return; // Không có ai like thì ngủ tiếp
        }

        // Tạo một bản sao (snapshot) của đệm hiện tại và xóa đệm cũ để đón lượt like
        // mới
        ConcurrentHashMap<String, Integer> snapshot = new ConcurrentHashMap<>(reactionBuffer);
        reactionBuffer.clear();

        for (Map.Entry<String, Integer> entry : snapshot.entrySet()) {
            String[] parts = entry.getKey().split("_");
            Long postId = Long.parseLong(parts[0]);
            String reactionType = parts[1];
            Integer delta = entry.getValue();

            if (delta != 0) {
                updatePostReactionCountDB(postId, reactionType, delta);
            }
        }
        System.out.println("🔄 [Eventual Consistency] Đã đồng bộ " + snapshot.size() + " bài viết xuống DB.");
    }

    private void updatePostReactionCountDB(Long postId, String reactionType, Integer delta) {
        postRepository.findById(postId).ifPresent(post -> {
            Map<String, Integer> counts = post.getReactionCounts();
            if (counts == null) {
                counts = new HashMap<>();
            }

            int currentCount = counts.getOrDefault(reactionType, 0);
            int newCount = Math.max(0, currentCount + delta);

            if (newCount == 0) {
                counts.remove(reactionType);
            } else {
                counts.put(reactionType, newCount);
            }

            post.setReactionCounts(counts);
            postRepository.save(post);
        });
    }

    @Transactional
    public PostResponse sharePost(Long originalPostId, PostRequest request) {
        User currentUser = getCurrentUser();
        Post originalPost = postRepository.findById(originalPostId)
                .orElseThrow(() -> new RuntimeException("Bài viết gốc không tồn tại"));

        Post rootPost = originalPost.getOriginalPost() != null ? originalPost.getOriginalPost() : originalPost;

        if (rootPost.getVisibility() == Visibility.PRIVATE) {
            throw new RuntimeException("Không thể chia sẻ bài viết riêng tư");
        }

        Post share = new Post();
        share.setContent(request.getContent());
        share.setAuthor(currentUser);
        share.setSharer(currentUser);
        share.setOriginalPost(rootPost);
        share.setVisibility(Visibility.PUBLIC);
        rootPost.setShareCount(rootPost.getShareCount() + 1);

        Post savedShare = postRepository.save(share);
        vptlService.trackSocialActivity(currentUser.getId(), "SHARE");
        evenPublisher.publishEvent(new NotificationEvent(
                currentUser,
                rootPost.getAuthor(),
                NotificationType.SHARE_POST,
                rootPost.getId(),
                "POST",
                "đã chia sẻ bài viết của bạn.",
                false));
        return mapToPostResponse(savedShare);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getPostsByAuthor(int page, int size) {
        User currentUser = getCurrentUser();
        Integer authorId = currentUser.getId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Post> posts = postRepository.findByAuthorId(authorId, pageable);
        return posts.map(this::mapToPostResponse);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getPostsByStudentCode(String studentCode, Pageable pageable) {
        if (!userRepository.existsByStudentCode(studentCode)) {
            throw new RuntimeException("User with studentCode " + studentCode + " not found");
        }

        if (!pageable.getSort().isSorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by("createdAt").descending());
        }

        // Lấy mã sinh viên của người đang xem
        String currentViewer = getCurrentViewerStudentCode();

        // 🐛 ĐÃ FIX: Xác định xem user có đang xem profile của chính mình hay không
        boolean isViewingOwnProfile = Objects.equals(currentViewer, studentCode);

        Page<Post> posts;
        if (isViewingOwnProfile) {
            // Nếu xem nhà mình -> Lấy cả bài PRIVATE/PENDING
            posts = postRepository.findByAuthorStudentCode(studentCode, pageable);
        } else {
            // Nếu xem nhà người khác -> Chỉ lấy bài PUBLIC
            posts = postRepository.findPublicByAuthorStudentCode(studentCode, pageable);
        }

        return posts.map(post -> mapToPostResponse(post, isViewingOwnProfile));
    }

    public String uploadFileToS3(MultipartFile file) {
        return fileStorageService.storeFile(file, "posts");
    }

    public PostResponse mapToPostResponse(Post post) {
        String currentViewerStudentCode = getCurrentViewerStudentCode();
        boolean isSelfPost = false;

        // 🐛 ĐÃ FIX: Chống NullPointerException bằng Objects.equals và check an toàn
        // Author
        if (currentViewerStudentCode != null && post.getAuthor() != null) {
            isSelfPost = Objects.equals(currentViewerStudentCode, post.getAuthor().getStudentCode());
        }

        return mapToPostResponse(post, isSelfPost);
    }

    public PostResponse mapToPostResponse(Post post, boolean isSelfPost) {
        User author = post.getAuthor();
        UserResponse authorDto = new UserResponse();
        authorDto.setId(author.getId());
        authorDto.setFullName(author.getFullName());
        authorDto.setAvatarUrl(author.getAvatarUrl());
        authorDto.setStudentCode(author.getStudentCode());
        authorDto.setCurrentAvatarFrame(author.getCurrentAvatarFrame());
        authorDto.setCurrentNameColor(author.getCurrentNameColor());

        // List<PostMedia> mediaList = post.getMedia() == null ? new ArrayList<>() :
        // post.getMedia();

        List<MediaResponse> mediaDtos = post.getMedia().stream()
                .map(m -> MediaResponse.builder()
                        .id(m.getId())
                        .url(m.getMediaUrl())
                        .type(m.getMediaType().toString())
                        .build())
                .collect(Collectors.toList());

        PostResponse originalPostResponse = null;

        if (post.getOriginalPost() != null) {
            if (post.getOriginalPost().getId().equals(post.getId())) {
                originalPostResponse = null;
            } else {
                // originalPostResponse = mapToPostResponse(post.getOriginalPost(), isSelfPost);
                originalPostResponse = buildFlattenedPostResponse(post.getOriginalPost(), isSelfPost);
            }
        }

        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .visibility(post.getVisibility())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .author(authorDto)
                .media(mediaDtos)
                .reactionCounts(post.getReactionCounts() != null ? post.getReactionCounts() : new HashMap<>())
                .commentCount(post.getCommentCount())
                .shareCount(post.getShareCount())
                .currentUserReaction(getCurrentUserReaction(post.getId()))
                .isSelfPost(isSelfPost)
                .originalPost(originalPostResponse)
                .build();
    }

    private PostResponse buildFlattenedPostResponse(Post post, boolean isSelfPost) {
        User author = post.getAuthor();
        UserResponse authorDto = new UserResponse();
        authorDto.setId(author.getId());
        authorDto.setFullName(author.getFullName());
        authorDto.setAvatarUrl(author.getAvatarUrl());
        authorDto.setStudentCode(author.getStudentCode());
        authorDto.setCurrentAvatarFrame(author.getCurrentAvatarFrame());
        authorDto.setCurrentNameColor(author.getCurrentNameColor());

        List<MediaResponse> mediaDtos = post.getMedia().stream()
                .map(m -> MediaResponse.builder()
                        .id(m.getId())
                        .url(m.getMediaUrl())
                        .type(m.getMediaType().toString())
                        .build())
                .collect(Collectors.toList());

        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .visibility(post.getVisibility())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .author(authorDto)
                .media(mediaDtos)
                .reactionCounts(post.getReactionCounts() != null ? post.getReactionCounts() : new HashMap<>())
                .commentCount(post.getCommentCount())
                .shareCount(post.getShareCount())
                .currentUserReaction(getCurrentUserReaction(post.getId()))
                .isSelfPost(isSelfPost)
                .originalPost(null)
                .build();
    }

    public String getCurrentUserReaction(Long postId) {
        User currentUser = getCurrentUser();
        Long userId = Long.valueOf(currentUser.getId());

        return postReactionRepository.findByPostIdAndUserId(postId, userId)
                .map(postReaction -> postReaction.getReactionType().name())
                .orElse(null);
    }

    private MediaType detectMediaType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null)
            return MediaType.IMAGE;
        if (contentType.startsWith("video"))
            return MediaType.VIDEO;
        if (contentType.startsWith("audio"))
            return MediaType.AUDIO;
        return MediaType.IMAGE;
    }

    private String normalizeContent(String content) {
        if (content == null)
            return "";
        return content.trim();
    }

    private void validatePostPayload(PostRequest request) {
        String content = normalizeContent(request.getContent());
        boolean hasNewMedia = request.getMediaFiles() != null && !request.getMediaFiles().isEmpty();
        boolean hasRetainedMedia = request.getRetainedMediaIds() != null && !request.getRetainedMediaIds().isEmpty();
        if (content.isEmpty() && !hasNewMedia && !hasRetainedMedia) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bài viết phải có nội dung hoặc hình ảnh/video.");
        }
    }
}
