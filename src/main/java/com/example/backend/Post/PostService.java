package com.example.backend.Post;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

// Import thư viện Cloudinary
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import com.example.backend.Enum.MediaType;
import com.example.backend.Enum.NotificationType;
import com.example.backend.Enum.Visibility;
import com.example.backend.Event.NotificationEvent;
import com.example.backend.PostMedia.MediaResponse;
import com.example.backend.PostMedia.PostMedia;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import com.example.backend.User.UserResponse;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostLikeRepository postLikeRepository;
    private final ApplicationEventPublisher evenPublisher;
    private final Cloudinary cloudinary; // Đã được inject từ Bean config

    // Đã xóa các biến @Value baseUrl và uploadDir vì không dùng nữa

    private User getCurrentUser() {
        String studentCode = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng (Token không hợp lệ?)"));
    }

    @Transactional
    public PostResponse createPost(PostRequest request) {
        User currentUser = getCurrentUser();

        Post post = new Post();
        post.setContent(request.getContent());
        post.setAuthor(currentUser);
        post.setSharer(currentUser);
        post.setOriginalPost(null);

        // ⭐️ CHIÊU 1: CẦM CHẾ THUẬT (Kiểm soát Visibility)
        if (request.getVisibility() == Visibility.PUBLIC) {
            post.setVisibility(Visibility.PENDING);
        } else {
            post.setVisibility(request.getVisibility());
        }

        // Xử lý lưu media files lên Cloudinary
        if (request.getMediaFiles() != null && !request.getMediaFiles().isEmpty()) {
            List<PostMedia> mediaList = new ArrayList<>();
            for (MultipartFile file : request.getMediaFiles()) {
                // Gọi hàm upload mới
                String fileUrl = storeFile(file); 
                
                PostMedia media = new PostMedia();
                media.setPost(post);
                media.setMediaUrl(fileUrl);
                media.setMediaType(detectMediaType(file));
                mediaList.add(media);
            }
            post.setMedia(mediaList);
        }

        Post savedPost = postRepository.save(post);
        return mapToPostResponse(savedPost);
    }

    @Transactional
    public PostResponse updatePost(Long postId, PostRequest request) {
        User currentUser = getCurrentUser();
        
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Bài viết không tồn tại!"));

        if (!post.getAuthor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa bài viết này!");
        }

        post.setContent(request.getContent());

        if (request.getVisibility() == Visibility.PUBLIC) {
            post.setVisibility(Visibility.PENDING);
        } else {
            post.setVisibility(request.getVisibility());
        }

        var currentMediaList = post.getMedia();
        currentMediaList.clear();
        
        // Xử lý lưu media files lên Cloudinary khi update
        if (request.getMediaFiles() != null && !request.getMediaFiles().isEmpty()) {
            for (MultipartFile file : request.getMediaFiles()) {
                String fileUrl = storeFile(file); // Gọi hàm upload mới
                
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

    @Transactional
    public List<PostResponse> getAllPostsForAdmin() {
        List<Post> posts = postRepository.findAllWithAuthorAndMedia();
        return posts.stream().map(this::mapToPostResponse).collect(Collectors.toList());
    }
    
    @Transactional
    public void approvePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Bài viết không tồn tại!"));
        post.setVisibility(Visibility.PUBLIC);
        postRepository.save(post);
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
            User user = getCurrentUser();
            Post post = postRepository.getReferenceById(postId);
            User author = post.getAuthor();

            PostLike newLike = PostLike.builder()
                    .post(post)
                    .user(user)
                    .build();
            
            postLikeRepository.save(newLike);
            postRepository.incrementLikeCount(postId);
            evenPublisher.publishEvent(new NotificationEvent(
                currentUser, author, NotificationType.LIKE_POST, postId, "POST", "đã thích bài viết của bạn"
            ));
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
        postRepository.save(rootPost);
        Post savedShare = postRepository.save(share);
        evenPublisher.publishEvent(new NotificationEvent(
            currentUser, 
            rootPost.getAuthor(), 
            NotificationType.SHARE_POST,
            rootPost.getId(), 
            "POST", 
            "đã chia sẻ bài viết của bạn."
        ));
        return mapToPostResponse(savedShare);
    }

    public Page<PostResponse> getPostsByAuthor(int page, int size) {
        User currentUser = getCurrentUser();
        Integer authorId = currentUser.getId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Post> posts = postRepository.findByAuthorId(authorId, pageable);
        return posts.map(this::mapToPostResponse);
    }

    public PostResponse mapToPostResponse(Post post) {
        User author = post.getAuthor();
        UserResponse authorDto = new UserResponse();
        authorDto.setId(author.getId());
        authorDto.setFullName(author.getFullName());
        authorDto.setAvatarUrl(author.getAvatarUrl());
        authorDto.setStudentCode(author.getStudentCode()); 

        List<PostMedia> mediaList = post.getMedia() == null ? new ArrayList<>() : post.getMedia();

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
                originalPostResponse = mapToPostResponse(post.getOriginalPost());
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
                .originalPost(originalPostResponse)
                .build();
    }

    // --- LOGIC MỚI: UPLOAD LÊN CLOUDINARY ---
    // Đổi tên từ storeFileToLocal -> storeFile
    public String storeFile(MultipartFile file) {
        try {            
            if (file.isEmpty()) {
                throw new RuntimeException("Không thể upload file rỗng.");
            }

            // Tạo tên file ngẫu nhiên để không bị trùng trên Cloud
            String fileName = UUID.randomUUID().toString(); 
            
            // Upload lên Cloudinary
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), 
                    ObjectUtils.asMap(
                        "public_id", "mini_social_network/" + fileName, // Folder trên cloud
                        "resource_type", "auto" // Tự động nhận diện ảnh/video/audio
                    ));

            // Trả về đường dẫn HTTPS (secure_url)
            return uploadResult.get("secure_url").toString();
            
        } catch (IOException e) {
            throw new RuntimeException("Lỗi upload file lên Cloudinary: " + file.getOriginalFilename(), e);
        }
    }

    private MediaType detectMediaType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) return MediaType.IMAGE;
        if (contentType.startsWith("video")) return MediaType.VIDEO;
        if (contentType.startsWith("audio")) return MediaType.AUDIO;
        return MediaType.IMAGE;
    }
}