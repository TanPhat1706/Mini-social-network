package com.example.backend.Notification;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;


    private User getCurrentUser() {
        String studentCode = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng (Token không hợp lệ?)"));
    }

    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getNotifications() {
        User currentUser = getCurrentUser();
        List<Notification> list = notificationRepository.findByReceiverIdOrderByCreatedAtDesc(Long.valueOf(currentUser.getId()));

        List<NotificationDTO> dtos = list.stream()
            .map(notificationService::mapToDTO)
            .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
}
