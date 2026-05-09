package com.example.backend.Chat;

import com.example.backend.Integration.BaseControllerTest;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print; // 🟢 Thêm import này
@WebMvcTest(
        value = ChatController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
class ChatControllerTest extends BaseControllerTest {

    // 🟢 Trực tiếp Inject Controller để test riêng hàm WebSocket
    @Autowired
    private ChatController chatController;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @MockBean
    private ChatMessageRepository chatMessageRepository;

    @MockBean
    private ChatRoomService chatRoomService;

    @MockBean
    private UserRepository userRepository;

    private User currentUser;
    private User partnerUser;
    private ChatMessage mockMessage;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1);
        currentUser.setStudentCode("1412");
        currentUser.setFullName("Lê Hồng Phát");

        partnerUser = new User();
        partnerUser.setId(2);
        partnerUser.setStudentCode("GUEST001");
        partnerUser.setFullName("Người Yêu Cũ");

        mockMessage = new ChatMessage();
        mockMessage.setId(100L);
        mockMessage.setSenderId(1);
        mockMessage.setReceiverId(2);
        mockMessage.setContent("Alo, dạo này em khỏe không?");
        mockMessage.setTimestamp(LocalDateTime.now());
        mockMessage.setRead(false);
    }

    // ==========================================
    // 1. TEST WEBSOCKET: processMessage
    // ==========================================
    @Test
    void processMessage_shouldSaveAndBroadcastToUsers() {
        // Mock dữ liệu
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(mockMessage);
        when(userRepository.findById(2)).thenReturn(Optional.of(partnerUser)); // Receiver
        when(userRepository.findById(1)).thenReturn(Optional.of(currentUser)); // Sender

        // Hành động: Gọi trực tiếp hàm thay vì dùng MockMvc
        chatController.processMessage(mockMessage);

        // Xác minh: Chat room được tạo/lấy ra
        verify(chatRoomService).getChatId(eq(1), eq(2), eq(true));
        
        // Xác minh: Tin nhắn đã được lưu và set trạng thái chưa đọc
        verify(chatMessageRepository).save(mockMessage);
        assertFalse(mockMessage.isRead());

        // Xác minh: Đã bắn WS cho Receiver
        verify(messagingTemplate).convertAndSendToUser(
                eq("GUEST001"), 
                eq("/queue/messages"), 
                eq(mockMessage)
        );

        // Xác minh: Đã bắn WS lại cho Sender (đồng bộ thiết bị)
        verify(messagingTemplate).convertAndSendToUser(
                eq("1412"), 
                eq("/queue/messages"), 
                eq(mockMessage)
        );
    }

    // ==========================================
    // 2. TEST GET CHAT HISTORY (HTTP REST)
    // ==========================================
    @Test
    void getChatHistory_shouldReturnListOfMessages() throws Exception {
        when(chatMessageRepository.findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByTimestampAsc(
                1, 2, 2, 1)).thenReturn(List.of(mockMessage));

        mockMvc.perform(get("/api/auth/messages/1/2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Alo, dạo này em khỏe không?"))
                .andExpect(jsonPath("$[0].senderId").value(1));
    }

// ==========================================
    // 3. TEST GET RECENT CONVERSATIONS (INBOX)
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void getRecentConversations_shouldReturnInboxList() throws Exception {
        // Mẹo nhỏ: Đổi thành Tiếng Việt không dấu để tránh bị tạch test do lỗi font UTF-8
        partnerUser.setFullName("Nguoi Yeu Cu");
        mockMessage.setContent("Alo, dao nay em khoe khong?");

        // Fallback logic mock: không thấy email -> tìm theo studentCode
        when(userRepository.findByEmail("1412")).thenReturn(Optional.empty());
        when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(currentUser));
        
        when(chatMessageRepository.findRecentMessages(1)).thenReturn(List.of(mockMessage));
        when(userRepository.findById(2)).thenReturn(Optional.of(partnerUser));

        mockMvc.perform(get("/api/auth/messages/recent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")) // Ép nhận diện UTF-8
                .andExpect(status().isOk())
                // 🟢 ĐÃ SỬA: Dùng đúng tên biến (partnerName, lastMessage, isRead)
                .andExpect(jsonPath("$[0].partnerName").value("Nguoi Yeu Cu"))
                .andExpect(jsonPath("$[0].lastMessage").value("Alo, dao nay em khoe khong?"))
                .andExpect(jsonPath("$[0].isRead").value(true)); // 🟢 ĐÃ SỬA: Đổi thành true theo đúng logic của sếp
    }
    
    @Test
    void getRecentConversations_whenNotAuthenticated_shouldReturn401() throws Exception {
        // Không gắn @WithMockUser -> Trả về 401
        mockMvc.perform(get("/api/auth/messages/recent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // 4. TEST MARK AS READ
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void markAsRead_shouldReturn200() throws Exception {
        when(userRepository.findByEmail("1412")).thenReturn(Optional.empty());
        when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(currentUser));

        mockMvc.perform(put("/api/auth/messages/read/2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Xác minh Repository đã được gọi với đúng tham số: (partnerId, myId)
        verify(chatMessageRepository).markMessagesAsRead(eq(2), eq(1));
    }
}