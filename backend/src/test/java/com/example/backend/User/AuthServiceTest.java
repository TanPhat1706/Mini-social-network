package com.example.backend.User;

import com.example.backend.Storage.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityHistoryRepository securityHistoryRepository; // 🟢 THÊM MOCK NÀY

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private AuthService authService;

    // ... (Các test case register giữ nguyên vì không bị ảnh hưởng) ...
    @Test
    void register_whenStudentCodeExists_shouldThrowAndNotSave() {
        RegisterRequest req = new RegisterRequest();
        req.setStudentCode("SV001");
        req.setEmail("sv001@example.com");
        req.setFullName("A");
        req.setPassword("pw");

        when(userRepository.existsByStudentCode("SV001")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(req));
        assertEquals("Mã sinh viên / giảng viên đã tồn tại!", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_whenEmailExists_shouldThrowAndNotSave() {
        RegisterRequest req = new RegisterRequest();
        req.setStudentCode("SV002");
        req.setEmail("sv002@example.com");
        req.setFullName("B");
        req.setPassword("pw");

        when(userRepository.existsByStudentCode("SV002")).thenReturn(false);
        when(userRepository.existsByEmail("sv002@example.com")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(req));
        assertEquals("Email đã tồn tại!", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_withTeacherRole_shouldSetTeacherRole_andInactive_andEncodePassword() {
        RegisterRequest req = new RegisterRequest();
        req.setStudentCode("GV001");
        req.setEmail("gv001@example.com");
        req.setFullName("Teacher A");
        req.setPassword("raw");
        req.setClassName("K17");
        req.setRole("TEACHER");

        when(userRepository.existsByStudentCode(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("raw")).thenReturn("hashed");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        User saved = authService.register(req);

        assertNotNull(saved);
        User u = captor.getValue();
        assertEquals("GV001", u.getStudentCode());
        assertEquals("gv001@example.com", u.getEmail());
        assertEquals("Teacher A", u.getFullName());
        assertEquals("K17", u.getClassName());
        assertEquals("TEACHER", u.getRole());
        assertEquals(false, u.getActive());
        assertEquals("hashed", u.getPassword());
    }

    @Test
    void register_withInvalidOrAdminRole_shouldForceStudentRole() {
        RegisterRequest req = new RegisterRequest();
        req.setStudentCode("SV003");
        req.setEmail("sv003@example.com");
        req.setFullName("Student A");
        req.setPassword("raw");
        req.setRole("ADMIN"); // cố tình set ADMIN

        when(userRepository.existsByStudentCode(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("raw")).thenReturn("hashed");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        authService.register(req);

        assertEquals("STUDENT", captor.getValue().getRole());
    }
    
    @Test
    void register_withStudentRole_shouldSaveSuccessfully_AndSetInactive() {
        RegisterRequest req = new RegisterRequest();
        req.setStudentCode("SV005");
        req.setEmail("sv005@example.com");
        req.setFullName("Student Five");
        req.setPassword("raw");
        req.setRole("STUDENT");

        when(userRepository.existsByStudentCode(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("raw")).thenReturn("hashed");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        User saved = authService.register(req);

        assertNotNull(saved);
        assertEquals("STUDENT", saved.getRole());
        assertEquals(false, saved.getActive()); 
    }

    // ==========================================
    // 🟢 SỬA LẠI CÁC TEST CASE CHO LOGIN
    // ==========================================

    @Test
    void login_whenUserNotFound_shouldThrow() {
        LoginRequest req = new LoginRequest();
        req.setIdentifier("x");
        req.setPassword("pw");

        when(userRepository.findByStudentCodeOrEmail("x", "x")).thenReturn(Optional.empty());

        // 🟢 Sửa: Thêm tham số sessionId
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(req, "test-session-id"));
        assertEquals("Tài khoản hoặc mật khẩu gần chính xác!", ex.getMessage());
        
        // 🟢 Sửa: verify generateToken(anyString(), anyString())
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }

    @Test
    void login_whenPasswordMismatch_shouldThrow() {
        LoginRequest req = new LoginRequest();
        req.setIdentifier("sv");
        req.setPassword("wrong");

        User u = new User();
        u.setStudentCode("SV001");
        u.setEmail("sv@example.com");
        u.setPassword("hashed");
        u.setActive(true);

        when(userRepository.findByStudentCodeOrEmail("sv", "sv")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        // 🟢 Sửa: Thêm tham số sessionId
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(req, "test-session-id"));
        assertEquals("Tài khoản hoặc mật khẩu gần chính xác!", ex.getMessage());
        
        // 🟢 Sửa: verify
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_whenInactive_shouldThrow() {
        LoginRequest req = new LoginRequest();
        req.setIdentifier("sv");
        req.setPassword("pw");

        User u = new User();
        u.setStudentCode("SV001");
        u.setPassword("hashed");
        u.setActive(false);

        when(userRepository.findByStudentCodeOrEmail("sv", "sv")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("pw", "hashed")).thenReturn(true);

        // 🟢 Sửa: Thêm tham số sessionId
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(req, "test-session-id"));
        assertEquals("Tài khoản của bạn chưa được kích hoạt hoặc đã bị khóa!", ex.getMessage());
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_whenActiveIsNull_shouldThrow() {
        LoginRequest req = new LoginRequest();
        req.setIdentifier("sv_new");
        req.setPassword("pw");

        User u = new User();
        u.setStudentCode("SV_NEW");
        u.setPassword("hashed");
        u.setActive(null); 

        when(userRepository.findByStudentCodeOrEmail("sv_new", "sv_new")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("pw", "hashed")).thenReturn(true);

        // 🟢 Sửa: Thêm tham số sessionId
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(req, "test-session-id"));
        assertEquals("Tài khoản của bạn chưa được kích hoạt hoặc đã bị khóa!", ex.getMessage());
        verify(jwtUtil, never()).generateToken(anyString(), anyString());
    }

    @Test
    void login_whenSuccess_shouldUpdateLastLogin_save_andReturnJwt() {
        LoginRequest req = new LoginRequest();
        req.setIdentifier("sv");
        req.setPassword("pw");
        String sessionId = "test-session-id"; // Mock ID

        User u = new User();
        u.setStudentCode("SV001");
        u.setPassword("hashed");
        u.setActive(true);

        when(userRepository.findByStudentCodeOrEmail("sv", "sv")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("pw", "hashed")).thenReturn(true);
        // 🟢 Sửa: Mock generateToken với 2 tham số
        when(jwtUtil.generateToken("SV001", sessionId)).thenReturn("token123");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // 🟢 Sửa: Gọi login với 2 tham số
        String token = authService.login(req, sessionId);

        assertEquals("token123", token);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertNotNull(captor.getValue().getLastLogin());
        // 🟢 Sửa: verify
        verify(jwtUtil).generateToken("SV001", sessionId);
    }

    // ==========================================
    // 🟢 THÊM TEST CASE MỚI CHO TÍNH NĂNG SECURITY HISTORY
    // ==========================================
    @Test
    void saveSecurityHistory_shouldParseAgentAndSave() {
        User u = new User();
        u.setStudentCode("SV001");
        
        String ip = "127.0.0.1";
        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
        String status = "SUCCESS";
        String sessionId = "session-123";

        ArgumentCaptor<SecurityHistory> captor = ArgumentCaptor.forClass(SecurityHistory.class);
        when(securityHistoryRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        authService.saveSecurityHistory(u, ip, ua, status, sessionId);

        SecurityHistory saved = captor.getValue();
        assertEquals(u, saved.getUser());
        assertEquals("127.0.0.1", saved.getIpAddress());
        assertEquals("SUCCESS", saved.getStatus());
        assertEquals("session-123", saved.getSessionId());
        assertTrue(saved.getIsActive());
        
        // Kiểm tra xem uap-java có parse đúng Chrome và Windows không
        assertTrue(saved.getBrowser().contains("Chrome"));
        assertTrue(saved.getDevice().contains("Windows"));
    }

    @Test
    void saveSecurityHistory_withNullAgent_shouldFallback() {
        User u = new User();
        authService.saveSecurityHistory(u, null, null, "FAILED", "session-123");
        
        ArgumentCaptor<SecurityHistory> captor = ArgumentCaptor.forClass(SecurityHistory.class);
        verify(securityHistoryRepository).save(captor.capture());
        
        SecurityHistory saved = captor.getValue();
        assertEquals("Unknown IP", saved.getIpAddress());
        assertEquals("Unknown Browser", saved.getBrowser());
        assertEquals("Unknown Device", saved.getDevice());
    }

    // ... (Các test case dưới này giữ nguyên vì không liên quan) ...

    @Test
    void searchUsers_shouldMapToUserResponse() {
        User u1 = new User();
        u1.setId(1);
        u1.setStudentCode("SV001");
        u1.setFullName("A");
        u1.setAvatarUrl("/uploads/a.png");
        u1.setClassName("K17");
        u1.setRole("STUDENT");
        u1.setActive(true);
        u1.setCurrentAvatarFrame("frame1");
        u1.setCurrentNameColor("#fff");

        when(userRepository.searchUsers("A")).thenReturn(List.of(u1));

        List<UserResponse> res = authService.searchUsers("A");

        assertEquals(1, res.size());
        UserResponse r = res.get(0);
        assertEquals(1, r.getId());
        assertEquals("SV001", r.getStudentCode());
        assertEquals("A", r.getFullName());
        assertEquals("/uploads/a.png", r.getAvatarUrl());
        assertEquals("K17", r.getClassName());
        assertEquals("STUDENT", r.getRole());
        assertEquals(true, r.getActive());
        assertEquals("frame1", r.getCurrentAvatarFrame());
        assertEquals("#fff", r.getCurrentNameColor());
    }

    @Test
    void searchUsers_whenNoMatch_shouldReturnEmptyList() {
        when(userRepository.searchUsers("GHOST_USER")).thenReturn(List.of());

        List<UserResponse> res = authService.searchUsers("GHOST_USER");

        assertNotNull(res);
        assertTrue(res.isEmpty());
    }

    @Test
    void saveFile_whenFileIsNull_shouldReturnNull() throws IOException {
        String result = authService.saveFile(null);
        assertNull(result);
    }

    @Test
    void saveFile_whenFileIsEmpty_shouldReturnNull() throws IOException {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "text/plain", new byte[0]);
        String result = authService.saveFile(emptyFile);
        assertNull(result);
    }

    @Test
    void saveFile_whenValidFile_shouldSaveAndReturnPath() throws IOException {
        MockMultipartFile validFile = new MockMultipartFile(
                "file", "test.png", "image/png", "dummy image content".getBytes(StandardCharsets.UTF_8)
        );
        when(fileStorageService.storeFile(validFile, null)).thenReturn("https://cdn.example.com/test.png");

        String path = authService.saveFile(validFile);

        assertEquals("https://cdn.example.com/test.png", path);
        verify(fileStorageService).storeFile(validFile, null);
    }

    @Test
    void updateProfile_whenUserNotFound_shouldThrow() {
        when(userRepository.findByStudentCode("GHOST")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.updateProfile("GHOST", null, null, null, null, null));
        
        assertEquals("User not found", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateProfile_whenFullNameBlankAfterTrim_shouldThrow() {
        when(userRepository.findByStudentCode("SV001")).thenReturn(Optional.of(new User()));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.updateProfile("SV001", "   ", null, null, null, null));
        assertEquals("Họ và tên không được để trống", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateProfile_shouldTrimAndTruncateBio_andSave() throws IOException {
        User u = new User();
        u.setStudentCode("SV001");
        when(userRepository.findByStudentCode("SV001")).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        String longBio = "x".repeat(600);
        User saved = authService.updateProfile("SV001", "  Name  ", "  " + longBio + "  ", "  K17  ", null, null);

        assertEquals("Name", saved.getFullName());
        assertEquals("K17", saved.getClassName());
        assertNotNull(saved.getBio());
        assertEquals(500, saved.getBio().length());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateProfile_whenAllFieldsNull_shouldNotUpdateAnything() throws IOException {
        User u = new User();
        u.setStudentCode("SV001");
        u.setFullName("Old Name");
        u.setBio("Old Bio");
        
        when(userRepository.findByStudentCode("SV001")).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = authService.updateProfile("SV001", null, null, null, null, null);

        assertEquals("Old Name", saved.getFullName());
        assertEquals("Old Bio", saved.getBio());
        verify(userRepository).save(u);
    }

    @Test
    void updateProfile_withAvatarAndCover_shouldUseSaveFile_andPersistPaths() throws IOException {
        User u = new User();
        u.setStudentCode("SV001");
        when(userRepository.findByStudentCode("SV001")).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(fileStorageService.storeFile(any(MockMultipartFile.class), eq("avatars"))).thenReturn("https://cdn.example.com/avatars/avatar.png");
        when(fileStorageService.storeFile(any(MockMultipartFile.class), eq("covers"))).thenReturn("https://cdn.example.com/covers/cover.png");

        MockMultipartFile avatar = new MockMultipartFile(
                "avatar", "a.png", "image/png", "a".getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile cover = new MockMultipartFile(
                "cover", "c.png", "image/png", "c".getBytes(StandardCharsets.UTF_8)
        );

        User saved = authService.updateProfile("SV001", null, null, null, avatar, cover);

        assertEquals("https://cdn.example.com/avatars/avatar.png", saved.getAvatarUrl());
        assertEquals("https://cdn.example.com/covers/cover.png", saved.getCoverPhotoUrl());
        verify(userRepository).save(any(User.class));
        verify(fileStorageService).storeFile(avatar, "avatars");
        verify(fileStorageService).storeFile(cover, "covers");
    }
    // ==========================================
    // 🟢 BỔ SUNG TEST CASE: SECURITY HISTORY (Nhánh thiết bị di động & Chuỗi rỗng)
    // ==========================================

    @Test
    void saveSecurityHistory_withMobileAgent_shouldParseDeviceCorrectly() {
        User u = new User();
        String ip = "192.168.1.1";
        // User-Agent của iPhone
        String ua = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1";
        
        ArgumentCaptor<SecurityHistory> captor = ArgumentCaptor.forClass(SecurityHistory.class);
        when(securityHistoryRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        authService.saveSecurityHistory(u, ip, ua, "SUCCESS", "session-ios");

        SecurityHistory saved = captor.getValue();
        // Kiểm tra xem luồng else (ghép tên OS và Device) có hoạt động đúng không
        assertTrue(saved.getDevice().contains("iOS"));
        assertTrue(saved.getDevice().contains("iPhone")); // Kết quả mong muốn: "iOS 16 (iPhone)"
    }

    @Test
    void saveSecurityHistory_withEmptyAgent_shouldFallback() {
        User u = new User();
        ArgumentCaptor<SecurityHistory> captor = ArgumentCaptor.forClass(SecurityHistory.class);
        when(securityHistoryRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // Truyền chuỗi rỗng "" thay vì null
        authService.saveSecurityHistory(u, "127.0.0.1", "", "FAILED", "session-empty");

        SecurityHistory saved = captor.getValue();
        assertEquals("Unknown Browser", saved.getBrowser());
        assertEquals("Unknown Device", saved.getDevice());
    }

    // ==========================================
    // 🟢 BỔ SUNG TEST CASE: CHANGE PASSWORD (Đang thiếu hoàn toàn)
    // ==========================================

    @Test
    void changePassword_whenPasswordsDoNotMatch_shouldThrowBadRequest() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("oldPass");
        req.setNewPassword("newPass123");
        req.setConfirmPassword("differentPass"); // Không khớp

        BadRequestException ex = assertThrows(BadRequestException.class, 
                () -> authService.changePassword(1, req));
        assertEquals("Mật khẩu xác nhận không khớp", ex.getMessage());
        verify(userRepository, never()).findById(anyInt());
    }

    @Test
    void changePassword_whenUserNotFound_shouldThrowBadRequest() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("oldPass");
        req.setNewPassword("newPass123");
        req.setConfirmPassword("newPass123");

        when(userRepository.findById(99)).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class, 
                () -> authService.changePassword(99, req));
        assertEquals("Người dùng không tồn tại", ex.getMessage());
    }

    @Test
    void changePassword_whenOldPasswordIncorrect_shouldThrowBadRequest() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("wrongOldPass");
        req.setNewPassword("newPass123");
        req.setConfirmPassword("newPass123");

        User u = new User();
        u.setPassword("hashedOldPass");

        when(userRepository.findById(1)).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("wrongOldPass", "hashedOldPass")).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, 
                () -> authService.changePassword(1, req));
        assertEquals("Mật khẩu cũ không chính xác", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_whenNewPasswordSameAsOld_shouldThrowBadRequest() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("samePass");
        req.setNewPassword("samePass");
        req.setConfirmPassword("samePass");

        User u = new User();
        u.setPassword("hashedOldPass");

        when(userRepository.findById(1)).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("samePass", "hashedOldPass")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, 
                () -> authService.changePassword(1, req));
        assertEquals("Mật khẩu mới phải khác mật khẩu cũ", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_whenValidRequest_shouldUpdateAndSave() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("oldPass");
        req.setNewPassword("newPass123");
        req.setConfirmPassword("newPass123");

        User u = new User();
        u.setPassword("hashedOldPass");

        when(userRepository.findById(1)).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("oldPass", "hashedOldPass")).thenReturn(true);
        when(passwordEncoder.encode("newPass123")).thenReturn("hashedNewPass123");

        authService.changePassword(1, req);

        assertEquals("hashedNewPass123", u.getPassword());
        assertNotNull(u.getLastPasswordResetAt());
        verify(userRepository).save(u);
    }
}
