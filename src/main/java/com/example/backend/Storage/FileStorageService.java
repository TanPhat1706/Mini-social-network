package com.example.backend.Storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeFile(MultipartFile file);
    boolean isValidFile(MultipartFile file);
}
