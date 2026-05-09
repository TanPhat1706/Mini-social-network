package com.example.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// 🟢 Chỉ cần gọi profile "test", Spring Boot sẽ tự động qua file application-test.properties để đọc cấu hình Database thật và JWT.
@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {

    @Test
    void contextLoads() {
        // Test case này đảm bảo toàn bộ Application Context (Database, Security, Bean...) 
        // khởi động trơn tru mà không bị crash. Xanh rờn là lụm tiền!
    }

}