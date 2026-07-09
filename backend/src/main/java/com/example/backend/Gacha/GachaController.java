package com.example.backend.Gacha;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import com.example.backend.Enum.CosmeticTheme;

import java.util.Map;

@RestController
@RequestMapping("/api/gacha")
@RequiredArgsConstructor
public class GachaController {

    private final GachaService gachaService;
    private final UserRepository userRepository;

    @PostMapping("/spin")
    public ResponseEntity<?> spinGacha(@RequestParam(defaultValue = "SUMMER") CosmeticTheme theme) {
        try {
            String studentCode = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByStudentCode(studentCode)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            GachaResponse response = gachaService.spin(user.getId(), theme);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    // Dán thêm 2 hàm này vào dưới hàm spinGacha()
    @GetMapping("/info")
    public ResponseEntity<?> getGachaInfo(@RequestParam(defaultValue = "SUMMER") CosmeticTheme theme) {
        try {
            String studentCode = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByStudentCode(studentCode)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return ResponseEntity.ok(gachaService.getGachaInfo(user.getId(), theme));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getGachaHistory(@RequestParam(defaultValue = "SUMMER") CosmeticTheme theme) {
        try {
            String studentCode = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByStudentCode(studentCode)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return ResponseEntity.ok(gachaService.getGachaHistory(user.getId(), theme));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}