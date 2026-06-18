package com.example.backend.Chat;

import com.example.backend.Integration.BaseControllerTest;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import com.example.backend.Enum.ReactionType;
import com.example.backend.Enum.MessageType;
import com.example.backend.Event.ReadEvent;
import com.example.backend.Event.TypingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

    @Test
    void processMessage_whenReceiverAndSenderNotFound_shouldStillSaveButNotBroadcast() {
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(mockMessage);
        when(userRepository.findById(2)).thenReturn(Optional.empty()); 
        when(userRepository.findById(1)).thenReturn(Optional.empty()); 

        chatController.processMessage(mockMessage);

        verify(chatMessageRepository).save(mockMessage);
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any(ChatMessage.class));
    }

    // ==========================================
    // 2. TEST HTTP REST: LỊCH SỬ CHAT VÀ INBOX
    // ==========================================
    @Test
    void getChatHistory_shouldReturnListOfMessages() throws Exception {
        when(chatMessageRepository.getHistoryWithRevokeFilter(1, 2, 1)).thenReturn(List.of(mockMessage));

        mockMvc.perform(get("/api/auth/messages/1/2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Alo, dạo này em khỏe không?"))
                .andExpect(jsonPath("$[0].senderId").value(1));
    }

    @Test
    @WithMockUser(username = "1412")
    void getRecentConversations_shouldReturnInboxList() throws Exception {
        mockConversation.setPartnerName("Nguoi Yeu Cu");
        mockConversation.setLastMessage("Alo, dao nay em khoe khong?");
        mockConversation.setRead(true);

        when(userRepository.findByEmail("1412")).thenReturn(Optional.empty());
        when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(currentUser));
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
        mockMvc.perform(get("/api/auth/messages/recent").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "1412")
    void getRecentConversations_whenPartnerNotFound_shouldSkipPartner() throws Exception {
        when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(currentUser));
        when(chatMessageRepository.findRecentMessages(1)).thenReturn(List.of(mockConversation));
        when(userRepository.findById(2)).thenReturn(Optional.empty()); // Đối tác bị xóa

        mockMvc.perform(get("/api/auth/messages/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ==========================================
    // 3. TEST MARK AS READ
    // ==========================================
    @Test
    @WithMockUser(username = "1412")
    void markAsRead_shouldReturn200() throws Exception {
        when(userRepository.findByEmail("1412")).thenReturn(Optional.empty());
        when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(currentUser));

        mockMvc.perform(put("/api/auth/messages/read/2").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(chatMessageRepository).markMessagesAsRead(2, 1);
    }

    // =========================================================================
    // 4. TEST TÍNH NĂNG THẢ CẢM XÚC (WEBSOCKET)
    // =========================================================================
    
    @Test
    void reactToMessage_whenMessageNotFound_shouldDoNothing() {
        ReactionRequest request = new ReactionRequest();
        request.setMessageId(999L);
        when(chatMessageRepository.findById(999L)).thenReturn(Optional.empty());

        chatController.reactToMessage(request);
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void reactToMessage_whenChangeReactionType_shouldUpdateAndNotSendSystemMessage() {
        // Đã thả LOVE trước đó
        ChatReaction existingReaction = ChatReaction.builder().chatMessage(mockMessage).userId(1).reactionType(ReactionType.LOVE).build();
        mockMessage.getReactions().add(existingReaction);

        // Nay đổi thành HAHA
        ReactionRequest request = new ReactionRequest();
        request.setMessageId(100L);
        request.setUserId(1); 
        request.setReactionType(ReactionType.HAHA);

        when(chatMessageRepository.findById(100L)).thenReturn(Optional.of(mockMessage));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(i -> i.getArgument(0));

        chatController.reactToMessage(request);

        assertEquals(1, mockMessage.getReactions().size());
        assertEquals(ReactionType.HAHA, mockMessage.getReactions().get(0).getReactionType());
        // Người tự thả cho chính mình -> Không sinh tin nhắn System
        verify(chatMessageRepository, times(1)).save(any());
    }

    @Test
    void reactToMessage_shouldRemoveReactionIfSameType() {
        ChatReaction existingReaction = ChatReaction.builder().chatMessage(mockMessage).userId(1).reactionType(ReactionType.LOVE).build();
        mockMessage.getReactions().add(existingReaction);

        ReactionRequest request = new ReactionRequest();
        request.setMessageId(100L);
        request.setUserId(1);
        request.setReactionType(ReactionType.LOVE);

        when(chatMessageRepository.findById(100L)).thenReturn(Optional.of(mockMessage));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(i -> i.getArgument(0));

        chatController.reactToMessage(request);

        assertTrue(mockMessage.getReactions().isEmpty());
    }

    @Test
    void reactToMessage_whenNewReactionFromPartner_shouldCoverAllEmojisAndSendSystemMessage() {
        // Quét toàn bộ Enum để phủ xanh 100% vòng switch-case "getEmojiForReaction"
        for (ReactionType type : ReactionType.values()) {
            mockMessage.setReactions(new ArrayList<>()); // reset
            
            ReactionRequest request = new ReactionRequest();
            request.setMessageId(100L);
            request.setUserId(2); // Đối tác (partnerUser) thả cảm xúc
            request.setReactionType(type);

            when(chatMessageRepository.findById(100L)).thenReturn(Optional.of(mockMessage));
            when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(i -> i.getArgument(0));
            when(userRepository.findById(1)).thenReturn(Optional.of(currentUser)); // Để sinh thông báo

            chatController.reactToMessage(request);
        }

        // Với mỗi loại cảm xúc, chatMessageRepository.save() sẽ được gọi 2 lần (1 cho Message, 1 cho System Message)
        verify(chatMessageRepository, times(ReactionType.values().length * 2)).save(any(ChatMessage.class));
    }

    @Test
    void reactToMessage_whenSenderAndReceiverNull_shouldSaveButNotBroadcast() {
        ReactionRequest request = new ReactionRequest();
        request.setMessageId(100L);
        request.setUserId(1);
        request.setReactionType(ReactionType.LIKE);

        when(chatMessageRepository.findById(100L)).thenReturn(Optional.of(mockMessage));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(anyInt())).thenReturn(Optional.empty()); // DB không tìm thấy ai

        chatController.reactToMessage(request);

        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any(ChatMessage.class));
    }

    // =========================================================================
    // 5. TEST TÍNH NĂNG THU HỒI (WEBSOCKET)
    // =========================================================================
    @Test
    void revokeMessage_whenMessageNotFound_shouldReturn() {
        RevokeRequest request = new RevokeRequest();
        request.setMessageId(999L);
        when(chatMessageRepository.findById(999L)).thenReturn(Optional.empty());

        chatController.revokeMessage(request);
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void revokeMessage_whenNotSenderAndNotReceiver_shouldReturn() {
        RevokeRequest request = new RevokeRequest();
        request.setMessageId(100L);
        request.setRequesterId(99); // Kẻ lạ mặt
        when(chatMessageRepository.findById(100L)).thenReturn(Optional.of(mockMessage));

        chatController.revokeMessage(request);
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void revokeMessage_everyone_shouldUpdateContentAndClearReactions() {
        mockMessage.getReactions().add(new ChatReaction());

        RevokeRequest request = new RevokeRequest();
        request.setMessageId(100L);
        request.setRequesterId(1); // Là người gửi
        request.setRevokeType("EVERYONE");

        when(chatMessageRepository.findById(100L)).thenReturn(Optional.of(mockMessage));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(1)).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(2)).thenReturn(Optional.of(partnerUser));

        chatController.revokeMessage(request);

        assertTrue(mockMessage.getIsDeletedEveryone());
        assertEquals("Tin nhắn đã thu hồi", mockMessage.getContent());
        assertTrue(mockMessage.getReactions().isEmpty()); 
        
        // Gọi save 2 lần: 1 lần cập nhật tin nhắn, 1 lần tạo tin nhắn Hệ thống "đã thu hồi"
        verify(chatMessageRepository, times(2)).save(any(ChatMessage.class));
    }

    @Test
    void revokeMessage_self_shouldSetDeletedBySenderId() {
        RevokeRequest request = new RevokeRequest();
        request.setMessageId(100L);
        request.setRequesterId(2); 
        request.setRevokeType("SELF");

        when(chatMessageRepository.findById(100L)).thenReturn(Optional.of(mockMessage));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(i -> i.getArgument(0));

        chatController.revokeMessage(request);

        assertFalse(mockMessage.getIsDeletedEveryone());
        assertEquals(Integer.valueOf(2), mockMessage.getDeletedBySenderId());
        verify(chatMessageRepository, times(1)).save(mockMessage);
    }

    // =========================================================================
    // 6. TEST TÍN HIỆU ĐANG GÕ VÀ ĐÃ XEM (WEBSOCKET)
    // =========================================================================
    
    @Test
    void handleTyping_whenReceiverExists_shouldBroadcast() {
        TypingEvent mockEvent = mock(TypingEvent.class);
        when(mockEvent.getReceiverId()).thenReturn(2);
        when(userRepository.findById(2)).thenReturn(Optional.of(partnerUser));

        chatController.handleTyping(mockEvent);

        verify(messagingTemplate).convertAndSendToUser("GUEST001", "/queue/typing", mockEvent);
    }

    @Test
    void handleTyping_whenReceiverNotFound_shouldNotBroadcast() {
        TypingEvent mockEvent = mock(TypingEvent.class);
        when(mockEvent.getReceiverId()).thenReturn(99);
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        chatController.handleTyping(mockEvent);

        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void handleRead_whenReceiverExists_shouldBroadcast() {
        ReadEvent mockEvent = mock(ReadEvent.class);
        when(mockEvent.getReceiverId()).thenReturn(2);
        when(userRepository.findById(2)).thenReturn(Optional.of(partnerUser));

        chatController.handleRead(mockEvent);

        verify(messagingTemplate).convertAndSendToUser("GUEST001", "/queue/read", mockEvent);
    }

    @Test
    void handleRead_whenReceiverNotFound_shouldNotBroadcast() {
        ReadEvent mockEvent = mock(ReadEvent.class);
        when(mockEvent.getReceiverId()).thenReturn(99);
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        chatController.handleRead(mockEvent);

        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }
}