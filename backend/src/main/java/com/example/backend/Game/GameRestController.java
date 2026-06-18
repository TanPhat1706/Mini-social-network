package com.example.backend.Game;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;

@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class GameRestController {

    private final TicTacToeService ticTacToeService;
    private final UserRepository userRepository;

    @PostMapping("/create")
    public ResponseEntity<?> createRoom(Authentication authentication) {
        // 1. Lấy thông tin User hiện tại
        String principal = authentication.getName();
        User currentUser = userRepository.findByStudentCode(principal)
                .orElseGet(() -> userRepository.findByEmail(principal).orElse(null));

        if (currentUser == null) {
            return ResponseEntity.status(401).body("User not found");
        }

        // 2. Gọi Service để tạo một GameSession mới trong Database
        // Giả sử TicTacToeService có hàm createSession(hostId) trả về GameSession
        GameSession session = ticTacToeService.createSession(currentUser.getId());

        // 3. Trả về thông tin session để Frontend nhận được sessionId
        return ResponseEntity.ok(session);
    }
}