package com.example.backend.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ua_parser.Parser;
import ua_parser.Client;

import java.util.List;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import java.nio.file.Path;
import java.io.IOException;
import java.util.UUID;


@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SecurityHistoryRepository securityHistoryRepository;

    // Lấy đường dẫn thư mục uploads từ file config (mặc định là "uploads")
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

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
public String login(LoginRequest req, String sessionId) { // Thêm tham số sessionId
        User user = userRepository.findByStudentCodeOrEmail(req.getIdentifier(), req.getIdentifier())
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại!"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu không đúng!");
        }
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new RuntimeException("Tài khoản của bạn chưa được kích hoạt hoặc đã bị khóa!");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Truyền sessionId vào khi sinh Token
        return jwtUtil.generateToken(user.getStudentCode(), sessionId);
    }

    public List<UserResponse> searchUsers(String query) {
        List<User> users = userRepository.searchUsers(query);
        
        // Chuyển đổi từ Entity sang Response DTO
        return users.stream().map(user -> UserResponse.builder()
                .id(user.getId())
                .studentCode(user.getStudentCode())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .className(user.getClassName())
                .role(user.getRole())
                .active(user.getActive())
                .currentAvatarFrame(user.getCurrentAvatarFrame())
                .currentNameColor(user.getCurrentNameColor())
                .build()
        ).collect(Collectors.toList());
    }
    public String saveFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;

        // Tạo tên file độc nhất để tránh trùng lặp
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        
        // Tạo đường dẫn tuyệt đối đến thư mục uploads
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Lưu file
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Trả về đường dẫn tương đối để lưu vào DB (ví dụ: /uploads/abc.jpg)
        // Lưu ý: WebMvcConfig đã map "/uploads/**" vào thư mục này
        return "/uploads/" + fileName;
    }

    // --- MỚI: HÀM CẬP NHẬT PROFILE ---
    public User updateProfile(String studentCode, String fullName, String bio, 
                              String className, MultipartFile avatar, MultipartFile cover) throws IOException {
        User user = userRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (fullName != null) {
            fullName = fullName.trim();
            if (fullName.isEmpty()) {
                throw new RuntimeException("Họ và tên không được để trống");
            }
        }
        if (className != null) {
            className = className.trim();
        }
        if (bio != null) {
            bio = bio.trim();
            if (bio.length() > 500) {
                bio = bio.substring(0, 500);
            }
        }

        // Cập nhật thông tin text nếu có
        if (fullName != null) user.setFullName(fullName);
        if (bio != null) user.setBio(bio);
        if (className != null) user.setClassName(className);

        // Cập nhật Avatar nếu có upload
        if (avatar != null && !avatar.isEmpty()) {
            String avatarPath = saveFile(avatar);
            user.setAvatarUrl(avatarPath);
        }

        // Cập nhật Ảnh bìa nếu có upload
        if (cover != null && !cover.isEmpty()) {
            String coverPath = saveFile(cover);
            user.setCoverPhotoUrl(coverPath);
        }

        return userRepository.save(user);
    }
    // --- MỚI: LƯU LỊCH SỬ BẢO MẬT (ĐĂNG NHẬP) ---
    public void saveSecurityHistory(User user, String ipAddress, String userAgentString, String status, String sessionId) {
        SecurityHistory history = new SecurityHistory();
        history.setUser(user);
        history.setIpAddress(ipAddress != null ? ipAddress : "Unknown IP");
        history.setStatus(status);
        history.setSessionId(sessionId);
        history.setIsActive(true); // Mặc định là true khi mới đăng nhập

        if (userAgentString != null && !userAgentString.isEmpty()) {
            try {
                // Khởi tạo Parser để bóc tách chuỗi User-Agent
                Parser uaParser = new Parser();
                Client c = uaParser.parse(userAgentString);

                // 1. Lấy thông tin Trình duyệt (Ví dụ: Chrome 120)
                String browser = c.userAgent.family;
                if (c.userAgent.major != null) {
                    browser += " " + c.userAgent.major;
                }
                history.setBrowser(browser);

                // 2. Lấy thông tin Hệ điều hành & Thiết bị (Ví dụ: Windows 10, iOS - iPhone)
                String os = c.os.family;
                if (c.os.major != null) {
                    os += " " + c.os.major;
                }
                
                String device = c.device.family;
                
                // Gom chung lại cho đẹp UI. Nếu là máy tính thường device sẽ báo "Other"
                if ("Other".equalsIgnoreCase(device) || device.equals(os)) {
                    history.setDevice(os);
                } else {
                    history.setDevice(os + " (" + device + ")");
                }

            } catch (Exception e) {
                // Fallback nếu thư viện không parse được
                history.setBrowser("Unknown Browser");
                history.setDevice("Unknown Device");
            }
        } else {
            history.setBrowser("Unknown Browser");
            history.setDevice("Unknown Device");
        }

        // Lưu xuống Database
        securityHistoryRepository.save(history);
    }
}