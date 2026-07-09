package com.example.backend.Notification;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.example.backend.Event.NotificationEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import com.example.backend.User.User;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @EventListener
    @Async
    public void handleNotificationEvent(NotificationEvent event) {
        try {
            log.info(">>> [START] Handling event from Sender ID: {} to Receiver ID: {}",
                    event.getSender().getId(), event.getReceiver().getId());

            if (event.getSender().getId().equals(event.getReceiver().getId())) {
                log.warn(">>> [SKIP] Sender and Receiver are the same person. Skipping.");
                return;
            }

            Notification notification = Notification.builder()
                    .sender(event.getSender())
                    .receiver(event.getReceiver())
                    .type(event.getType())
                    .entityId(event.getEntityId())
                    .entityType(event.getEntityType())
                    .metadata(createMetadataJson(event))
                    .isRead(false)
                    .isAnonymous(event.isAnonymous())
                    .build();

            Notification savedNotification = notificationRepository.save(notification);
            NotificationDTO response = mapToDTO(savedNotification);

            String targetPrincipal = event.getReceiver().getStudentCode();

            log.info("=================================================");
            log.info(">>> [DEBUG-SOCKET] Preparing to send WebSocket Msg");

            if (targetPrincipal == null || targetPrincipal.isEmpty()) {
                log.error("!!! [CRITICAL ERROR] StudentCode is NULL or EMPTY for Receiver ID: {}",
                        event.getReceiver().getId());
                log.error("!!! Socket message will NOT be sent because user is undefined.");
                return;
            }

            log.info(">>> Target Principal (User): '{}'", targetPrincipal);
            log.info(">>> Destination: /user/{}/queue/notifications", targetPrincipal);

            try {
                log.info(">>> Payload: {}", objectMapper.writeValueAsString(response));
            } catch (Exception e) {
                log.info(">>> Payload (Object): {}", response);
            }

            messagingTemplate.convertAndSendToUser(
                    targetPrincipal,
                    "/queue/notifications",
                    response);

            log.info(">>> [SUCCESS] convertAndSendToUser executed.");
            log.info("=================================================");

        } catch (Exception e) {
            log.error("!!! [EXCEPTION] Error in handleNotificationEvent: ", e);
        }
    }

    private String createMetadataJson(NotificationEvent event) {
        try {
            Map<String, Object> metadata = new HashMap<>();

            if (event.getReactionType() != null) {
                metadata.put("reactionType", event.getReactionType().name());
            }

            switch (event.getType()) {
                case COMMENT_POST:
                case REPLY_COMMENT:
                    String snippet = event.getMessage().length() > 50
                            ? event.getMessage().substring(0, 50) + "..."
                            : event.getMessage();
                    metadata.put("commentSnippet", snippet);
                    break;
                case FRIEND_REQUEST:
                    metadata.put("status", "PENDING");
                    break;
                default:
                    break;
            }
            return objectMapper.writeValueAsString(metadata);

        } catch (Exception e) {
            System.out.println("Error creating metadata JSON: " + e.getMessage());
            return "{}";
        }
    }

    // 2. CẬP NHẬT HÀM MAP TO DTO (Ghi đè thông tin người gửi nếu là ẩn danh)
    NotificationDTO mapToDTO(Notification notification) {
        String messageContent = buildMessageContent(notification);
        String targetUrl = buildTargetUrl(notification);
        User sender = notification.getSender();

        // 🟢 CHỐT CHẶN BẢO MẬT: Bọc Boolean.TRUE.equals() để chống lỗi
        // NullPointerException
        boolean isAnon = Boolean.TRUE.equals(notification.getIsAnonymous());

        // 🟢 DÙNG BIẾN isAnon ĐÃ ĐƯỢC XỬ LÝ ĐỂ GÁN GIÁ TRỊ
        Long senderId = isAnon ? 0L : Long.valueOf(sender.getId());
        String senderName = isAnon ? "Một người dùng ẩn danh" : sender.getFullName();
        String senderAvatar = isAnon ? "https://ui-avatars.com/api/?name=Anonymous&background=808080&color=fff"
                : sender.getAvatarUrl();
        String avatarFrame = isAnon ? null : sender.getCurrentAvatarFrame();
        String nameColor = isAnon ? null : sender.getCurrentNameColor();

        String reactionType = null;
        try {
            if (notification.getMetadata() != null && !notification.getMetadata().isBlank()) {
                JsonNode metadataNode = objectMapper.readTree(notification.getMetadata());
                if (metadataNode.has("reactionType")) {
                    reactionType = metadataNode.get("reactionType").asText(null);
                }
            }
        } catch (Exception e) {
            log.warn("Cannot parse notification.metadata for reactionType", e);
        }

        return NotificationDTO.builder()
                .id(notification.getId())
                .senderId(senderId)
                .senderName(senderName)
                .senderAvatar(senderAvatar)
                .senderAvatarFrame(avatarFrame)
                .senderNameColor(nameColor)
                .message(messageContent)
                .targetUrl(targetUrl)
                .createdAt(notification.getCreatedAt())
                .isRead(notification.getIsRead() != null ? notification.getIsRead() : false) // 🟢 Bảo vệ luôn isRead
                                                                                             // cho chắc chắn
                .type(notification.getType())
                .reactionType(reactionType)
                .build();
    }

    private String buildMessageContent(Notification n) {
        switch (n.getType()) {
            case LIKE_POST:
                return "đã bày tỏ cảm xúc về bài viết của bạn.";
            case LIKE_COMMENT:
                return "đã bày tỏ cảm xúc về bình luận của bạn";
            case COMMENT_POST:
                return "đã bình luận về bài viết của bạn.";
            case REPLY_COMMENT:
                return "đã phản hồi bình luận của bạn.";
            case SHARE_POST:
                return "đã chia sẻ bài viết của bạn.";
            case FRIEND_REQUEST:
                return "đã gửi cho bạn lời mời kết bạn.";
            case ACCEPT_FRIEND:
                return "đã chấp nhận lời mời kết bạn.";
            default:
                return "đã tương tác với bạn.";
        }
    }

    private String buildTargetUrl(Notification n) {
        switch (n.getType()) {
            case LIKE_POST:
                return "/posts/" + n.getEntityId();
            case LIKE_COMMENT:
                return "/posts/" + n.getEntityId();
            case COMMENT_POST:
                return "/posts/" + n.getEntityId();
            case REPLY_COMMENT:
                return "/posts/" + n.getEntityId();
            case FRIEND_REQUEST:
                return "/users/" + n.getSender().getId();
            default:
                return "/";
        }
    }

    public void markAllAsRead(Long receiverId) {
        try {
            notificationRepository.markAllAsReadByReceiverId(receiverId);
            log.info(">>> [DB SYNC] Marked all notifications as read for User ID: {}", receiverId);
        } catch (Exception e) {
            log.error(">>> [DB ERROR] Failed to mark notifications as read for User ID: {}", receiverId, e);
        }
    }
}
