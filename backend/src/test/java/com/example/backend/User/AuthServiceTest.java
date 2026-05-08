package com.example.backend.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Tránh ghi file thật ra workspace khi test updateProfile/saveFile
        ReflectionTestUtils.setField(authService, "uploadDir", tempDir.toString());
    }

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
    void login_whenUserNotFound_shouldThrow() {
        LoginRequest req = new LoginRequest();
        req.setIdentifier("x");
        req.setPassword("pw");

        when(userRepository.findByStudentCodeOrEmail("x", "x")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(req));
        assertEquals("Tài khoản không tồn tại!", ex.getMessage());
        verify(jwtUtil, never()).generateToken(anyString());
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

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(req));
        assertEquals("Mật khẩu không đúng!", ex.getMessage());
        verify(jwtUtil, never()).generateToken(anyString());
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

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(req));
        assertEquals("Tài khoản của bạn chưa được kích hoạt hoặc đã bị khóa!", ex.getMessage());
        verify(jwtUtil, never()).generateToken(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_whenSuccess_shouldUpdateLastLogin_save_andReturnJwt() {
        LoginRequest req = new LoginRequest();
        req.setIdentifier("sv");
        req.setPassword("pw");

        User u = new User();
        u.setStudentCode("SV001");
        u.setPassword("hashed");
        u.setActive(true);

        when(userRepository.findByStudentCodeOrEmail("sv", "sv")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("pw", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken("SV001")).thenReturn("token123");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        String token = authService.login(req);

        assertEquals("token123", token);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertNotNull(captor.getValue().getLastLogin());
        verify(jwtUtil).generateToken("SV001");
    }

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
    void updateProfile_withAvatarAndCover_shouldUseSaveFile_andPersistPaths() throws IOException {
        User u = new User();
        u.setStudentCode("SV001");
        when(userRepository.findByStudentCode("SV001")).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthService spy = Mockito.spy(authService);
        doReturn("/uploads/avatar.png", "/uploads/cover.png").when(spy).saveFile(any());

        MockMultipartFile avatar = new MockMultipartFile(
                "avatar", "a.png", "image/png", "a".getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile cover = new MockMultipartFile(
                "cover", "c.png", "image/png", "c".getBytes(StandardCharsets.UTF_8)
        );

        User saved = spy.updateProfile("SV001", null, null, null, avatar, cover);

        assertEquals("/uploads/avatar.png", saved.getAvatarUrl());
        assertEquals("/uploads/cover.png", saved.getCoverPhotoUrl());
        verify(userRepository).save(any(User.class));
        verify(spy, times(2)).saveFile(any());
    }
}

