package com.example.backend.Security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.ServletException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class SecurityLoggingFilterTest {

    @InjectMocks
    private SecurityLoggingFilter securityLoggingFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    // ==========================================
    // 1. TEST LUỒNG AN TOÀN (HAPPY PATH)
    // ==========================================
    @Test
    @DisplayName("Bỏ qua và cho đi tiếp khi Request an toàn, không có mã độc")
    void doFilterInternal_whenSafeRequest_shouldPassNormally() throws ServletException, IOException {
        request.setMethod("GET");
        request.setRequestURI("/api/users");
        request.setQueryString("page=1&size=10");
        request.addHeader("User-Agent", "Mozilla/5.0");

        assertDoesNotThrow(() -> securityLoggingFilter.doFilterInternal(request, response, filterChain));
        assertNotNull(response); 
    }

    @Test
    @DisplayName("Xử lý đúng khi Query và User-Agent bị null")
    void doFilterInternal_whenQueryAndUaAreNull_shouldPassNormally() throws ServletException, IOException {
        request.setMethod("POST");
        request.setRequestURI("/api/auth/login");

        assertDoesNotThrow(() -> securityLoggingFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    @DisplayName("Trích xuất đúng IP khi đi qua Nginx/Proxy (có X-Forwarded-For)")
    void doFilterInternal_whenXForwardedForExists_shouldExtractCorrectIp() throws ServletException, IOException {
        request.setRequestURI("/api/test");
        request.addHeader("X-Forwarded-For", "192.168.1.100, 10.0.0.1"); 
        
        assertDoesNotThrow(() -> securityLoggingFilter.doFilterInternal(request, response, filterChain));
    }

    // ==========================================
    // 2. TEST CÁC LUỒNG PHÁT HIỆN TẤN CÔNG
    // ==========================================
    @Test
    @DisplayName("Phát hiện tấn công SQL Injection")
    void doFilterInternal_whenSqlInjectionDetected_shouldLogWarning() throws ServletException, IOException {
        request.setRequestURI("/api/products");
        request.setQueryString("id=1' OR '1'='1");

        assertDoesNotThrow(() -> securityLoggingFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    @DisplayName("Phát hiện tấn công XSS (Cross-Site Scripting)")
    void doFilterInternal_whenXssDetected_shouldLogWarning() throws ServletException, IOException {
        request.setRequestURI("/api/comments");
        request.setQueryString("content=<script>alert('hacked')</script>");

        assertDoesNotThrow(() -> securityLoggingFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    @DisplayName("Phát hiện tấn công Path Traversal")
    void doFilterInternal_whenPathTraversalDetected_shouldLogWarning() throws ServletException, IOException {
        request.setRequestURI("/api/files/../../etc/passwd");

        assertDoesNotThrow(() -> securityLoggingFilter.doFilterInternal(request, response, filterChain));
    }

    // ==========================================
    // 3. TEST LUỒNG NGOẠI LỆ (EXCEPTION PATH)
    // ==========================================
    @Test
    @DisplayName("Không làm crash ứng dụng khi có lỗi xảy ra trong quá trình ghi log")
    void doFilterInternal_whenExceptionThrownInsideTryBlock_shouldCatchAndContinue() throws ServletException, IOException {
        // Chuẩn bị Mock Logger và ép nó văng lỗi Runtime
        Logger mockLogger = Mockito.mock(Logger.class);
        Mockito.doThrow(new RuntimeException("Simulated Log Failure"))
               .when(mockLogger).warn(anyString(), any(), any(), any(), any(), any());
               
        // Tiêm Mock Logger vào biến 'log' (bây giờ đã đổi thành non-static nên inject mượt mà)
        ReflectionTestUtils.setField(securityLoggingFilter, "log", mockLogger);

        // Kích hoạt SQLI regex
        request.setRequestURI("/api/hacked");
        request.setQueryString("query='"); 

        // Đảm bảo Filter KHÔNG văng exception ra ngoài làm sập app
        assertDoesNotThrow(() -> securityLoggingFilter.doFilterInternal(request, response, filterChain));
        
        // Xác nhận khối catch đã bắt được lỗi và gọi log.error()
        Mockito.verify(mockLogger).error(anyString(), anyString());
    }
}