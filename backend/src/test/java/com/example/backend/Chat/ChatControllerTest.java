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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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
    // ==========================================
    // 🟢 BỔ SUNG: CÁC NHÁNH CỦA PROCESS MESSAGE
    // ==========================================

    @Test
    void processMessage_whenReceiverAndSenderNotFound_shouldStillSaveButNotBroadcast() {
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(mockMessage);
        
        // 🟢 Cố tình trả về null để lấp 2 nhánh if (receiver != null) và if (sender != null)
        when(userRepository.findById(2)).thenReturn(Optional.empty()); 
        when(userRepository.findById(1)).thenReturn(Optional.empty()); 

        chatController.processMessage(mockMessage);

        verify(chatMessageRepository).save(mockMessage);
        // Đảm bảo MessagingTemplate không bao giờ được gọi
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any(ChatMessage.class));
    }

    // ==========================================
    // 🟢 BỔ SUNG: CÁC NHÁNH CỦA MARK AS READ
    // ==========================================

    @Test
    @WithMockUser(username = "test@example.com")
    void markAsRead_whenUserFoundByEmail_shouldSucceed() throws Exception {
        // 🟢 Lấp nhánh tìm bằng Email
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(currentUser));

        mockMvc.perform(put("/api/auth/messages/read/2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(chatMessageRepository).markMessagesAsRead(eq(2), eq(1));
    }

    @Test
    @WithMockUser(username = "ghost")
    void markAsRead_whenUserNotFound_shouldDoNothingButReturn200() throws Exception {
        // 🟢 Lấp nhánh không tìm thấy user
        when(userRepository.findByEmail("ghost")).thenReturn(Optional.empty());
        when(userRepository.findByStudentCode("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/auth/messages/read/2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Đảm bảo Repository không được gọi vì currentUser == null
        verify(chatMessageRepository, never()).markMessagesAsRead(anyInt(), anyInt());
    }

    // ==========================================
    // 🟢 BỔ SUNG: CÁC NHÁNH CỦA GET RECENT CONVERSATIONS
    // ==========================================

    @Test
    @WithMockUser(username = "test@example.com")
    void getRecentConversations_whenUserFoundByEmail_shouldReturnList() throws Exception {
        // 🟢 Lấp nhánh tìm bằng Email
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(currentUser));
        when(chatMessageRepository.findRecentMessages(1)).thenReturn(List.of(mockMessage));
        when(userRepository.findById(2)).thenReturn(Optional.of(partnerUser));

        mockMvc.perform(get("/api/auth/messages/recent"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "ghost")
    void getRecentConversations_whenUserNotFound_shouldReturn404() throws Exception {
        // 🟢 Lấp nhánh không tìm thấy user -> Trả về 404
        when(userRepository.findByEmail("ghost")).thenReturn(Optional.empty());
        when(userRepository.findByStudentCode("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/auth/messages/recent"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "1412")
    void getRecentConversations_whenPartnerNotFound_shouldSkipPartner() throws Exception {
        when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(currentUser));
        when(chatMessageRepository.findRecentMessages(1)).thenReturn(List.of(mockMessage));
        
        // 🟢 Lấp nhánh partner == null (Không add vào list conversations)
        when(userRepository.findById(2)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/auth/messages/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty()); // List trả về phải rỗng
    }

    @Test
    @WithMockUser(username = "1412")
    void getRecentConversations_testIsReadLogic() throws Exception {
        when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(currentUser));

        // Tạo 3 tin nhắn mô phỏng 3 nhánh logic của isRead
        // 1. Đối tác gửi cho mình, mình chưa đọc -> isRead = false
        ChatMessage msg1 = new ChatMessage();
        msg1.setSenderId(2); msg1.setReceiverId(1); msg1.setRead(false);

        // 2. Đối tác gửi cho mình, mình đã đọc -> isRead = true
        ChatMessage msg2 = new ChatMessage();
        msg2.setSenderId(3); msg2.setReceiverId(1); msg2.setRead(true);

        // 3. Mình gửi cho đối tác -> isRead = true (Luôn true ở phía mình hiển thị inbox)
        ChatMessage msg3 = new ChatMessage();
        msg3.setSenderId(1); msg3.setReceiverId(4); msg3.setRead(false);

        when(chatMessageRepository.findRecentMessages(1)).thenReturn(List.of(msg1, msg2, msg3));
        
        when(userRepository.findById(2)).thenReturn(Optional.of(buildUser(2, "User2")));
        when(userRepository.findById(3)).thenReturn(Optional.of(buildUser(3, "User3")));
        when(userRepository.findById(4)).thenReturn(Optional.of(buildUser(4, "User4")));

        mockMvc.perform(get("/api/auth/messages/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                // Kiểm tra nhánh 1
                .andExpect(jsonPath("$[0].isRead").value(false))
                // Kiểm tra nhánh 2
                .andExpect(jsonPath("$[1].isRead").value(true))
                // Kiểm tra nhánh 3
                .andExpect(jsonPath("$[2].isRead").value(true));
    }

    // Hàm tiện ích tạo User cho test
    private User buildUser(Integer id, String name) {
        User u = new User();
        u.setId(id);
        u.setFullName(name);
        return u;
    }
}