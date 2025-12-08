package com.example.backend.controller;

import com.example.backend.model.RefreshToken;
import com.example.backend.model.User;
import com.example.backend.payload.request.LoginRequest;
import com.example.backend.payload.request.RegisterRequest;
import com.example.backend.payload.request.TokenRefreshRequest;
import com.example.backend.payload.response.JwtResponse;
import com.example.backend.payload.response.MessageResponse;
import com.example.backend.payload.response.TokenRefreshResponse;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.jwt.JwtUtils;
import com.example.backend.security.services.UserDetailsImpl;
import com.example.backend.service.AuthService;
import com.example.backend.service.RefreshTokenService;

import jakarta.persistence.criteria.CriteriaBuilder.In;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    RefreshTokenService refreshTokenService;

    @Autowired
    AuthService authService;

    // --- 1. ENDPOINT ĐĂNG KÝ (REGISTER) ---
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByStudentCode(request.getStudentCode())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Student Code already taken!"));
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // Tạo User mới
        User user = new User();
        user.setStudentCode(request.getStudentCode());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword())); // Băm mật khẩu
        user.setFullName(request.getFullName());
        user.setRole("USER"); // Thiết lập vai trò mặc định

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    // --- 2. ENDPOINT ĐĂNG NHẬP (LOGIN) ---
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        // 1. Xác thực người dùng
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getStudentCode(), loginRequest.getPassword()));

        // Thiết lập SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Lấy thông tin chi tiết của User
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // 2. Tạo Access Token (JWT ngắn hạn)
        String accessToken = jwtUtils.generateJwtToken(authentication);

        // 3. Tạo Refresh Token và lưu vào DB
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());

        // 4. Trả về thông tin User và Tokens
        return ResponseEntity.ok(new JwtResponse(
                accessToken,
                refreshToken.getToken(), // Refresh Token
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                userDetails.getFullName(),
                userDetails.getRole()

        // ... Thêm các thông tin khác nếu cần (role, fullName, v.v.)
        ));
    }

    // --- 3. ENDPOINT LÀM MỚI TOKEN (REFRESH TOKEN) ---
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration) // Kiểm tra Refresh Token có hết hạn không
                .map(RefreshToken::getUser) // Lấy thông tin User từ Refresh Token
                .map(user -> {
                    // Tạo Access Token mới
                    String accessToken = jwtUtils.generateJwtTokenFromUsername(user.getStudentCode());

                    // Trả về Access Token mới và giữ nguyên Refresh Token (hoặc tạo Refresh Token
                    // mới nếu bạn muốn)
                    return ResponseEntity.ok(new TokenRefreshResponse(accessToken, requestRefreshToken));
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }

    // --- 4. ENDPOINT ĐĂNG XUẤT (LOGOUT) ---
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        // Lấy ID của user đang đăng nhập từ Security Context
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        Integer userId = userDetails.getId();

        authService.logoutUser(userId);

        return ResponseEntity.ok(new MessageResponse("Log out successful!"));
    }
}