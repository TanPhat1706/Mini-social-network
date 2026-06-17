package com.example.backend.Moderation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/moderation/blacklist")
@PreAuthorize("hasRole('ADMIN')") // 🟢 Chỉ Admin mới được can thiệp
public class ModerationAdminController {

    @Autowired
    private BlacklistedWordRepository blacklistedWordRepository;

    @Autowired
    private AutoModerationService autoModerationService;

    // Xem danh sách từ cấm
    @GetMapping
    public ResponseEntity<List<BlacklistedWord>> getAllBadWords() {
        return ResponseEntity.ok(blacklistedWordRepository.findAll());
    }

    // Thêm từ cấm mới
    @PostMapping
    public ResponseEntity<?> addBadWord(@RequestBody Map<String, String> payload) {
        String word = payload.get("word"); // Nhận từ khóa qua key "word"
        if (word == null || word.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Từ khóa không được để trống");
        }

        // Logic thêm từ khóa...
        blacklistedWordRepository.save(new BlacklistedWord(word));
        autoModerationService.refreshCache();
        return ResponseEntity.ok("Đã thêm từ khóa cấm thành công");
    }

    // Xóa từ cấm
    @DeleteMapping("/{id}")
    public ResponseEntity<?> removeBadWord(@PathVariable Integer id) {
        blacklistedWordRepository.deleteById(id);
        autoModerationService.refreshCache(); // Cập nhật lại RAM
        return ResponseEntity.ok("Đã xóa từ khóa cấm");
    }
}