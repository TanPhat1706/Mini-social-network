package com.example.backend.Storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    default String storeFile(MultipartFile file) {
        return storeFile(file, null);
    }

    String storeFile(MultipartFile file, String directory);

    boolean isValidFile(MultipartFile file);
}
