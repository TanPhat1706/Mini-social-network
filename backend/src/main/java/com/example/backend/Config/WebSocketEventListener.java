package com.example.backend.Config;

import com.example.backend.Presence.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final PresenceService presenceService;

    // Bắt sự kiện khi Client vượt qua bộ lọc Auth và kết nối thành công
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal userPrincipal = headerAccessor.getUser();

        if (userPrincipal != null) {
            String studentCode = userPrincipal.getName(); // Đây là username chứa trong JWT
            presenceService.markUserAsOnline(studentCode);
        }
    }

    // Bắt sự kiện khi Client rớt mạng, đóng trình duyệt hoặc tắt ChatBox
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal userPrincipal = headerAccessor.getUser();

        if (userPrincipal != null) {
            String studentCode = userPrincipal.getName();
            presenceService.markUserAsOffline(studentCode);
        }
    }
}