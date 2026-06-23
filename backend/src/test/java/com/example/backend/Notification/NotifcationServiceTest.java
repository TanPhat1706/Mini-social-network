package com.example.backend.Notification;

import com.example.backend.Enum.NotificationType;
import com.example.backend.Enum.ReactionType;
import com.example.backend.Event.NotificationEvent;
import com.example.backend.User.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper(); 

    @InjectMocks
    private NotificationService notificationService;

    private User sender;
    private User receiver;

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setId(1);
        sender.setFullName("Lê Hồng Phát");
        sender.setStudentCode("SV001");
        sender.setAvatarUrl("avatar1.jpg");
        sender.setCurrentAvatarFrame("frame_gold");
        sender.setCurrentNameColor("#FF0000");

        receiver = new User();
        receiver.setId(2);
        receiver.setFullName("User Hai");
        receiver.setStudentCode("SV002");
    }

    // ==========================================
    // 1. TEST LUỒNG EVENT CƠ BẢN
    // ==========================================

    @Test
    void handleNotificationEvent_whenSenderIsReceiver_shouldSkipAndNotSave() {
        // 🟢 ĐÃ FIX CONSTRUCTOR: Thêm null cho param ReactionType
        NotificationEvent event = new NotificationEvent(sender, sender, NotificationType.LIKE_POST, 10L, "POST", "", null, false);

        notificationService.handleNotificationEvent(event);

        verify(notificationRepository, never()).save(any(Notification.class));
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void handleNotificationEvent_whenReceiverStudentCodeIsNull_shouldSaveButNotSendSocket() {
        receiver.setStudentCode(null);
        NotificationEvent event = new NotificationEvent(sender, receiver, NotificationType.SHARE_POST, 20L, "POST", "", null, false);

        Notification savedNotification = Notification.builder()
                .id(99L).sender(sender).receiver(receiver).type(NotificationType.SHARE_POST).build();
        
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        notificationService.handleNotificationEvent(event);

        verify(notificationRepository).save(any(Notification.class));
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void handleNotificationEvent_whenValid_shouldSaveAndSendSocket() {
        NotificationEvent event = new NotificationEvent(
                sender, receiver, NotificationType.LIKE_POST, 30L, "POST", "", null, false);

        Notification savedNotification = Notification.builder()
                .id(100L)
                .sender(sender)       
                .receiver(receiver)   
                .type(NotificationType.LIKE_POST)
                .entityId(30L)
                .isAnonymous(false) 
                .isRead(false) 
                .build();
        
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        notificationService.handleNotificationEvent(event);

        verify(notificationRepository).save(any(Notification.class));
        
        ArgumentCaptor<NotificationDTO> dtoCaptor = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq("SV002"),
                eq("/queue/notifications"),
                dtoCaptor.capture()
        );

        NotificationDTO sentDto = dtoCaptor.getValue();
        assertEquals(100L, sentDto.getId());
        assertEquals("Lê Hồng Phát", sentDto.getSenderName());
        assertEquals("/posts/30", sentDto.getTargetUrl()); 
    }

    @Test
    void handleNotificationEvent_whenExceptionThrown_shouldCatchAndLog() {
        NotificationEvent event = new NotificationEvent(sender, receiver, NotificationType.LIKE_POST, 30L, "POST", "", null, false);
        
        when(notificationRepository.save(any(Notification.class))).thenThrow(new RuntimeException("DB Save Error"));
        
        assertDoesNotThrow(() -> notificationService.handleNotificationEvent(event));
    }

    // ==========================================
    // 2. TEST METADATA JSON (createMetadataJson)
    // ==========================================

    @Test
    void handleNotificationEvent_withLongComment_shouldTruncateMetadata() {
        String longMessage = "Đây là một bình luận rất rất rất rất rất rất rất rất rất dài vượt qua giới hạn năm mươi ký tự.";
        NotificationEvent event = new NotificationEvent(sender, receiver, NotificationType.COMMENT_POST, 40L, "POST", longMessage, null, false);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        when(notificationRepository.save(notifCaptor.capture())).thenReturn(new Notification());

        notificationService.handleNotificationEvent(event);

        Notification saved = notifCaptor.getValue();
        assertNotNull(saved.getMetadata());
        assertTrue(saved.getMetadata().contains("..."));
        assertTrue(saved.getMetadata().contains("commentSnippet"));
    }

    @Test
    void handleNotificationEvent_withShortComment_shouldNotTruncateMetadata() {
        String shortMessage = "Bình luận ngắn nè";
        NotificationEvent event = new NotificationEvent(sender, receiver, NotificationType.COMMENT_POST, 40L, "POST", shortMessage, null, false);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        when(notificationRepository.save(notifCaptor.capture())).thenReturn(new Notification());

        notificationService.handleNotificationEvent(event);

        Notification saved = notifCaptor.getValue();
        assertTrue(saved.getMetadata().contains("Bình luận ngắn nè"));
    }

    @Test
    void handleNotificationEvent_withFriendRequest_shouldAddPendingMetadata() {
        NotificationEvent event = new NotificationEvent(sender, receiver, NotificationType.FRIEND_REQUEST, 50L, "USER", "", null, false);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        when(notificationRepository.save(notifCaptor.capture())).thenReturn(new Notification());

        notificationService.handleNotificationEvent(event);

        Notification saved = notifCaptor.getValue();
        assertTrue(saved.getMetadata().contains("\"status\":\"PENDING\""));
    }

    // 🟢 MỚI: TEST ĐƯA REACTION TYPE VÀO METADATA
    @Test
    @DisplayName("Nếu có ReactionType -> Đưa vào Metadata JSON")
    void handleNotificationEvent_withReactionType_shouldIncludeInMetadata() {
        NotificationEvent event = new NotificationEvent(sender, receiver, NotificationType.LIKE_COMMENT, 30L, "COMMENT", "", ReactionType.LOVE, false);
        
        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        when(notificationRepository.save(notifCaptor.capture())).thenReturn(new Notification());

        notificationService.handleNotificationEvent(event);

        Notification saved = notifCaptor.getValue();
        assertTrue(saved.getMetadata().contains("\"reactionType\":\"LOVE\""));
    }

    @Test
    void handleNotificationEvent_whenJsonProcessingException_shouldReturnEmptyJson() throws Exception {
        NotificationEvent event = new NotificationEvent(sender, receiver, NotificationType.LIKE_POST, 30L, "POST", "", null, false);
        
        doThrow(new RuntimeException("Parse Error")).when(objectMapper).writeValueAsString(any());

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        when(notificationRepository.save(notifCaptor.capture())).thenReturn(new Notification());

        notificationService.handleNotificationEvent(event);

        Notification saved = notifCaptor.getValue();
        assertEquals("{}", saved.getMetadata());
    }

    // ==========================================
    // 3. TEST MAP TO DTO VÀ SWITCH-CASE (buildMessageContent / buildTargetUrl)
    // ==========================================

    @Test
    void mapToDTO_shouldMapCorrectlyAndBuildMessageAndUrl() {
        Notification notification = Notification.builder()
                .id(5L)
                .sender(sender)
                .receiver(receiver)
                .type(NotificationType.ACCEPT_FRIEND)
                .entityId(10L)
                .isRead(false)
                .isAnonymous(false)
                .build();

        NotificationDTO dto = notificationService.mapToDTO(notification);

        assertEquals(5L, dto.getId());
        assertEquals("Lê Hồng Phát", dto.getSenderName());
        assertEquals("đã chấp nhận lời mời kết bạn.", dto.getMessage());
        assertEquals("/", dto.getTargetUrl()); 
    }

    @Test
    void mapToDTO_whenIsAnonymous_shouldMaskSenderInfo() {
        Notification notification = Notification.builder()
                .id(99L)
                .sender(sender)
                .receiver(receiver)
                .type(NotificationType.COMMENT_POST)
                .entityId(20L)
                .isRead(false)
                .isAnonymous(true) // Cờ ẩn danh
                .build();

        NotificationDTO dto = notificationService.mapToDTO(notification);

        assertEquals(0L, dto.getSenderId()); 
        assertEquals("Một người dùng ẩn danh", dto.getSenderName()); 
        assertEquals("https://ui-avatars.com/api/?name=Anonymous&background=808080&color=fff", dto.getSenderAvatar()); 
        assertNull(dto.getSenderAvatarFrame()); 
        assertNull(dto.getSenderNameColor()); 
        
        assertEquals("đã bình luận về bài viết của bạn.", dto.getMessage());
    }

    // 🟢 MỚI: TEST LẤY REACTION TYPE TỪ METADATA
    @Test
    @DisplayName("mapToDTO: Đọc thành công reactionType từ metadata JSON")
    void mapToDTO_withReactionMetadata_shouldExtractReactionType() {
        Notification notification = Notification.builder()
                .id(1L).sender(sender).receiver(receiver).type(NotificationType.LIKE_COMMENT)
                .metadata("{\"reactionType\":\"HAHA\"}")
                .build();
        NotificationDTO dto = notificationService.mapToDTO(notification);
        assertEquals("HAHA", dto.getReactionType());
    }

    // 🟢 MỚI: TEST BẮT LỖI KHI METADATA BỊ HỎNG (Nhánh catch exception)
    @Test
    @DisplayName("mapToDTO: Log lỗi và bỏ qua nếu metadata JSON không hợp lệ")
    void mapToDTO_withInvalidMetadata_shouldCatchException() {
        Notification notification = Notification.builder()
                .id(1L).sender(sender).receiver(receiver).type(NotificationType.LIKE_COMMENT)
                .metadata("invalid_json_string")
                .build();
        
        assertDoesNotThrow(() -> {
            NotificationDTO dto = notificationService.mapToDTO(notification);
            assertNull(dto.getReactionType());
        });
    }

    @Test
    void mapToDTO_shouldCoverAllSwitchBranches() {
        // 1. LIKE_POST
        Notification nLike = buildMockNotification(NotificationType.LIKE_POST, 10L);
        NotificationDTO dtoLike = notificationService.mapToDTO(nLike);
        assertEquals("đã bày tỏ cảm xúc về bài viết của bạn.", dtoLike.getMessage());
        assertEquals("/posts/10", dtoLike.getTargetUrl());

        // 2. SHARE_POST
        Notification nShare = buildMockNotification(NotificationType.SHARE_POST, 20L);
        NotificationDTO dtoShare = notificationService.mapToDTO(nShare);
        assertEquals("đã chia sẻ bài viết của bạn.", dtoShare.getMessage());
        assertEquals("/", dtoShare.getTargetUrl());

        // 3. FRIEND_REQUEST
        Notification nFriend = buildMockNotification(NotificationType.FRIEND_REQUEST, 30L);
        NotificationDTO dtoFriend = notificationService.mapToDTO(nFriend);
        assertEquals("đã gửi cho bạn lời mời kết bạn.", dtoFriend.getMessage());
        assertEquals("/users/" + sender.getId(), dtoFriend.getTargetUrl());

        // 4. Default branch (VD: FOLLOW)
        Notification nDefault = buildMockNotification(NotificationType.FOLLOW, 40L);
        NotificationDTO dtoDefault = notificationService.mapToDTO(nDefault);
        assertEquals("đã tương tác với bạn.", dtoDefault.getMessage());
        assertEquals("/", dtoDefault.getTargetUrl());

        // 🟢 5. BỔ SUNG: LIKE_COMMENT
        Notification nLikeCmt = buildMockNotification(NotificationType.LIKE_COMMENT, 50L);
        NotificationDTO dtoLikeCmt = notificationService.mapToDTO(nLikeCmt);
        assertEquals("đã bày tỏ cảm xúc về bình luận của bạn", dtoLikeCmt.getMessage());
        assertEquals("/posts/50", dtoLikeCmt.getTargetUrl());

        // 🟢 6. BỔ SUNG: REPLY_COMMENT
        Notification nReply = buildMockNotification(NotificationType.REPLY_COMMENT, 60L);
        NotificationDTO dtoReply = notificationService.mapToDTO(nReply);
        assertEquals("đã phản hồi bình luận của bạn.", dtoReply.getMessage());
        assertEquals("/posts/60", dtoReply.getTargetUrl());
    }

    private Notification buildMockNotification(NotificationType type, Long entityId) {
        return Notification.builder()
                .id(99L)
                .sender(sender)
                .receiver(receiver)
                .type(type)
                .entityId(entityId)
                .isRead(false)
                .isAnonymous(false)
                .build();
    }

    // ==========================================
    // 4. TEST TÍNH NĂNG MARK ALL AS READ
    // ==========================================

    @Test
    void markAllAsRead_shouldCallRepository() {
        notificationService.markAllAsRead(2L);
        verify(notificationRepository).markAllAsReadByReceiverId(2L);
    }

    @Test
    void markAllAsRead_whenException_shouldCatchAndLog() {
        doThrow(new RuntimeException("DB Timeout")).when(notificationRepository).markAllAsReadByReceiverId(2L);
        assertDoesNotThrow(() -> notificationService.markAllAsRead(2L));
    }
}