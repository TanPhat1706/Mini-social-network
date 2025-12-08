package com.example.backend.service; // Đảm bảo đúng tên package của bạn

import com.example.backend.model.User;
import com.example.backend.payload.request.RegisterRequest;
import com.example.backend.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenService refreshTokenService; // Dùng để quản lý Logout

    /**
     * Thực hiện logic Đăng ký User.
     *
     * @param request DTO chứa thông tin đăng ký
     * @return User Entity đã được lưu
     * @throws RuntimeException nếu StudentCode hoặc Email đã tồn tại
     */
    @Transactional
    public User registerNewUser(RegisterRequest request) {

        // Kiểm tra trùng lặp
        if (userRepository.existsByStudentCode(request.getStudentCode())) {
            throw new RuntimeException("Student Code already taken!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already in use!");
        }

        // Tạo Entity User
        User user = new User();
        user.setStudentCode(request.getStudentCode());
        user.setEmail(request.getEmail());
        // Băm mật khẩu
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setClassName(request.getClassName());
        user.setRole("USER");

        return userRepository.save(user);
    }

    /**
     * Xử lý logic Đăng xuất (Xóa Refresh Token khỏi DB).
     *
     * @param userId ID của User đăng xuất
     */
    @Transactional
    public void logoutUser(Integer userId) {
        // Xóa Refresh Token để vô hiệu hóa phiên đăng nhập
        refreshTokenService.deleteByUserId(userId);
    }
}