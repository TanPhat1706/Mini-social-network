package com.example.backend.Moderation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoModerationServiceTest {

    @Mock
    private BlacklistedWordRepository blacklistedWordRepository;

    @InjectMocks
    private AutoModerationService autoModerationService;

    @BeforeEach
    void setUp() {
        // Giả lập Entity BlacklistedWord từ Database
        // (Sử dụng cách khởi tạo an toàn nhất tùy thuộc vào Entity thực tế của bạn)
        BlacklistedWord word1 = new BlacklistedWord();
        word1.setWord("cấm");
        
        BlacklistedWord word2 = new BlacklistedWord();
        word2.setWord("độc hại");

        // Giả lập Repository trả về danh sách từ cấm
        when(blacklistedWordRepository.findAll()).thenReturn(List.of(word1, word2));

        // Kích hoạt nạp dữ liệu vào RAM (Giả lập hoạt động của @PostConstruct)
        autoModerationService.initCache();
    }

    @Test
    @DisplayName("initCache và refreshCache -> Nạp dữ liệu từ DB vào RAM thành công")
    void refreshCache_shouldLoadWordsFromDB() {
        // initCache() đã gọi refreshCache() 1 lần ở @BeforeEach
        verify(blacklistedWordRepository, times(1)).findAll();

        // Kiểm chứng tính hiệu quả của RAM Cache thông qua hàm check
        assertTrue(autoModerationService.containsBadWord("có từ cấm ở đây"));
    }

    @Test
    @DisplayName("Duyệt nội dung NULL -> Trả về false")
    void containsBadWord_whenNull_shouldReturnFalse() {
        assertFalse(autoModerationService.containsBadWord(null), "Nội dung null phải trả về false");
    }

    @Test
    @DisplayName("Duyệt nội dung RỖNG hoặc KHOẢNG TRẮNG -> Trả về false")
    void containsBadWord_whenEmptyOrWhitespace_shouldReturnFalse() {
        assertFalse(autoModerationService.containsBadWord(""), "Chuỗi rỗng phải trả về false");
        assertFalse(autoModerationService.containsBadWord("    "), "Chuỗi toàn khoảng trắng phải trả về false");
    }

    @Test
    @DisplayName("Duyệt nội dung SẠCH -> Trả về false")
    void containsBadWord_whenClean_shouldReturnFalse() {
        String cleanContent = "Đây là một bài viết hoàn toàn bình thường và trong sáng";
        assertFalse(autoModerationService.containsBadWord(cleanContent), "Nội dung sạch phải trả về false");
    }

    @Test
    @DisplayName("Duyệt nội dung CÓ TỪ CẤM (Test Case Insensitive) -> Trả về true")
    void containsBadWord_whenContainsToxicWord_shouldReturnTrue() {
        // Test 1: Chứa từ cấm viết thường
        assertTrue(autoModerationService.containsBadWord("Đừng có làm điều bị cấm"));
        
        // Test 2: Chứa từ cấm viết HOA (Hệ thống phải tự toLowerCase và bắt được)
        assertTrue(autoModerationService.containsBadWord("Nội dung này rất ĐỘC HẠI nhé!"));
    }
}