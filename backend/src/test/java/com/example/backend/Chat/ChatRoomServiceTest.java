package com.example.backend.Chat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @InjectMocks
    private ChatRoomService chatRoomService;

    // ==========================================
    // TEST CASE 1: PHÒNG CHAT ĐÃ TỒN TẠI
    // ==========================================
    @Test
    void getChatId_whenRoomExists_shouldReturnExistingChatId() {
        ChatRoom existingRoom = ChatRoom.builder()
                .chatId("1_2")
                .senderId(1)
                .recipientId(2)
                .build();

        // Giả lập Repository tìm thấy phòng chat
        when(chatRoomRepository.findBySenderIdAndRecipientId(1, 2))
                .thenReturn(Optional.of(existingRoom));

        // Gọi hàm với cờ true hay false đều không quan trọng vì nó sẽ rẽ nhánh luôn từ đầu
        Optional<String> result = chatRoomService.getChatId(1, 2, true);

        assertTrue(result.isPresent());
        assertEquals("1_2", result.get());
        
        // Đảm bảo không có lệnh save() nào bị gọi thừa thãi
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    // ==========================================
    // TEST CASE 2: PHÒNG CHƯA TỒN TẠI & CỜ CREATE LÀ FALSE
    // ==========================================
    @Test
    void getChatId_whenRoomDoesNotExist_andCreateIsFalse_shouldReturnEmpty() {
        // Giả lập Repository không tìm thấy phòng
        when(chatRoomRepository.findBySenderIdAndRecipientId(1, 2))
                .thenReturn(Optional.empty());

        // Gọi hàm với cờ createNewRoomIfNotExists = false (Ví dụ: Chỉ kiểm tra xem có phòng chưa chứ không muốn tạo)
        Optional<String> result = chatRoomService.getChatId(1, 2, false);

        assertFalse(result.isPresent()); // Phải trả về Optional rỗng
        
        // Đảm bảo tuyệt đối không có phòng nào được lưu xuống DB
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    // ==========================================
    // TEST CASE 3: PHÒNG CHƯA TỒN TẠI & CỜ CREATE LÀ TRUE (LUỒNG LÕI)
    // ==========================================
    @Test
    void getChatId_whenRoomDoesNotExist_andCreateIsTrue_shouldCreateAndReturnNewChatId() {
        // Giả lập Repository không tìm thấy phòng
        when(chatRoomRepository.findBySenderIdAndRecipientId(1, 2))
                .thenReturn(Optional.empty());

        // Thực thi hàm với cờ tạo mới
        Optional<String> result = chatRoomService.getChatId(1, 2, true);

        // Kiểm chứng kết quả trả về
        assertTrue(result.isPresent());
        assertEquals("1_2", result.get()); // Theo đúng logic String.format("%s_%s")

        // Bắt trọn 2 lệnh save xuống Database
        ArgumentCaptor<ChatRoom> captor = ArgumentCaptor.forClass(ChatRoom.class);
        verify(chatRoomRepository, times(2)).save(captor.capture());

        // Lấy danh sách 2 bản ghi đã được lưu
        List<ChatRoom> savedRooms = captor.getAllValues();
        assertEquals(2, savedRooms.size());

        // Bản ghi 1: Sender -> Recipient
        ChatRoom senderRecipient = savedRooms.get(0);
        assertEquals("1_2", senderRecipient.getChatId());
        assertEquals(1, senderRecipient.getSenderId());
        assertEquals(2, senderRecipient.getRecipientId());

        // Bản ghi 2: Recipient -> Sender (Logic lưu ngược để truy vấn 2 chiều)
        ChatRoom recipientSender = savedRooms.get(1);
        assertEquals("1_2", recipientSender.getChatId());
        assertEquals(2, recipientSender.getSenderId());
        assertEquals(1, recipientSender.getRecipientId());
    }
}