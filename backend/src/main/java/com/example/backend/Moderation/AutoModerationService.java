package com.example.backend.Moderation;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@Service
public class AutoModerationService {

    @Autowired
    private BlacklistedWordRepository blacklistedWordRepository;

    // 🟢 DÙNG CACHE TRÊN RAM ĐỂ TỐC ĐỘ DUYỆT ĐẠT 0.001s (Không gọi DB)
    private Set<String> badWordsCache = new CopyOnWriteArraySet<>();

    @PostConstruct
    public void initCache() {
        refreshCache();
    }

    // Nạp lại Cache từ DB (Dùng khi Admin thêm/xóa từ khóa)
    public void refreshCache() {
        badWordsCache = blacklistedWordRepository.findAll()
                .stream()
                .map(BlacklistedWord::getWord)
                .collect(Collectors.toCollection(CopyOnWriteArraySet::new));
        System.out.println(">>> [Auto-Moderation] Đã nạp " + badWordsCache.size() + " từ khóa cấm vào RAM.");
    }

    // 🟢 HÀM DUYỆT BÀI TỰ ĐỘNG
    public boolean containsBadWord(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        String lowerContent = content.toLowerCase();
        
        // Quét nội dung xem có chứa từ cấm nào không
        for (String badWord : badWordsCache) {
            if (lowerContent.contains(badWord)) {
                return true; // Phát hiện từ cấm!
            }
        }
        return false; // Nội dung sạch
    }
}