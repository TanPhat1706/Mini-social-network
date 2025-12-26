package com.example.backend.Chat;

import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    @Autowired
    private ChatRoomService chatRoomService;
    @Autowired
    private UserRepository userRepository;

    // 1. WebSocket: Gửi/Nhận tin nhắn
    @MessageMapping("/chat")
    public void processMessage(@Payload ChatMessage chatMessage) {
        chatRoomService.getChatId(chatMessage.getSenderId(), chatMessage.getReceiverId(), true);
        ChatMessage saved = chatMessageRepository.save(chatMessage);

        messagingTemplate.convertAndSendToUser(
                String.valueOf(chatMessage.getReceiverId()),
                "/queue/messages",
                saved);
    }

    // 2. API: Lấy lịch sử tin nhắn chi tiết
    @GetMapping("/api/auth/messages/{senderId}/{receiverId}")
    public ResponseEntity<List<ChatMessage>> getChatHistory(
            @PathVariable Integer senderId,
            @PathVariable Integer receiverId) {
        return ResponseEntity.ok(chatMessageRepository
                .findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByTimestampAsc(
                        senderId, receiverId, receiverId, senderId));
    }

    // 3. API MỚI: Lấy danh sách Inbox (Hội thoại gần đây)
    // --- SỬA LOGIC API NÀY ---
    @GetMapping("/api/auth/messages/recent")
    public ResponseEntity<List<ChatConversationDTO>> getRecentConversations() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return ResponseEntity.status(401).build();

        String principalName = auth.getName(); // Đây có thể là Email HOẶC Mã sinh viên
        System.out.println("DEBUG: Đang tìm User với thông tin: " + principalName); // In ra console để check

        // 1. Thử tìm bằng Email
        User currentUser = userRepository.findByEmail(principalName).orElse(null);

        // 2. Nếu không thấy, thử tìm bằng Student Code
        if (currentUser == null) {
            currentUser = userRepository.findByStudentCode(principalName).orElse(null);
        }

        // 3. Nếu vẫn không thấy -> Trả về 404 (Lúc này mới thực sự là lỗi)
        if (currentUser == null) {
            System.out.println("DEBUG: Không tìm thấy User trong DB!");
            return ResponseEntity.notFound().build();
        }

        Integer userId = currentUser.getId();
        List<ChatMessage> allMessages = chatMessageRepository.findRecentMessages(userId);

        Map<Integer, ChatMessage> latestMessagesMap = new LinkedHashMap<>();
        for (ChatMessage msg : allMessages) {
            Integer partnerId = msg.getSenderId().equals(userId) ? msg.getReceiverId() : msg.getSenderId();
            latestMessagesMap.putIfAbsent(partnerId, msg);
        }

        List<ChatConversationDTO> conversations = new ArrayList<>();
        for (Map.Entry<Integer, ChatMessage> entry : latestMessagesMap.entrySet()) {
            Integer partnerId = entry.getKey();
            ChatMessage msg = entry.getValue();
            User partner = userRepository.findById(partnerId).orElse(null);

            if (partner != null) {
                conversations.add(new ChatConversationDTO(
                        partner.getId(),
                        partner.getFullName(),
                        partner.getAvatarUrl(),
                        msg.getContent(),
                        msg.getTimestamp(),
                        true));
            }
        }
        return ResponseEntity.ok(conversations);
    }

}