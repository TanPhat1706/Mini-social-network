package com.example.backend.Storage;

import java.util.Collections;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class StorageController {

    // Inject interface — tự động chọn Local hoặc S3 tùy cấu hình STORAGE_TYPE
    private final FileStorageService fileStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileUrl = fileStorageService.storeFile(file, "posts");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Collections.singletonMap("url", fileUrl));
    }
}
