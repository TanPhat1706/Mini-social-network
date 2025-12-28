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
import org.springframework.web.bind.annotation.*;

import java.util.*;

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

    @MessageMapping("/chat")
    public void processMessage(@Payload ChatMessage chatMessage) {
        // 1. Tạo Chat Room nếu chưa có
        chatRoomService.getChatId(chatMessage.getSenderId(), chatMessage.getReceiverId(), true);
        
        // 2. Lưu tin nhắn xuống DB
        chatMessage.setRead(false);
        ChatMessage saved = chatMessageRepository.save(chatMessage);

        // 3. GỬI REAL-TIME CHO NGƯỜI NHẬN (RECEIVER)
        // Logic cũ SAI: String.valueOf(chatMessage.getReceiverId())
        // Logic mới ĐÚNG: Phải tìm StudentCode của Receiver
        User receiver = userRepository.findById(chatMessage.getReceiverId()).orElse(null);
        
        if (receiver != null) {
            // Gửi tới studentCode (Principal Name của Socket)
            messagingTemplate.convertAndSendToUser(
                receiver.getStudentCode(), 
                "/queue/messages", 
                saved
            );
            System.out.println(">>> Sent WS to Receiver: " + receiver.getStudentCode());
        }

        // 4. (Tuỳ chọn) GỬI LẠI CHO NGƯỜI GỬI (SENDER)
        // Để đảm bảo tính đồng bộ trên các thiết bị khác của người gửi (nếu họ login nhiều nơi)
        User sender = userRepository.findById(chatMessage.getSenderId()).orElse(null);
        if (sender != null) {
            messagingTemplate.convertAndSendToUser(
                sender.getStudentCode(),
                "/queue/messages",
                saved
            );
        }
    }

    @GetMapping("/api/auth/messages/{senderId}/{receiverId}")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable Integer senderId,
            @PathVariable Integer receiverId) {
        return ResponseEntity.ok(
                chatMessageRepository.findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByTimestampAsc(senderId,
                        receiverId, receiverId, senderId));
    }

    // --- SỬA API NÀY ĐỂ TRẢ VỀ TRẠNG THÁI isRead ---
    @GetMapping("/api/auth/messages/recent")
    public ResponseEntity<List<ChatConversationDTO>> getRecentConversations() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return ResponseEntity.status(401).build();

        String principalName = auth.getName();
        User currentUser = userRepository.findByEmail(principalName).orElse(null);
        if (currentUser == null)
            currentUser = userRepository.findByStudentCode(principalName).orElse(null);
        if (currentUser == null)
            return ResponseEntity.notFound().build();

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
                boolean isRead = true;
                if (msg.getSenderId().equals(partnerId) && !msg.isRead()) {
                    isRead = false;
                }

                conversations.add(new ChatConversationDTO(
                        partner.getId(),
                        partner.getFullName(),
                        partner.getAvatarUrl(),
                        msg.getContent(),
                        msg.getTimestamp(),
                        isRead));
            }
        }
        return ResponseEntity.ok(conversations);
    }

    // --- THÊM API MỚI: ĐÁNH DẤU ĐÃ ĐỌC ---
    @PutMapping("/api/auth/messages/read/{partnerId}")
    public ResponseEntity<Void> markAsRead(@PathVariable Integer partnerId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String principalName = auth.getName();
        User currentUser = userRepository.findByEmail(principalName).orElse(null);
        if (currentUser == null)
            currentUser = userRepository.findByStudentCode(principalName).orElse(null);

        if (currentUser != null) {
            chatMessageRepository.markMessagesAsRead(partnerId, currentUser.getId());
        }
        return ResponseEntity.ok().build();
    }
}