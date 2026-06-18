package com.example.backend.Chat;

import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import com.example.backend.Enum.ReactionType;
import com.example.backend.Event.ReadEvent;
import com.example.backend.Event.TypingEvent;
import com.example.backend.Enum.MessageType;

import jakarta.transaction.Transactional;

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
                    saved);
            System.out.println(">>> Sent WS to Receiver: " + receiver.getStudentCode());
        }

        // 4. (Tuỳ chọn) GỬI LẠI CHO NGƯỜI GỬI (SENDER)
        // Để đảm bảo tính đồng bộ trên các thiết bị khác của người gửi (nếu họ login
        // nhiều nơi)
        User sender = userRepository.findById(chatMessage.getSenderId()).orElse(null);
        if (sender != null) {
            messagingTemplate.convertAndSendToUser(
                    sender.getStudentCode(),
                    "/queue/messages",
                    saved);
        }
    }

    // 🟢 CẬP NHẬT LẠI API GET LỊCH SỬ CHAT
    @GetMapping("/api/auth/messages/{senderId}/{receiverId}")
    public ResponseEntity<List<ChatMessage>> getChatHistory(
            @PathVariable Integer senderId,
            @PathVariable Integer receiverId) {

        // Trả về lịch sử, truyền senderId làm requesterId để lọc tin nhắn đã thu hồi 1
        // chiều
        return ResponseEntity.ok(
                chatMessageRepository.getHistoryWithRevokeFilter(senderId, receiverId, senderId));
    }

    // =========================================================================
    // 🟢 ĐÃ TỐI ƯU: Đón nhận trực tiếp DTO từ Database (Sạch bóng lỗi di truyền)
    // =========================================================================
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

        // Vì ChatMessageRepository giờ đã tự nén dữ liệu và kết nối bảng dưới DB,
        // Chúng ta chỉ cần gọi hàm và hứng thẳng danh sách kết quả, không cần lặp map thủ công nữa!
        List<ChatConversationDTO> conversations = chatMessageRepository.findRecentMessages(userId);

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

    // 🟢 THÊM MỚI: API Thả Cảm Xúc qua STOMP
    @MessageMapping("/chat.react")
    @Transactional
    public void reactToMessage(@Payload ReactionRequest request) {
        ChatMessage message = chatMessageRepository.findById(request.getMessageId()).orElse(null);
        if (message == null)
            return;

        ChatReaction existingReaction = message.getReactions().stream()
                .filter(r -> r.getUserId().equals(request.getUserId()))
                .findFirst().orElse(null);

        boolean isNewReaction = false;
        ReactionType finalReaction = request.getReactionType();

        if (existingReaction != null) {
            message.getReactions().remove(existingReaction);
            if (existingReaction.getReactionType() == request.getReactionType()) {
                finalReaction = null; // Bấm lại cảm xúc cũ -> Gỡ (Unlike)
            } else {
                isNewReaction = true; // Chuyển sang cảm xúc khác
            }
        } else {
            isNewReaction = true; // Thả cảm xúc mới
        }

        if (finalReaction != null) {
            ChatReaction newReaction = ChatReaction.builder()
                    .chatMessage(message)
                    .userId(request.getUserId())
                    .reactionType(finalReaction)
                    .build();
            message.getReactions().add(newReaction);
        }

        ChatMessage updatedMessage = chatMessageRepository.save(message);
        broadcastUpdatedMessage(updatedMessage);

        // 🟢 TẠO TIN NHẮN "ẢO" ĐỂ HIỂN THỊ THÔNG BÁO VÀO INBOX TRÊN HEADER
        // Chỉ gửi nếu: Là thả cảm xúc mới VÀ người thả không phải là tác giả của tin
        // nhắn đó
        if (isNewReaction && finalReaction != null && !request.getUserId().equals(message.getSenderId())) {
            String emoji = getEmojiForReaction(finalReaction);

            ChatMessage systemMsg = ChatMessage.builder()
                    .senderId(request.getUserId())
                    .receiverId(message.getSenderId()) // Gửi tới người chủ tin nhắn
                    .content("đã thả " + emoji + " vào tin nhắn của bạn")
                    .messageType(MessageType.SYSTEM)
                    .isRead(false) // Báo đỏ chưa đọc trên Header
                    .build();

            ChatMessage savedSystemMsg = chatMessageRepository.save(systemMsg);

            // Bắn qua WS để Header người nhận nhảy số đếm ngay lập tức
            User receiver = userRepository.findById(message.getSenderId()).orElse(null);
            if (receiver != null) {
                messagingTemplate.convertAndSendToUser(
                        receiver.getStudentCode(),
                        "/queue/messages",
                        savedSystemMsg);
            }
        }
    }

    // 🟢 THÊM MỚI: API Thu Hồi Tin Nhắn qua STOMP
    @MessageMapping("/chat.revoke")
    public void revokeMessage(@Payload RevokeRequest request) {
        ChatMessage message = chatMessageRepository.findById(request.getMessageId()).orElse(null);
        if (message == null)
            return;

        boolean isSender = message.getSenderId().equals(request.getRequesterId());
        boolean isReceiver = message.getReceiverId().equals(request.getRequesterId());

        if (!isSender && !isReceiver)
            return;

        // Cờ kiểm soát
        boolean isModified = false;
        boolean isRevokedEveryone = false; // 🟢 Thêm cờ để biết là thu hồi 2 phía

        if ("EVERYONE".equalsIgnoreCase(request.getRevokeType()) && isSender) {
            message.setIsDeletedEveryone(true);
            message.setContent("Tin nhắn đã thu hồi");
            message.getReactions().clear();
            isModified = true;
            isRevokedEveryone = true; // Đánh dấu đã thu hồi 2 phía
        } else if ("SELF".equalsIgnoreCase(request.getRevokeType())) {
            message.setDeletedBySenderId(request.getRequesterId());
            isModified = true;
        }

        // TỐI ƯU: Nếu không có thay đổi gì (hành động không hợp lệ), DỪNG LẠI NGAY!
        if (!isModified) {
            return;
        }

        ChatMessage updatedMessage = chatMessageRepository.save(message);
        broadcastUpdatedMessage(updatedMessage);

        // 🟢 TẠO TIN NHẮN "ẢO" ĐỂ HIỂN THỊ THÔNG BÁO VÀO INBOX KHI THU HỒI
        if (isRevokedEveryone) {
            ChatMessage systemMsg = ChatMessage.builder()
                    .senderId(request.getRequesterId()) // Người ấn thu hồi (luôn là Sender)
                    .receiverId(message.getReceiverId()) // Gửi thông báo tới người kia
                    .content("đã thu hồi một tin nhắn")
                    .messageType(MessageType.SYSTEM)
                    .isRead(false) // Báo đỏ chưa đọc trên Header
                    .build();

            ChatMessage savedSystemMsg = chatMessageRepository.save(systemMsg);

            // Bắn qua WS để Header người nhận nhảy số đếm ngay lập tức
            User receiver = userRepository.findById(message.getReceiverId()).orElse(null);
            if (receiver != null) {
                messagingTemplate.convertAndSendToUser(
                        receiver.getStudentCode(),
                        "/queue/messages",
                        savedSystemMsg);
            }
        }
    }

    // 🟢 HÀM PHỤ TRỢ: Gửi tin nhắn cập nhật cho cả 2 phía
    private void broadcastUpdatedMessage(ChatMessage message) {
        User receiver = userRepository.findById(message.getReceiverId()).orElse(null);
        User sender = userRepository.findById(message.getSenderId()).orElse(null);

        if (receiver != null) {
            messagingTemplate.convertAndSendToUser(receiver.getStudentCode(), "/queue/messages", message);
        }
        if (sender != null) {
            messagingTemplate.convertAndSendToUser(sender.getStudentCode(), "/queue/messages", message);
        }
    }

    // 🟢 HÀM PHỤ TRỢ MAPPING ENUM SANG ICON
    private String getEmojiForReaction(ReactionType type) {
        return switch (type) {
            case LOVE -> "❤️";
            case HAHA -> "😆";
            case WOW -> "😮";
            case SAD -> "😢";
            case ANGRY -> "😡";
            case LIKE -> "👍";
        };
    }
    // Import UserRepository và SimpMessagingTemplate nếu chưa có
    
    @MessageMapping("/chat.typing")
    public void handleTyping(TypingEvent event) {
        // 1. Tìm User nhận để lấy studentCode (dùng làm địa chỉ gửi tới)
        User receiver = userRepository.findById(event.getReceiverId())
            .orElse(null);
            
        if (receiver != null && receiver.getStudentCode() != null) {
            // 2. Chuyển tiếp tín hiệu thẳng sang Frontend của người nhận (Bỏ qua việc lưu Database)
            messagingTemplate.convertAndSendToUser(
                receiver.getStudentCode(),
                "/queue/typing",
                event
            );
        }
    }
    // 🟢 LUỒNG TRUNG CHUYỂN TÍN HIỆU "ĐÃ XEM"
    @MessageMapping("/chat.read")
    public void handleRead(ReadEvent event) {
        User receiver = userRepository.findById(event.getReceiverId()).orElse(null);
            
        if (receiver != null && receiver.getStudentCode() != null) {
            messagingTemplate.convertAndSendToUser(
                receiver.getStudentCode(),
                "/queue/read",
                event
            );
        }
    }
}