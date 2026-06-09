package com.example.backend.Storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalFileStorageServiceTest {

    private LocalFileStorageService localFileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Dùng thư mục tạm thời do JUnit 5 cung cấp để không sinh file rác
        localFileStorageService = new LocalFileStorageService(tempDir.toString());
    }

    @Test
    @DisplayName("Lưu file thành công và trả về đường dẫn hợp lệ")
    void storeFile_whenValidFile_shouldSaveAndReturnPath() {
        MockMultipartFile validFile = new MockMultipartFile(
                "file", "avatar.png", "image/png", "dummy content".getBytes(StandardCharsets.UTF_8)
        );

        String resultUrl = localFileStorageService.storeFile(validFile, "posts");

        assertNotNull(resultUrl);
        assertTrue(resultUrl.startsWith("/uploads/"), "URL phải bắt đầu bằng /uploads/");
        assertTrue(resultUrl.endsWith(".png"), "URL phải kết thúc bằng đuôi .png");
    }

    @Test
    @DisplayName("Ném lỗi IllegalArgumentException khi file null hoặc rỗng")
    void storeFile_whenFileNullOrEmpty_shouldThrowException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> localFileStorageService.storeFile(null));
        assertThrows(IllegalArgumentException.class, () -> localFileStorageService.storeFile(emptyFile));
    }

    @Test
    @DisplayName("Tự động fallback tên file thành 'file' nếu originalFilename bị trống")
    void storeFile_whenOriginalFilenameIsBlank_shouldUseFallbackName() {
        MockMultipartFile fileWithNoName = new MockMultipartFile(
                "file", "", "image/png", "data".getBytes()
        );

        String resultUrl = localFileStorageService.storeFile(fileWithNoName, "posts");
        // generateSafeFileName returns uuid (no extension when originalFilename is blank)
        assertNotNull(resultUrl);
        assertFalse(resultUrl.isBlank(), "URL không được rỗng");
        assertTrue(resultUrl.startsWith("/uploads/"), "URL phải bắt đầu bằng /uploads/");
    }

    @Test
    @DisplayName("Ném RuntimeException nếu xảy ra lỗi I/O (Lỗi đọc luồng dữ liệu)")
    void storeFile_whenIOExceptionOccurs_shouldThrowRuntimeException() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.txt");
        // Ép luồng dữ liệu ném lỗi
        when(mockFile.getInputStream()).thenThrow(new IOException("Disk error"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> localFileStorageService.storeFile(mockFile));
        assertTrue(ex.getMessage().contains("Lỗi lưu file vào thư mục local"));
    }

    @Test
    @DisplayName("Kiểm tra tính hợp lệ của file (isValidFile)")
    void isValidFile_shouldReturnCorrectBoolean() {
        MockMultipartFile validFile = new MockMultipartFile("file", "data".getBytes());
        MockMultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);

        assertTrue(localFileStorageService.isValidFile(validFile));
        assertFalse(localFileStorageService.isValidFile(emptyFile));
        assertFalse(localFileStorageService.isValidFile(null));
    }
}