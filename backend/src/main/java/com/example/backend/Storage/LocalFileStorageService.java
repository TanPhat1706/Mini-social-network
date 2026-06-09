package com.example.backend.Storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.web.multipart.MultipartFile;

/**
 * Lưu file vào thư mục local trên server.
 * URL trả về dạng: /uploads/uuid.png
 * WebMvcConfig đã map "/uploads/**" vào thư mục này.
 */
public class LocalFileStorageService implements FileStorageService {

    private final String uploadDir;

    public LocalFileStorageService(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public String storeFile(MultipartFile file, String directory) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or missing");
        }

        String fileName = generateSafeFileName(file);
        String normalizedDirectory = normalizeDirectory(directory);

        try {
            Path uploadPath = resolveUploadPath(normalizedDirectory);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return buildPublicPath(normalizedDirectory, fileName);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi lưu file vào thư mục local", e);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        try {
            String relativePath = fileUrl.replaceFirst("^/?uploads/", "");
            Path filePath = Paths.get(uploadDir).resolve(relativePath).normalize();
            if (!filePath.startsWith(Paths.get(uploadDir).normalize())) {
                throw new SecurityException("Invalid file path: " + fileUrl);
            }
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Error deleting file from local storage", e);
        }
    }

    @Override
    public boolean isValidFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private Path resolveUploadPath(String directory) {
        Path uploadPath = Paths.get(uploadDir);
        if (directory == null || directory.isBlank()) {
            return uploadPath;
        }

        for (String segment : directory.split("/")) {
            if (!segment.isBlank()) {
                uploadPath = uploadPath.resolve(segment);
            }
        }
        return uploadPath;
    }

    private String buildPublicPath(String directory, String fileName) {
        if (directory == null || directory.isBlank()) {
            return "/uploads/" + fileName;
        }
        return "/uploads/" + directory + "/" + fileName;
    }

    private String normalizeDirectory(String directory) {
        if (directory == null || directory.isBlank()) {
            return "";
        }

        String normalized = directory.trim().replace("\\", "/");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
