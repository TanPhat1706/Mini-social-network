package com.example.backend.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private Cloudinary cloudinary;

    // --- ĐĂNG KÝ ---
    public User register(RegisterRequest req) {
        // 1. Kiểm tra tồn tại
        if (userRepository.existsByStudentCode(req.getStudentCode())) {
            throw new RuntimeException("Mã sinh viên / giảng viên đã tồn tại!");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email đã tồn tại!");
        }

        // 2. Map dữ liệu từ Request sang Entity
        User user = new User();
        user.setStudentCode(req.getStudentCode());
        user.setEmail(req.getEmail());
        user.setFullName(req.getFullName());


        user.setClassName(req.getClassName());
        // ⭐️ XỬ LÝ ROLE & BẢO MẬT (QUAN TRỌNG)
        String requestRole = req.getRole();
        
        if ("TEACHER".equalsIgnoreCase(requestRole)) {
            user.setRole("TEACHER");
        } else {
            // Mặc định là STUDENT nếu role rỗng hoặc sai
            // TUYỆT ĐỐI KHÔNG cho phép set ADMIN từ request này
            user.setRole("STUDENT"); 
        }

        // Đảm bảo user mới phải chờ duyệt
        user.setActive(false);

        // 3. Mã hóa mật khẩu trước khi lưu
        user.setPassword(passwordEncoder.encode(req.getPassword()));

        // Lưu xuống DB (các trường active, createdAt tự động xử lý bởi @PrePersist
        // trong Entity)
        return userRepository.save(user);
    }

    // --- ĐĂNG NHẬP ---
    public String login(LoginRequest req) {
        // 1. Tìm user bằng StudentCode HOẶC Email
        // (Truyền req.getIdentifier() vào cả 2 tham số để JPA check cả 2 cột)
        User user = userRepository.findByStudentCodeOrEmail(req.getIdentifier(), req.getIdentifier())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));
        System.out.println("USER: " + user.getStudentCode() + " - " + user.getEmail() + " - " + user.getPassword());

        // 2. Kiểm tra mật khẩu (So sánh pass thô và pass đã hash trong DB)
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu không đúng!");
        }

        // 3. Kiểm tra trạng thái hoạt động (Active)
        // Dùng Boolean.TRUE.equals để tránh lỗi NullPointerException nếu active bị null
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new RuntimeException("Tài khoản của bạn chưa được kích hoạt hoặc đã bị khóa!");
        }

        // 4. Cập nhật thời gian đăng nhập lần cuối
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // 5. Tạo JWT Token
        // Lưu ý: Dùng studentCode làm định danh (subject) trong Token
        return jwtUtil.generateToken(user.getStudentCode());
    }

    public List<UserResponse> searchUsers(String query) {
        List<User> users = userRepository.searchUsers(query);

        return users.stream().map(user -> UserResponse.builder()
                .id(user.getId())
                .studentCode(user.getStudentCode())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .className(user.getClassName())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build()).collect(Collectors.toList());
    }

    public User updateProfile(String studentCode, UpdateProfileRequest req, MultipartFile file) {
        User user = userRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1. Cập nhật thông tin Text
        if (req.getFullName() != null && !req.getFullName().isEmpty()) {
            user.setFullName(req.getFullName());
        }
        if (req.getClassName() != null) {
            user.setClassName(req.getClassName());
        }
        if (req.getBio() != null) {
            user.setBio(req.getBio());
        }

        // 2. Xử lý Upload Ảnh (Nếu có gửi kèm file)
        if (file != null && !file.isEmpty()) {
            try {
                // Upload lên Cloudinary
                Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                        ObjectUtils.asMap(
                            "public_id", "users_avatar/" + UUID.randomUUID().toString(),
                            "resource_type", "auto"
                        ));
                
                // Lấy link ảnh từ Cloudinary trả về
                String newAvatarUrl = uploadResult.get("secure_url").toString();
                
                // Lưu vào DB
                user.setAvatarUrl(newAvatarUrl);
                
            } catch (IOException e) {
                throw new RuntimeException("Lỗi upload avatar: " + e.getMessage());
            }
        }
        // Lưu ý: Nếu req.getAvatarUrl() có string (trường hợp user ko đổi ảnh), 
        // ta giữ nguyên logic cũ hoặc bỏ qua tùy nghiệp vụ. 
        // Ở đây ưu tiên File: Có File thì lấy File, không có File thì giữ nguyên ảnh cũ.

        return userRepository.save(user);
    }
}