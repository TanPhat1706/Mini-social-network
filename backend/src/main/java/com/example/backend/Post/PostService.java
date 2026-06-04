package com.example.backend.Post;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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
import com.example.backend.Enum.MediaType;
import com.example.backend.Enum.NotificationType;
import com.example.backend.Enum.Visibility;
import com.example.backend.Event.NotificationEvent;
import com.example.backend.PostMedia.MediaResponse;
import com.example.backend.PostMedia.PostMedia;
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
    private final PostLikeRepository postLikeRepository;
    private final ApplicationEventPublisher evenPublisher;
    private final FileStorageService fileStorageService;
    private final VptlService vptlService;

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

        // ⭐️ CHIÊU 1 (Lặp lại): Khi sửa bài, nếu đổi sang PUBLIC cũng phải duyệt lại
        if (request.getVisibility() == Visibility.PUBLIC) {
            post.setVisibility(Visibility.PENDING);
        } else {
            post.setVisibility(request.getVisibility());
        }

        var currentMediaList = post.getMedia();
        currentMediaList.clear();

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
    public String deletePost(Long postId) {
        User currentUser = getCurrentUser();

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Bài viết không tồn tại!"));

        boolean isAdmin = "ADMIN".equals(currentUser.getRole());
        if (!post.getAuthor().getId().equals(currentUser.getId()) && !isAdmin) {
            throw new RuntimeException("Bạn không có quyền xóa bài viết này!");
        }

        postRepository.delete(post);
        return "Xóa bài viết thành công";
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
    public void toggleLike(Long postId) {
        User currentUser = getCurrentUser();
        Long userId = Long.valueOf(currentUser.getId());
        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndUserId(postId, userId);

        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
            postRepository.decrementLikeCount(postId);
        } else {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("Bài viết không tồn tại!"));
            User user = getCurrentUser();
            User author = post.getAuthor();

            PostLike newLike = PostLike.builder()
                    .post(post)
                    .user(user)
                    .build();

            postLikeRepository.save(newLike);
            postRepository.incrementLikeCount(postId);
            vptlService.trackSocialActivity(currentUser.getId(), "LIKE");
            evenPublisher.publishEvent(new NotificationEvent(
                    currentUser, author, NotificationType.LIKE_POST, postId, "POST", "đã thích bài viết của bạn",
                    false));
        }
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

        boolean isSelfPost = studentCode.equals(getCurrentViewerStudentCode());
        Page<Post> posts = postRepository.findByAuthorStudentCode(studentCode, pageable);
        return posts.map(post -> mapToPostResponse(post, isSelfPost));
    }

    public String uploadFileToS3(MultipartFile file) {
        return fileStorageService.storeFile(file, "posts");
    }

    public PostResponse mapToPostResponse(Post post) {
        return mapToPostResponse(post, false);
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

        // List<PostMedia> mediaList = post.getMedia() == null ? new ArrayList<>() : post.getMedia();

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
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .shareCount(post.getShareCount())
                .isLikedByCurrentUser(false)
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
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .shareCount(post.getShareCount())
                .isLikedByCurrentUser(false)
                .isSelfPost(isSelfPost)
                .originalPost(null)
                .build();
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
        boolean hasMedia = request.getMediaFiles() != null && !request.getMediaFiles().isEmpty();
        if (content.isEmpty() && !hasMedia) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bài viết phải có nội dung hoặc hình ảnh/video.");
        }
    }
}
