package com.example.backend.Chat;

import com.example.backend.Integration.BaseControllerTest;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import com.example.backend.Enum.ReactionType;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// 🟢 FIX: Import đầy đủ các hàm Assertions của JUnit
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = ChatController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
class ChatControllerTest extends BaseControllerTest {

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
    // 🟢 MỚI: Biến giả lập để trả về cho API Recent Messages
    private ChatConversationDTO mockConversation; 

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
        mockMessage.setReactions(new ArrayList<>());

        // 🟢 MỚI: Khởi tạo biến giả lập ChatConversationDTO (Có 9 tham số theo cấu trúc mới nhất)
        mockConversation = new ChatConversationDTO(
                2,
                "GUEST001",
                "Người Yêu Cũ",
                "http://avatar.com/ex.jpg",
                "Alo, dạo này em khỏe không?",
                LocalDateTime.now(),
                false,
                "gold-frame",
                "vip-color"
        );
    }

    // ==========================================
    // 1. TEST WEBSOCKET: processMessage
    // ==========================================
    @Test
    void processMessage_shouldSaveAndBroadcastToUsers() {
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(mockMessage);
        when(userRepository.findById(2)).thenReturn(Optional.of(partnerUser));
        when(userRepository.findById(1)).thenReturn(Optional.of(currentUser));

        chatController.processMessage(mockMessage);

        verify(chatRoomService).getChatId(1, 2, true);
        verify(chatMessageRepository).save(mockMessage);
        assertFalse(mockMessage.isRead());

        verify(messagingTemplate).convertAndSendToUser("GUEST001", "/queue/messages", mockMessage);
        verify(messagingTemplate).convertAndSendToUser("1412", "/queue/messages", mockMessage);
    }

    // ==========================================
    // 2. TEST GET CHAT HISTORY (HTTP REST)
    // ==========================================
    @Test
    void getChatHistory_shouldReturnListOfMessages() throws Exception {
        when(chatMessageRepository.getHistoryWithRevokeFilter(1, 2, 1))
                .thenReturn(List.of(mockMessage));

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
        // Cập nhật lại giả lập DTO cho khớp nội dung Test
        mockConversation.setPartnerName("Nguoi Yeu Cu");
        mockConversation.setLastMessage("Alo, dao nay em khoe khong?");
        mockConversation.setRead(true);

        when(userRepository.findByEmail("1412")).thenReturn(Optional.empty());
        when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(currentUser));
        
        // 🟢 FIX: Truyền List.of(mockConversation) thay vì mockMessage
        when(chatMessageRepository.findRecentMessages(1)).thenReturn(List.of(mockConversation));
        when(userRepository.findById(2)).thenReturn(Optional.of(partnerUser));

        mockMvc.perform(get("/api/auth/messages/recent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].partnerName").value("Nguoi Yeu Cu"))
                .andExpect(jsonPath("$[0].lastMessage").value("Alo, dao nay em khoe khong?"))
                .andExpect(jsonPath("$[0].isRead").value(true));
    }
    
    @Test
    void getRecentConversations_whenNotAuthenticated_shouldReturn401() throws Exception {
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

        verify(chatMessageRepository).markMessagesAsRead(2, 1);
    }

    @Test
    void processMessage_whenReceiverAndSenderNotFound_shouldStillSaveButNotBroadcast() {
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(mockMessage);
        
        when(userRepository.findById(2)).thenReturn(Optional.empty()); 
        when(userRepository.findById(1)).thenReturn(Optional.empty()); 

        chatController.processMessage(mockMessage);

        verify(chatMessageRepository).save(mockMessage);
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any(ChatMessage.class));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void markAsRead_whenUserFoundByEmail_shouldSucceed() throws Exception {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(currentUser));

        mockMvc.perform(put("/api/auth/messages/read/2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(chatMessageRepository).markMessagesAsRead(2, 1);
    }

    @Test
    @WithMockUser(username = "ghost")
    void markAsRead_whenUserNotFound_shouldDoNothingButReturn200() throws Exception {
        when(userRepository.findByEmail("ghost")).thenReturn(Optional.empty());
        when(userRepository.findByStudentCode("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/auth/messages/read/2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(chatMessageRepository, never()).markMessagesAsRead(anyInt(), anyInt());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getRecentConversations_whenUserFoundByEmail_shouldReturnList() throws Exception {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(currentUser));
        // 🟢 FIX: Truyền List.of(mockConversation) thay vì mockMessage
        when(chatMessageRepository.findRecentMessages(1)).thenReturn(List.of(mockConversation));
        when(userRepository.findById(2)).thenReturn(Optional.of(partnerUser));

        mockMvc.perform(get("/api/auth/messages/recent"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "ghost")
    void getRecentConversations_whenUserNotFound_shouldReturn404() throws Exception {
        when(userRepository.findByEmail("ghost")).thenReturn(Optional.empty());
        when(userRepository.findByStudentCode("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/auth/messages/recent"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "1412")
    void getRecentConversations_whenPartnerNotFound_shouldSkipPartner() throws Exception {
        when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(currentUser));
        // 🟢 FIX: Truyền List.of(mockConversation)
        when(chatMessageRepository.findRecentMessages(1)).thenReturn(List.of(mockConversation));
        // Cố tình cho user 2 (partner) bằng empty
        when(userRepository.findById(2)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/auth/messages/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(username = "1412")
    void getRecentConversations_testIsReadLogic() throws Exception {
        when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(currentUser));

        // 🟢 Cập nhật danh sách Mock thành kiểu DTO
        ChatConversationDTO conv1 = new ChatConversationDTO(2, "ST002", "User2", "", "Msg", LocalDateTime.now(), false, "", "");
        ChatConversationDTO conv2 = new ChatConversationDTO(3, "ST003", "User3", "", "Msg", LocalDateTime.now(), true, "", "");
        ChatConversationDTO conv3 = new ChatConversationDTO(4, "ST004", "User4", "", "Msg", LocalDateTime.now(), true, "", "");

        when(chatMessageRepository.findRecentMessages(1)).thenReturn(List.of(conv1, conv2, conv3));
        
        when(userRepository.findById(2)).thenReturn(Optional.of(buildUser(2, "User2")));
        when(userRepository.findById(3)).thenReturn(Optional.of(buildUser(3, "User3")));
        when(userRepository.findById(4)).thenReturn(Optional.of(buildUser(4, "User4")));

        mockMvc.perform(get("/api/auth/messages/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].isRead").value(false))
                .andExpect(jsonPath("$[1].isRead").value(true))
                .andExpect(jsonPath("$[2].isRead").value(true));
    }

    private User buildUser(Integer id, String name) {
        User u = new User();
        u.setId(id);
        u.setFullName(name);
        return u;
    }

    // =========================================================================
    // 🟢 KỊCH BẢN THỬ NGHIỆM: TÍNH NĂNG THẢ CẢM XÚC (@MessageMapping)
    // =========================================================================

    @Test
    void reactToMessage_shouldAddReactionAndBroadcast() {
        ReactionRequest request = new ReactionRequest();
        request.setMessageId(100L);
        request.setUserId(1); 
        request.setReactionType(ReactionType.LOVE);

        when(chatMessageRepository.findById(100L)).thenReturn(Optional.of(mockMessage));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(1)).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(2)).thenReturn(Optional.of(partnerUser));

        chatController.reactToMessage(request);

        assertEquals(1, mockMessage.getReactions().size());
        assertEquals(ReactionType.LOVE, mockMessage.getReactions().get(0).getReactionType());
        verify(chatMessageRepository, times(1)).save(mockMessage);

        verify(messagingTemplate).convertAndSendToUser("GUEST001", "/queue/messages", mockMessage);
        verify(messagingTemplate).convertAndSendToUser("1412", "/queue/messages", mockMessage);
    }

    @Test
    void reactToMessage_shouldRemoveReactionIfSameType() {
        ChatReaction existingReaction = ChatReaction.builder()
                .chatMessage(mockMessage).userId(1).reactionType(ReactionType.LOVE).build();
        mockMessage.getReactions().add(existingReaction);

        ReactionRequest request = new ReactionRequest();
        request.setMessageId(100L);
        request.setUserId(1);
        request.setReactionType(ReactionType.LOVE);

        when(chatMessageRepository.findById(100L)).thenReturn(Optional.of(mockMessage));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(1)).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(2)).thenReturn(Optional.of(partnerUser));

        chatController.reactToMessage(request);

        assertTrue(mockMessage.getReactions().isEmpty());
        verify(chatMessageRepository, times(1)).save(mockMessage);
    }

    // =========================================================================
    // 🟢 KỊCH BẢN THỬ NGHIỆM: TÍNH NĂNG THU HỒI (@MessageMapping)
    // =========================================================================
    @Test
    void revokeMessage_everyone_shouldUpdateContentAndClearReactions() {
        mockMessage.getReactions().add(new ChatReaction());

        RevokeRequest request = new RevokeRequest();
        request.setMessageId(100L);
        request.setRequesterId(1);
        request.setRevokeType("EVERYONE");

        when(chatMessageRepository.findById(100L)).thenReturn(Optional.of(mockMessage));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(1)).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(2)).thenReturn(Optional.of(partnerUser));

        chatController.revokeMessage(request);

        assertTrue(mockMessage.getIsDeletedEveryone());
        assertEquals("Tin nhắn đã thu hồi", mockMessage.getContent());
        assertTrue(mockMessage.getReactions().isEmpty()); 
        verify(chatMessageRepository, times(2)).save(any(ChatMessage.class));
    }

    @Test
    void revokeMessage_everyone_shouldFailIfNotSender() {
        RevokeRequest request = new RevokeRequest();
        request.setMessageId(100L);
        request.setRequesterId(2); 
        request.setRevokeType("EVERYONE");

        when(chatMessageRepository.findById(100L)).thenReturn(Optional.of(mockMessage));

        chatController.revokeMessage(request);

        assertFalse(mockMessage.getIsDeletedEveryone());
        assertNotEquals("Tin nhắn đã thu hồi", mockMessage.getContent());

        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any(ChatMessage.class));
    }

    @Test
    void revokeMessage_self_shouldSetDeletedBySenderId() {
        RevokeRequest request = new RevokeRequest();
        request.setMessageId(100L);
        request.setRequesterId(2); 
        request.setRevokeType("SELF");

        when(chatMessageRepository.findById(100L)).thenReturn(Optional.of(mockMessage));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(1)).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(2)).thenReturn(Optional.of(partnerUser));

        chatController.revokeMessage(request);

        assertFalse(mockMessage.getIsDeletedEveryone());
        assertEquals(Integer.valueOf(2), mockMessage.getDeletedBySenderId());
        verify(chatMessageRepository, times(1)).save(mockMessage);
    }
}