package com.example.backend.Storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    private S3StorageService s3StorageService;

    @Mock
    private S3Client s3Client;

    private final String bucketName = "my-aws-bucket";
    private final String region = "ap-southeast-1";

    @BeforeEach
    void setUp() {
        // Cấu hình S3 mặc định không có Custom Endpoint hay Public URL
        s3StorageService = new S3StorageService("accessKey", "secretKey", region, bucketName);
        ReflectionTestUtils.setField(s3StorageService, "s3Client", s3Client);
    }

    // ==========================================
    // 1. TEST STORE FILE & NORMALIZE DIRECTORY
    // ==========================================

    @Test
    @DisplayName("Upload file với Directory hợp lệ -> Xử lý dấu slash và trả về URL AWS")
    void storeFile_withValidDirectory_shouldUploadAndReturnUrl() {
        MockMultipartFile validFile = new MockMultipartFile("file", "cover.jpg", "image/jpeg", "data".getBytes());
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // Test nhánh normalizeDirectory: truyền vào "/posts/images/" -> phải chuẩn hóa thành "posts/images"
        String resultUrl = s3StorageService.storeFile(validFile, "/posts/images/");

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        assertTrue(requestCaptor.getValue().key().startsWith("posts/images/"), "Key phải chứa directory đã chuẩn hóa");
        assertNotNull(resultUrl);
        assertTrue(resultUrl.contains("amazonaws.com/posts/images/"));
    }

    @Test
    @DisplayName("Upload file với Directory rỗng hoặc null -> Lưu ở Root")
    void storeFile_withNullOrEmptyDirectory_shouldUploadToRoot() {
        MockMultipartFile validFile = new MockMultipartFile("file", "test.png", "image/png", "data".getBytes());
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        s3StorageService.storeFile(validFile, "   "); // Khoảng trắng
        
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        
        assertFalse(requestCaptor.getValue().key().contains("/"), "Key không được chứa dấu slash nếu thư mục rỗng");
    }

    @Test
    @DisplayName("Ném lỗi IllegalArgumentException khi file null hoặc rỗng")
    void storeFile_whenFileNullOrEmpty_shouldThrowException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> s3StorageService.storeFile(null, "dir"));
        assertThrows(IllegalArgumentException.class, () -> s3StorageService.storeFile(emptyFile, "dir"));
    }

    @Test
    @DisplayName("Ném RuntimeException khi quá trình đọc file xảy ra lỗi I/O")
    void storeFile_whenIOException_shouldThrowRuntimeException() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("error.txt");
        when(mockFile.getInputStream()).thenThrow(new IOException("S3 Upload Failed"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> s3StorageService.storeFile(mockFile, "dir"));
        assertEquals("Failed to upload file to S3", ex.getMessage());
    }

    // ==========================================
    // 2. TEST DELETE FILE & EXTRACT OBJECT KEY
    // ==========================================

    @Test
    @DisplayName("Xóa file thành công với Default AWS URL")
    void deleteFile_withDefaultAwsUrl_shouldDelete() {
        String fileUrl = "https://my-aws-bucket.s3.ap-southeast-1.amazonaws.com/posts/img.jpg";
        
        s3StorageService.deleteFile(fileUrl);
        
        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertEquals("posts/img.jpg", captor.getValue().key());
    }

    @Test
    @DisplayName("Xóa file thành công khi xài Custom Endpoint (MinIO)")
    void deleteFile_withCustomEndpoint_shouldDelete() {
        S3StorageService customService = new S3StorageService("ak", "sk", region, bucketName, "https://minio.com", null);
        ReflectionTestUtils.setField(customService, "s3Client", s3Client);

        customService.deleteFile("https://minio.com/my-aws-bucket/avatars/1.png");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertEquals("avatars/1.png", captor.getValue().key());
    }

    @Test
    @DisplayName("Xóa file thành công khi xài Public CDN Base URL")
    void deleteFile_withPublicBaseUrl_shouldDelete() {
        S3StorageService cdnService = new S3StorageService("ak", "sk", region, bucketName, null, "https://cdn.domain.com");
        ReflectionTestUtils.setField(cdnService, "s3Client", s3Client);

        cdnService.deleteFile("https://cdn.domain.com/covers/2.png");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertEquals("covers/2.png", captor.getValue().key());
    }

    @Test
    @DisplayName("Hủy thao tác xóa nếu URL bị null, rỗng hoặc parse thất bại")
    void deleteFile_whenInvalidUrl_shouldDoNothing() {
        s3StorageService.deleteFile(null);
        s3StorageService.deleteFile("   ");
        s3StorageService.deleteFile("invalid uri %^&"); // Bắn Exception nội bộ và return null objectKey

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("Bắt lỗi RuntimeException nếu S3 Client gặp sự cố khi xóa")
    void deleteFile_whenS3ClientThrows_shouldWrapInRuntimeException() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenThrow(new RuntimeException("AWS Error"));
        
        RuntimeException ex = assertThrows(RuntimeException.class, 
                () -> s3StorageService.deleteFile("https://my-aws-bucket.s3.ap-southeast-1.amazonaws.com/test.jpg"));
        
        assertTrue(ex.getMessage().contains("Failed to delete file from S3"));
    }

    // ==========================================
    // 3. TEST CÁC HÀM NORMALIZATION VÀ VALIDATION
    // ==========================================

    @Test
    @DisplayName("isValidFile kiểm tra chính xác")
    void isValidFile_shouldReturnCorrectBoolean() {
        MockMultipartFile validFile = new MockMultipartFile("file", "data".getBytes());
        assertTrue(s3StorageService.isValidFile(validFile));
        assertFalse(s3StorageService.isValidFile(null));
        assertFalse(s3StorageService.isValidFile(new MockMultipartFile("f", new byte[0])));
    }

    @Test
    @DisplayName("normalizeBaseUrl: Thêm https nếu thiếu scheme, cắt slash thừa")
    void normalizeBaseUrl_shouldAutoPrefixAndTrim() {
        S3StorageService service = new S3StorageService("ak", "sk", region, bucketName, "minio.local:9000/", null);
        String url = service.buildFileUrl("test.png");
        
        // Nó phải tự thêm https:// và xóa dấu / ở cuối endpoint
        assertEquals("https://minio.local:9000/my-aws-bucket/test.png", url);
    }

    @Test
    @DisplayName("normalizeBaseUrl: Vô hiệu hóa endpoint nếu mang giá trị null, undefined, Placeholder")
    void normalizeBaseUrl_whenDisabledValues_shouldFallbackToDefaultAwsUrl() {
        // Khởi tạo service với các string đặc biệt từ .env lỗi
        S3StorageService service = new S3StorageService("ak", "sk", region, bucketName, "undefined", "${CDN_URL}");
        
        String url = service.buildFileUrl("file.png");
        
        // Rơi về default S3 bucket URL
        assertEquals("https://my-aws-bucket.s3.ap-southeast-1.amazonaws.com/file.png", url);
    }
    
    @Test
    @DisplayName("normalizeBaseUrl: Bỏ qua nếu host không hợp lệ (Host bị null)")
    void normalizeBaseUrl_whenInvalidHost_shouldFallbackToDefaultAwsUrl() {
        // Truyền vào 3 dấu slash "///" + path, Java URI sẽ parse ra Host = null
        // Cố tình thêm chữ đằng sau để tránh bị vòng lặp cắt dấu "/" ở cuối
        S3StorageService service = new S3StorageService("ak", "sk", region, bucketName, "https:///no-host-path", null);
        String url = service.buildFileUrl("file.png");
        
        assertEquals("https://my-aws-bucket.s3.ap-southeast-1.amazonaws.com/file.png", url);
    }
}