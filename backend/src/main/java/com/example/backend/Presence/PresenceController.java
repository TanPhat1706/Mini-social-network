package com.example.backend.Presence;

import com.example.backend.Presence.PresenceService;
import com.example.backend.Presence.UserPresenceDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController // 🟢 Đánh dấu đây là API Controller trả về JSON
@RequiredArgsConstructor // 🟢 Tự động tạo Constructor cho biến final (Clean Code, thay thế @Autowired)
public class PresenceController {

    // Khai báo final để bắt buộc Spring phải tiêm Service vào qua Constructor
    private final PresenceService presenceService;

    // API Kiểm tra trạng thái hoạt động của một người
    @GetMapping("/api/users/{studentCode}/presence")
    public ResponseEntity<UserPresenceDTO> getUserPresence(@PathVariable String studentCode) {
        UserPresenceDTO presence = presenceService.getUserPresence(studentCode);
        return ResponseEntity.ok(presence);
    }
}