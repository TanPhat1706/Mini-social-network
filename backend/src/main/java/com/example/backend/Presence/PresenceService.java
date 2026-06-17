package com.example.backend.Presence;

import com.example.backend.User.UserRepository;
import com.example.backend.User.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {

    private final UserRepository userRepository;

    // 🟢 BỘ NHỚ RAM ĐỂ LƯU CÁC USER ĐANG ONLINE (Key: studentCode, Value: Thời điểm Online)
    private final Map<String, LocalDateTime> activeUsers = new ConcurrentHashMap<>();

    public PresenceService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Khi User bật ChatBox (Kết nối STOMP)
    public void markUserAsOnline(String studentCode) {
        activeUsers.put(studentCode, LocalDateTime.now());
        System.out.println(">>> 🟢 [PRESENCE] User " + studentCode + " is ONLINE.");
    }

    // Khi User tắt tab, mất mạng (Ngắt kết nối STOMP)
    @Transactional
    public void markUserAsOffline(String studentCode) {
        activeUsers.remove(studentCode);
        System.out.println(">>> 🔴 [PRESENCE] User " + studentCode + " is OFFLINE.");

        // Cập nhật thời gian Offline xuống Database (Chạy bất đồng bộ nếu muốn tối ưu hơn)
        userRepository.findByStudentCode(studentCode).ifPresent(user -> {
            user.setLastActiveAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    // API hỗ trợ Frontend kiểm tra trạng thái của một người
    public UserPresenceDTO getUserPresence(String studentCode) {
        boolean isOnline = activeUsers.containsKey(studentCode);
        LocalDateTime lastSeen = null;

        if (isOnline) {
            lastSeen = LocalDateTime.now(); // Nếu đang online thì last seen là hiện tại
        } else {
            // Nếu offline, móc DB ra xem lần cuối là khi nào
            User user = userRepository.findByStudentCode(studentCode).orElse(null);
            if (user != null) {
                lastSeen = user.getLastActiveAt();
            }
        }
        return new UserPresenceDTO(studentCode, isOnline, lastSeen);
    }
}