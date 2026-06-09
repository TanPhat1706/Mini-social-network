package com.example.backend.Storage;

import com.example.backend.Integration.BaseControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = StorageController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
class StorageControllerTest extends BaseControllerTest {

    // Mock cái Interface này, Spring sẽ không quan tâm bên dưới là S3 hay Local nữa
    @MockBean
    private FileStorageService fileStorageService;

    // ==========================================
    // 1. TEST LUỒNG THÀNH CÔNG (HAPPY PATH)
    // ==========================================
    @Test
    @WithMockUser
    @DisplayName("Upload file thành công trả về HTTP 201 và đường dẫn URL")
    void uploadFile_whenValidFile_shouldReturn201AndUrl() throws Exception {
        // Chuẩn bị một file giả mạo
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",               // Tên param phải khớp với @RequestParam("file")
                "test-image.jpg",     // Tên file gốc
                MediaType.IMAGE_JPEG_VALUE,
                "dummy image content".getBytes()
        );

        // Kịch bản: Khi Controller gọi Service, luôn trả về 1 URL giả định
        String expectedUrl = "https://my-aws-bucket.s3.amazonaws.com/uuid_test-image.jpg";
        when(fileStorageService.storeFile(any())).thenReturn(expectedUrl);

        // Bắn request (Lưu ý dùng multipart() thay vì post() thông thường)
        mockMvc.perform(multipart("/api/storage/upload")
                        .file(mockFile))
                .andExpect(status().isCreated()) // Kỳ vọng HTTP 201
                .andExpect(content().string(expectedUrl)); // Kỳ vọng body chứa URL
    }

    // ==========================================
    // 2. TEST LUỒNG THẤT BẠI (EXCEPTION PATH)
    // ==========================================
    @Test
    @WithMockUser
    @DisplayName("Upload file thất bại (do Service ném lỗi) thì trả về HTTP 500")
    void uploadFile_whenServiceThrowsException_shouldReturn500() throws Exception {
        // Chuẩn bị một file rỗng
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", 
                "empty.txt", 
                MediaType.TEXT_PLAIN_VALUE, 
                new byte[0]
        );

        // Kịch bản: Service ném lỗi giống hệt như logic thật
        when(fileStorageService.storeFile(any()))
                .thenThrow(new IllegalArgumentException("File is empty or missing"));

        // Controller của bạn không có khối try-catch, nên lỗi sẽ bị văng thẳng ra ngoài
        // và rơi vào cái GlobalExceptionHandler mà chúng ta đã setup lúc nãy (Exception.class -> 500)
        mockMvc.perform(multipart("/api/storage/upload")
                        .file(emptyFile))
                .andExpect(status().isInternalServerError()); // HTTP 500
    }
}