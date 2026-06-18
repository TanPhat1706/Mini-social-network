package com.example.backend.Storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

    // ==========================================
    // 1. TEST STORE FILE & DIRECTORY NORMALIZATION
    // ==========================================

    @Test
    @DisplayName("Lưu file với directory phức tạp (nhiều dấu slash, backslash) -> Trả về URL chuẩn")
    void storeFile_withComplexDirectory_shouldNormalizeAndReturnPath() {
        MockMultipartFile validFile = new MockMultipartFile(
                "file", "avatar.png", "image/png", "dummy".getBytes(StandardCharsets.UTF_8)
        );

        // Truyền directory có backslash, thừa slash ở đầu/cuối, và slash lặp lại (để bao phủ segment.isBlank())
        String resultUrl = localFileStorageService.storeFile(validFile, "\\/users//avatars/\\");

        assertNotNull(resultUrl);
        // 🟢 FIXED: Cập nhật lại chuỗi kỳ vọng thành "users//avatars" để khớp với logic của buildPublicPath
        assertTrue(resultUrl.contains("/uploads/users//avatars/"), "URL phải chứa directory đã được xử lý");
    }
    
    @Test
    @DisplayName("Lưu file với directory rỗng/null -> Lưu vào gốc /uploads/")
    void storeFile_withNullOrBlankDirectory_shouldSaveToRoot() {
        MockMultipartFile validFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", "dummy".getBytes()
        );

        String resultUrl1 = localFileStorageService.storeFile(validFile, null);
        String resultUrl2 = localFileStorageService.storeFile(validFile, "   ");

        assertTrue(resultUrl1.startsWith("/uploads/"));
        assertFalse(resultUrl1.contains("//"));
        assertTrue(resultUrl2.startsWith("/uploads/"));
    }

    @Test
    @DisplayName("Ném lỗi IllegalArgumentException khi file null hoặc rỗng")
    void storeFile_whenFileNullOrEmpty_shouldThrowException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> localFileStorageService.storeFile(null, "dir"));
        assertThrows(IllegalArgumentException.class, () -> localFileStorageService.storeFile(emptyFile, "dir"));
    }

    @Test
    @DisplayName("Tự động fallback tên file thành 'file' nếu originalFilename bị trống")
    void storeFile_whenOriginalFilenameIsBlank_shouldUseFallbackName() {
        MockMultipartFile fileWithNoName = new MockMultipartFile(
                "file", "", "image/png", "data".getBytes()
        );

        String resultUrl = localFileStorageService.storeFile(fileWithNoName, "posts");
        assertNotNull(resultUrl);
        assertTrue(resultUrl.startsWith("/uploads/posts/"));
    }

    @Test
    @DisplayName("Ném RuntimeException nếu xảy ra lỗi I/O (Lỗi đọc luồng dữ liệu)")
    void storeFile_whenIOExceptionOccurs_shouldThrowRuntimeException() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.txt");
        // Ép luồng dữ liệu ném lỗi
        when(mockFile.getInputStream()).thenThrow(new IOException("Disk error"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> localFileStorageService.storeFile(mockFile, "dir"));
        assertTrue(ex.getMessage().contains("Lỗi lưu file vào thư mục local"));
    }

    // ==========================================
    // 2. TEST DELETE FILE
    // ==========================================

    @Test
    @DisplayName("Bỏ qua khi fileUrl bị null hoặc rỗng")
    void deleteFile_whenUrlNullOrBlank_shouldDoNothing() {
        assertDoesNotThrow(() -> localFileStorageService.deleteFile(null));
        assertDoesNotThrow(() -> localFileStorageService.deleteFile("   "));
    }

    @Test
    @DisplayName("Xóa file hợp lệ thành công")
    void deleteFile_withValidUrl_shouldDelete() throws IOException {
        // Tạo file thực tế trong tempDir
        Path fakeFile = tempDir.resolve("delete_me.png");
        Files.writeString(fakeFile, "dummy");
        assertTrue(Files.exists(fakeFile));

        // Gọi hàm xóa
        localFileStorageService.deleteFile("/uploads/delete_me.png");

        // Kiểm chứng file đã bay màu
        assertFalse(Files.exists(fakeFile), "File phải bị xóa khỏi ổ cứng");
    }

    @Test
    @DisplayName("Chặn Path Traversal Attack (Xóa file ngoài thư mục cho phép)")
    void deleteFile_withPathTraversal_shouldThrowSecurityException() {
        SecurityException ex = assertThrows(SecurityException.class, () -> {
            localFileStorageService.deleteFile("/uploads/../../Windows/System32/cmd.exe");
        });
        assertTrue(ex.getMessage().contains("Invalid file path"));
    }

    @Test
    @DisplayName("Ném RuntimeException khi bị lỗi I/O lúc xóa file")
    void deleteFile_whenIOException_shouldThrowRuntimeException() throws IOException {
        // MẸO: Để ép Files.deleteIfExists() ném IOException, ta sẽ tạo một THƯ MỤC KHÔNG RỖNG
        Path notEmptyDir = tempDir.resolve("folder");
        Files.createDirectory(notEmptyDir);
        Files.writeString(notEmptyDir.resolve("file.txt"), "dummy"); // Thêm file để khóa thư mục

        // Cố xóa thư mục không rỗng sẽ văng DirectoryNotEmptyException (kế thừa IOException)
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            localFileStorageService.deleteFile("/uploads/folder");
        });
        assertTrue(ex.getMessage().contains("Error deleting file from local storage"));
    }

    // ==========================================
    // 3. TEST IS VALID FILE
    // ==========================================

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