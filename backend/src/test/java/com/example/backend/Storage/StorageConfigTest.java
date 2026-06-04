package com.example.backend.Storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageConfigTest {

    @Test
    @DisplayName("Tạo Bean LocalFileStorageService thành công khi gọi hàm localFileStorageService")
    void localFileStorageService_shouldReturnLocalInstance() {
        StorageConfig config = new StorageConfig();
        FileStorageService service = config.localFileStorageService("dummy-dir");
        
        assertTrue(service instanceof LocalFileStorageService);
    }

    @Test
    @DisplayName("Tạo Bean S3StorageService thành công khi gọi hàm s3FileStorageService")
    void s3FileStorageService_shouldReturnS3Instance() {
        StorageConfig config = new StorageConfig();
        FileStorageService service = config.s3FileStorageService(
                "access", "secret", "ap-southeast-1", "bucket", "", ""
        );
        
        assertTrue(service instanceof S3StorageService);
    }
}
