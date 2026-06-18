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
        // Khởi tạo service (nó sẽ tự tạo s3Client thật bên trong constructor)
        s3StorageService = new S3StorageService("dummyAccessKey", "dummySecretKey", region, bucketName);
        
        // Dùng Reflection tiêm s3Client GIẢ vào đè lên s3Client THẬT
        ReflectionTestUtils.setField(s3StorageService, "s3Client", s3Client);
    }

    @Test
    @DisplayName("Upload file lên S3 thành công và trả về URL chuẩn xác")
    void storeFile_whenValidFile_shouldUploadAndReturnUrl() {
        MockMultipartFile validFile = new MockMultipartFile(
                "file", "cover.jpg", "image/jpeg", "dummy image".getBytes()
        );

        // Giả lập s3Client gọi hàm putObject thành công
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String resultUrl = s3StorageService.storeFile(validFile);

        // Bắt Request gửi lên AWS xem cấu hình đúng không
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(1)).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest capturedRequest = requestCaptor.getValue();
        assertEquals(bucketName, capturedRequest.bucket());
        assertEquals("image/jpeg", capturedRequest.contentType());

        // Đảm bảo URL trả về đúng format của AWS S3
        assertNotNull(resultUrl);
        assertTrue(resultUrl.startsWith("https://my-aws-bucket.s3.ap-southeast-1.amazonaws.com/"),
                "URL phải bắt đầu bằng S3 endpoint chuẩn");
        assertTrue(resultUrl.endsWith(".jpg"), "URL phải kết thúc bằng đuôi .jpg");
    }

    @Test
    @DisplayName("Ném lỗi IllegalArgumentException khi file null hoặc rỗng")
    void storeFile_whenFileNullOrEmpty_shouldThrowException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> s3StorageService.storeFile(null));
        assertThrows(IllegalArgumentException.class, () -> s3StorageService.storeFile(emptyFile));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Ném RuntimeException khi quá trình đọc file xảy ra lỗi I/O")
    void storeFile_whenIOException_shouldThrowRuntimeException() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("error.txt");
        when(mockFile.getInputStream()).thenThrow(new IOException("S3 Upload Failed"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> s3StorageService.storeFile(mockFile));
        assertEquals("Failed to upload file to S3", ex.getMessage());
    }

    @Test
    @DisplayName("Kiểm tra tính hợp lệ của file (isValidFile)")
    void isValidFile_shouldReturnCorrectBoolean() {
        MockMultipartFile validFile = new MockMultipartFile("file", "data".getBytes());
        assertTrue(s3StorageService.isValidFile(validFile));
        assertFalse(s3StorageService.isValidFile(null));
    }

    @Test
    @DisplayName("Tự thêm https khi endpoint/public URL chỉ có hostname")
    void constructor_whenEndpointHasNoScheme_shouldAutoPrefixHttps() {
        S3StorageService service = new S3StorageService(
                "dummyAccessKey",
                "dummySecretKey",
                region,
                bucketName,
                "s3.ap-southeast-1.amazonaws.com",
                null
        );

        assertEquals(
                "https://s3.ap-southeast-1.amazonaws.com/" + bucketName + "/avatars/test.png",
                service.buildFileUrl("avatars/test.png")
        );
    }

    @Test
    @DisplayName("Bỏ qua endpoint rỗng giả và fallback về URL S3 mặc định")
    void constructor_whenEndpointIsDisabledValue_shouldFallbackToDefaultAwsUrl() {
        S3StorageService service = new S3StorageService(
                "dummyAccessKey",
                "dummySecretKey",
                region,
                bucketName,
                "null",
                "${AWS_URL}"
        );

        String resultUrl = service.buildFileUrl("covers/test.png");
        assertEquals(
                "https://my-aws-bucket.s3.ap-southeast-1.amazonaws.com/covers/test.png",
                resultUrl
        );
    }
}
