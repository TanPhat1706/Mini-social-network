package com.example.backend.Storage;

import java.util.UUID;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    default String storeFile(MultipartFile file) {
        return storeFile(file, null);
    }

    String storeFile(MultipartFile file, String directory);
    void deleteFile(String fileUrl);

    boolean isValidFile(MultipartFile file);

    default String generateSafeFileName(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        String extension = "";

        if (StringUtils.hasText(originalFileName) && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        return UUID.randomUUID().toString() + extension;
    }
}
