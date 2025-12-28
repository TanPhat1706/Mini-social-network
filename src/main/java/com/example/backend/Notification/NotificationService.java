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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

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
                    .build();
            
            Notification savedNotification = notificationRepository.save(notification);
            NotificationDTO response = mapToDTO(savedNotification);
            
            String targetPrincipal = event.getReceiver().getStudentCode();
            
            log.info("=================================================");
            log.info(">>> [DEBUG-SOCKET] Preparing to send WebSocket Msg");
            
            if (targetPrincipal == null || targetPrincipal.isEmpty()) {
                log.error("!!! [CRITICAL ERROR] StudentCode is NULL or EMPTY for Receiver ID: {}", event.getReceiver().getId());
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
                    response
            );   
            
            log.info(">>> [SUCCESS] convertAndSendToUser executed.");
            log.info("=================================================");

        } catch (Exception e) {
            log.error("!!! [EXCEPTION] Error in handleNotificationEvent: ", e);
        }
    }

    private String createMetadataJson(NotificationEvent event) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            
            switch (event.getType()) {
                case COMMENT_POST:
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

    NotificationDTO mapToDTO(Notification notification) {
        String messageContent = buildMessageContent(notification);
        String targetUrl = buildTargetUrl(notification);

        return NotificationDTO.builder()
                .id(notification.getId())
                .senderId(Long.valueOf(notification.getSender().getId()))
                .senderName(notification.getSender().getFullName())
                .senderAvatar(notification.getSender().getAvatarUrl())
                .message(messageContent)
                .targetUrl(targetUrl)
                .createdAt(notification.getCreatedAt())
                .isRead(notification.isRead())
                .type(notification.getType())
                .build();
    }

    private String buildMessageContent(Notification n) {
        switch (n.getType()) {
            case LIKE_POST:
                return "đã thích bài viết của bạn.";
            case COMMENT_POST:
                return "đã bình luận về bài viết của bạn.";
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
            case COMMENT_POST:
                return "/posts/" + n.getEntityId();
            case FRIEND_REQUEST:
                return "/users/" + n.getSender().getId();
            default:
                return "/";
        }
    }
}
