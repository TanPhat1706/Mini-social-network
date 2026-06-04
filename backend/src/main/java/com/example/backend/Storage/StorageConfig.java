package com.example.backend.Storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ⭐ CẤU HÌNH CHUYỂN ĐỔI GIỮA LOCAL VÀ S3
 *
 * Chỉ cần đổi biến STORAGE_TYPE trong file .env:
 *   - STORAGE_TYPE=local  → Lưu file vào thư mục uploads/ trên server
 *   - STORAGE_TYPE=s3     → Upload file lên AWS S3
 *
 * Mặc định: local (nếu không khai báo STORAGE_TYPE)
 */
@Configuration
public class StorageConfig {

    // ========== CHẾ ĐỘ LOCAL (Mặc định) ==========
    @Bean
    @ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
    public FileStorageService localFileStorageService(
            @Value("${app.upload.dir:uploads}") String uploadDir) {
        System.out.println("🟢 [STORAGE] Đang sử dụng chế độ: LOCAL (uploads/" + uploadDir + ")");
        return new LocalFileStorageService(uploadDir);
    }

    // ========== CHẾ ĐỘ S3 (Cần có AWS credentials) ==========
    @Bean
    @ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
    public FileStorageService s3FileStorageService(
            @Value("${aws.access-key-id}") String accessKey,
            @Value("${aws.secret-access-key}") String secretKey,
            @Value("${aws.region}") String region,
            @Value("${aws.bucket-name}") String bucketName,
            @Value("${aws.endpoint:}") String endpoint,
            @Value("${aws.url:}") String publicBaseUrl) {
        System.out.println("🟢 [STORAGE] Đang sử dụng chế độ: AWS S3 (bucket: " + bucketName + ")");
        return new S3StorageService(accessKey, secretKey, region, bucketName, endpoint, publicBaseUrl);
    }
}
