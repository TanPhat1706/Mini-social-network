package com.example.backend.Notification;

import com.example.backend.Enum.NotificationType;
import com.example.backend.Event.NotificationEvent;
import com.example.backend.User.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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

    // Sử dụng Spy để ObjectMapper chạy thật, giúp test metadata JSON chính xác
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

    @Test
    void handleNotificationEvent_whenSenderIsReceiver_shouldSkipAndNotSave() {
        // 🟢 ĐÃ SỬA: Thêm false vào cuối
        NotificationEvent event = new NotificationEvent(sender, sender, NotificationType.LIKE_POST, 10L, "POST", "", false);

        notificationService.handleNotificationEvent(event);

        verify(notificationRepository, never()).save(any(Notification.class));
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void handleNotificationEvent_whenReceiverStudentCodeIsNull_shouldSaveButNotSendSocket() {
        // Tái hiện case lưu DB thành công nhưng user nhận bị lỗi mất định danh (StudentCode = null)
        receiver.setStudentCode(null);
        // 🟢 ĐÃ SỬA: Thêm false vào cuối
        NotificationEvent event = new NotificationEvent(sender, receiver, NotificationType.SHARE_POST, 20L, "POST", "", false);

        Notification savedNotification = Notification.builder()
                .id(99L).sender(sender).receiver(receiver).type(NotificationType.SHARE_POST).build();
        
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        notificationService.handleNotificationEvent(event);

        // Đảm bảo vẫn lưu vào DB
        verify(notificationRepository).save(any(Notification.class));
        // Nhưng tuyệt đối không bắn socket
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void handleNotificationEvent_whenValid_shouldSaveAndSendSocket() {
        // Happy path thuần túy
        // 🟢 ĐÃ SỬA: Thêm false vào cuối
        NotificationEvent event = new NotificationEvent(sender, receiver, NotificationType.LIKE_POST, 30L, "POST", "", false);

        Notification savedNotification = Notification.builder()
                .id(100L).sender(sender).receiver(receiver).type(NotificationType.LIKE_POST).entityId(30L).build();
        
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        notificationService.handleNotificationEvent(event);

        verify(notificationRepository).save(any(Notification.class));
        
        // Kiểm tra xem message WebSocket có được gửi đúng địa chỉ không
        ArgumentCaptor<NotificationDTO> dtoCaptor = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq("SV002"),
                eq("/queue/notifications"),
                dtoCaptor.capture()
        );

        NotificationDTO sentDto = dtoCaptor.getValue();
        assertEquals(100L, sentDto.getId());
        assertEquals("Lê Hồng Phát", sentDto.getSenderName());
        assertEquals("/posts/30", sentDto.getTargetUrl()); // Test luôn logic buildUrl
    }

    @Test
    void handleNotificationEvent_withLongComment_shouldTruncateMetadata() {
        // Test logic cắt ngắn chuỗi bình luận > 50 ký tự
        String longMessage = "Đây là một bình luận rất rất rất rất rất rất rất rất rất dài vượt qua giới hạn năm mươi ký tự.";
        // 🟢 ĐÃ SỬA: Thêm false vào cuối
        NotificationEvent event = new NotificationEvent(sender, receiver, NotificationType.COMMENT_POST, 40L, "POST", longMessage, false);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        when(notificationRepository.save(notifCaptor.capture())).thenReturn(new Notification());

        notificationService.handleNotificationEvent(event);

        Notification saved = notifCaptor.getValue();
        assertNotNull(saved.getMetadata());
        assertTrue(saved.getMetadata().contains("..."));
        assertTrue(saved.getMetadata().contains("commentSnippet"));
        assertTrue(saved.getMetadata().length() < 100); // Json đã được rút gọn
    }

    @Test
    void handleNotificationEvent_withFriendRequest_shouldAddPendingMetadata() {
        // Test logic metadata cho kết bạn
        // 🟢 ĐÃ SỬA: Thêm false vào cuối
        NotificationEvent event = new NotificationEvent(sender, receiver, NotificationType.FRIEND_REQUEST, 50L, "USER", "", false);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        when(notificationRepository.save(notifCaptor.capture())).thenReturn(new Notification());

        notificationService.handleNotificationEvent(event);

        Notification saved = notifCaptor.getValue();
        assertTrue(saved.getMetadata().contains("\"status\":\"PENDING\""));
    }

    @Test
    void mapToDTO_shouldMapCorrectlyAndBuildMessageAndUrl() {
        // Tách riêng hàm mapToDTO ra test để đảm bảo các Enum dịch ra tiếng Việt chuẩn xác
        Notification notification = Notification.builder()
                .id(5L)
                .sender(sender)
                .receiver(receiver)
                .type(NotificationType.ACCEPT_FRIEND)
                .entityId(10L)
                .isRead(false)
                .isAnonymous(false) // Đảm bảo không ẩn danh
                .build();

        NotificationDTO dto = notificationService.mapToDTO(notification);

        assertEquals(5L, dto.getId());
        assertEquals(1L, dto.getSenderId());
        assertEquals("Lê Hồng Phát", dto.getSenderName());
        assertEquals("avatar1.jpg", dto.getSenderAvatar());
        assertEquals("frame_gold", dto.getSenderAvatarFrame());
        assertEquals("#FF0000", dto.getSenderNameColor());
        assertEquals("đã chấp nhận lời mời kết bạn.", dto.getMessage());
        assertEquals("/", dto.getTargetUrl()); // ACCEPT_FRIEND rơi vào nhánh default của URL
        assertFalse(dto.isRead());
    }

    // ========================================================
    // 🟢 MỚI: TEST CASE KIỂM CHỨNG TÍNH NĂNG "ĐEO MẶT NẠ"
    // ========================================================
    @Test
    void mapToDTO_whenIsAnonymous_shouldMaskSenderInfo() {
        Notification notification = Notification.builder()
                .id(99L)
                .sender(sender) // Mặc dù truyền sender là Phát
                .receiver(receiver)
                .type(NotificationType.COMMENT_POST)
                .entityId(20L)
                .isRead(false)
                .isAnonymous(true) // 🟢 BẬT CỜ ẨN DANH LÊN
                .build();

        NotificationDTO dto = notificationService.mapToDTO(notification);

        assertEquals(99L, dto.getId());
        
        // KIỂM TRA XEM MẶT NẠ ĐÃ ĐƯỢC ĐEO CHUẨN CHƯA
        assertEquals(0L, dto.getSenderId()); // ID bị xóa dấu vết
        assertEquals("Một người dùng ẩn danh", dto.getSenderName()); // Đổi tên
        assertEquals("https://ui-avatars.com/api/?name=Anonymous&background=808080&color=fff", dto.getSenderAvatar()); // Avatar xám
        assertNull(dto.getSenderAvatarFrame()); // Khung bị ẩn
        assertNull(dto.getSenderNameColor()); // Màu tên bị ẩn
        
        assertEquals("đã bình luận về bài viết của bạn.", dto.getMessage());
    }
}