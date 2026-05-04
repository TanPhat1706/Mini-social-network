package com.example.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        // ========== Database Configuration (H2 In-memory) ==========
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=true",
        
        // ========== JPA Configuration (Trị tận gốc lỗi Enum) ==========
        "spring.jpa.database=default",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",

        // ========== File Upload Configuration ==========
        "app.upload.dir=./test-uploads",
        "spring.servlet.multipart.max-file-size=50MB",
        "spring.servlet.multipart.max-request-size=50MB",

        // ========== JWT Configuration ==========
        "jwt.secret=mock_secret_key_must_be_very_long_for_hs256_algorithm",
        "jwt.expiration=86400000",
        "jwt.refresh.expiration=604800000",

        // ========== CORS Configuration ==========
        "cors.allowed.origins=*",

        // ========== Server Configuration ==========
        "server.servlet.encoding.charset=UTF-8",
        "server.servlet.encoding.force=true",
        "server.port=8080",

        // ========== Swagger Configuration ==========
        "springdoc.swagger-ui.path=/swagger-ui.html",
        "springdoc.api-docs.path=/v3/api-docs"
})
class BackendApplicationTests {

    @Test
    void contextLoads() {
        // Test case này giờ đây sẽ pass xanh rờn vì ApplicationContext đã được nạp đủ biến!
    }

}