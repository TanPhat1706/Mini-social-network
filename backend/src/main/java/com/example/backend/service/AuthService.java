package com.example.backend.service;

import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // --- ĐĂNG KÝ ---
    public User register(RegisterRequest req) {
        // 1. Kiểm tra tồn tại
        if (userRepository.existsByStudentCode(req.getStudentCode())) {
            throw new RuntimeException("Mã sinh viên đã tồn tại!");
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

        // 2. Kiểm tra mật khẩu (So sánh pass thô và pass đã hash trong DB)
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu không đúng!");
        }

        // 3. Kiểm tra trạng thái hoạt động (Active)
        // Dùng Boolean.TRUE.equals để tránh lỗi NullPointerException nếu active bị null
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new RuntimeException("Tài khoản đã bị khóa!");
        }

        // 4. Cập nhật thời gian đăng nhập lần cuối
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // 5. Tạo JWT Token
        // Lưu ý: Dùng studentCode làm định danh (subject) trong Token
        return jwtUtil.generateToken(user.getStudentCode());
    }
}