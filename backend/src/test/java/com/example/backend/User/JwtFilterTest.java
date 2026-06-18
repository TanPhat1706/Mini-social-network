package com.example.backend.User;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.apache.commons.logging.Log;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private SecurityHistoryRepository securityHistoryRepository;

    @Mock
    private Log mockLogger; // Mock logger của GenericFilterBean

    @InjectMocks
    private JwtFilter jwtFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        SecurityContextHolder.clearContext();

        // Tiêm mockLogger vào lớp cha (GenericFilterBean) để test luồng catch (Exception)
        ReflectionTestUtils.setField(jwtFilter, GenericFilterBean.class, "logger", mockLogger, Log.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Bỏ qua nếu không có header Authorization")
    void doFilterInternal_whenNoAuthHeader_shouldPassThrough() throws ServletException, IOException {
        jwtFilter.doFilterInternal(request, response, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Bỏ qua nếu Header không bắt đầu bằng Bearer")
    void doFilterInternal_whenInvalidAuthHeader_shouldPassThrough() throws ServletException, IOException {
        request.addHeader("Authorization", "Basic YWRtaW46YWRtaW4=");
        jwtFilter.doFilterInternal(request, response, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Ghi log lỗi nếu Token hỏng (extractUsername ném lỗi)")
    void doFilterInternal_whenTokenExtractionFails_shouldLogError() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer bad-token");
        when(jwtUtil.extractUsername("bad-token")).thenThrow(new RuntimeException("Bad JWT"));

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(mockLogger).error(eq("Lỗi trích xuất token"), any(RuntimeException.class));
    }

    @Test
    @DisplayName("Xác thực thành công khi Token hợp lệ và Session còn sống")
    void doFilterInternal_whenTokenAndSessionValid_shouldSetAuthentication() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer valid-token");
        String username = "SV001";
        String sessionId = "session-123";

        when(jwtUtil.extractUsername("valid-token")).thenReturn(username);
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractSessionId("valid-token")).thenReturn(sessionId);

        UserDetails userDetails = new User(username, "password", new ArrayList<>());
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);

        SecurityHistory history = new SecurityHistory();
        history.setIsActive(true); // Session CÒN SỐNG
        when(securityHistoryRepository.findBySessionId(sessionId)).thenReturn(Optional.of(history));

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(username, SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    @DisplayName("Từ chối truy cập khi Session đã bị khóa (isActive = false)")
    void doFilterInternal_whenSessionIsInactive_shouldDenyAccess() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer valid-token");
        String username = "SV001";
        String sessionId = "session-123";

        when(jwtUtil.extractUsername("valid-token")).thenReturn(username);
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractSessionId("valid-token")).thenReturn(sessionId);

        UserDetails userDetails = new User(username, "password", new ArrayList<>());
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);

        SecurityHistory history = new SecurityHistory();
        history.setIsActive(false); // 🔴 SESSION BỊ KHÓA
        when(securityHistoryRepository.findBySessionId(sessionId)).thenReturn(Optional.of(history));

        jwtFilter.doFilterInternal(request, response, filterChain);

        // SecurityContext KHÔNG được gán Authentication
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(mockLogger).warn("Từ chối truy cập: Session đã bị vô hiệu hóa đối với user " + username);
    }
    
    @Test
    @DisplayName("Từ chối truy cập khi không tìm thấy Session trong DB")
    void doFilterInternal_whenSessionNotFound_shouldDenyAccess() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer valid-token");
        
        when(jwtUtil.extractUsername("valid-token")).thenReturn("SV001");
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractSessionId("valid-token")).thenReturn("fake-session");
        
        when(userDetailsService.loadUserByUsername("SV001")).thenReturn(new User("SV001", "pw", new ArrayList<>()));
        // Không tìm thấy trong DB
        when(securityHistoryRepository.findBySessionId("fake-session")).thenReturn(Optional.empty());

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}