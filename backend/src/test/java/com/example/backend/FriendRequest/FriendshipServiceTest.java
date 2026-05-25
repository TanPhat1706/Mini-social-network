package com.example.backend.FriendRequest;

import com.example.backend.Enum.NotificationType;
import com.example.backend.Event.NotificationEvent;
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceTest {

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private FriendshipService friendshipService;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setId(1);
        user1.setFullName("User Một");

        user2 = new User();
        user2.setId(2);
        user2.setFullName("User Hai");
    }

    // ==========================================
    // TEST: GỬI LỜI MỜI KẾT BẠN (sendRequest)
    // ==========================================

    @Test
    void sendRequest_whenSenderEqualsReceiver_shouldThrow() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> friendshipService.sendRequest(1, 1));
        assertEquals("Không thể kết bạn với chính mình", ex.getMessage());
    }

    @Test
    void sendRequest_whenUserNotFound_shouldThrow() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user1));
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> friendshipService.sendRequest(1, 99));
        assertEquals("Receiver not found", ex.getMessage());
    }

    @Test
    void sendRequest_whenAlreadyPendingOrAccepted_shouldThrow() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2)).thenReturn(Optional.of(user2));

        Friendship existing = new Friendship();
        existing.setStatus("ACCEPTED");
        when(friendshipRepository.findFriendship(1, 2)).thenReturn(Optional.of(existing));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> friendshipService.sendRequest(1, 2));
        assertEquals("Đã tồn tại mối quan hệ", ex.getMessage());
    }

    @Test
    void sendRequest_whenStatusDeleted_shouldRestoreToPending_andPublishEvent() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2)).thenReturn(Optional.of(user2));

        Friendship existing = new Friendship();
        existing.setStatus("DELETED");
        when(friendshipRepository.findFriendship(1, 2)).thenReturn(Optional.of(existing));

        String result = friendshipService.sendRequest(1, 2);

        assertEquals("Đã gửi lại lời mời", result);
        assertEquals("PENDING", existing.getStatus());
        assertEquals(1, existing.getActionUserId());
        verify(friendshipRepository).save(existing);
        
        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals("đã gửi lại lời mời kết bạn.", eventCaptor.getValue().getMessage());
    }

    @Test
    void sendRequest_whenNoExisting_shouldCreateNew_andPublishEvent() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2)).thenReturn(Optional.of(user2));
        when(friendshipRepository.findFriendship(1, 2)).thenReturn(Optional.empty());

        String result = friendshipService.sendRequest(1, 2);

        assertEquals("Đã gửi lời mời", result);

        ArgumentCaptor<Friendship> fsCaptor = ArgumentCaptor.forClass(Friendship.class);
        verify(friendshipRepository).save(fsCaptor.capture());
        assertEquals("PENDING", fsCaptor.getValue().getStatus());
        assertEquals(1, fsCaptor.getValue().getActionUserId());

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(NotificationType.FRIEND_REQUEST, eventCaptor.getValue().getType());
    }

    // ==========================================
    // TEST: CHẤP NHẬN LỜI MỜI (acceptRequest)
    // ==========================================

    @Test
    void acceptRequest_whenNotFound_shouldThrow() {
        when(friendshipRepository.findFriendship(1, 2)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> friendshipService.acceptRequest(1, 2));
        assertEquals("Không tìm thấy lời mời", ex.getMessage());
    }

    @Test
    void acceptRequest_whenActionUserIsSelf_shouldThrow() {
        Friendship f = new Friendship();
        f.setActionUserId(1); // Chính user 1 là người gửi nhưng lại đi bấm chấp nhận
        when(friendshipRepository.findFriendship(1, 2)).thenReturn(Optional.of(f));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> friendshipService.acceptRequest(1, 2));
        assertEquals("Không thể tự chấp nhận lời mời của chính mình", ex.getMessage());
    }

    @Test
    void acceptRequest_whenValid_shouldSetAccepted_andPublishEvent() {
        Friendship f = new Friendship();
        f.setActionUserId(2); // User 2 gửi
        when(friendshipRepository.findFriendship(1, 2)).thenReturn(Optional.of(f));
        when(userRepository.findById(1)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2)).thenReturn(Optional.of(user2));

        String result = friendshipService.acceptRequest(1, 2);

        assertEquals("Đã trở thành bạn bè", result);
        assertEquals("ACCEPTED", f.getStatus());
        assertEquals(1, f.getActionUserId()); // Action được gán lại cho người vừa bấm Accept
        verify(friendshipRepository).save(f);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(NotificationType.ACCEPT_FRIEND, eventCaptor.getValue().getType());
    }

    // ==========================================
    // TEST: XÓA QUAN HỆ / TỪ CHỐI (removeFriendship)
    // ==========================================

    @Test
    void removeFriendship_whenNotFound_shouldThrow() {
        when(friendshipRepository.findFriendship(1, 2)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> friendshipService.removeFriendship(1, 2));
        assertEquals("Không tìm thấy mối quan hệ", ex.getMessage());
    }

    @Test
    void removeFriendship_whenAccepted_shouldReturnHuyKetBan() {
        Friendship f = new Friendship();
        f.setStatus("ACCEPTED");
        when(friendshipRepository.findFriendship(1, 2)).thenReturn(Optional.of(f));

        String result = friendshipService.removeFriendship(1, 2);

        assertEquals("Đã hủy kết bạn", result);
        assertEquals("DELETED", f.getStatus());
        assertEquals(1, f.getActionUserId());
        verify(friendshipRepository).save(f);
        verify(eventPublisher, never()).publishEvent(any()); // Đảm bảo không bắn thông báo khi xóa bạn
    }

    @Test
    void removeFriendship_whenPending_shouldReturnHuyLoiMoi() {
        Friendship f = new Friendship();
        f.setStatus("PENDING");
        when(friendshipRepository.findFriendship(1, 2)).thenReturn(Optional.of(f));

        String result = friendshipService.removeFriendship(1, 2);

        assertEquals("Đã hủy lời mời", result);
        assertEquals("DELETED", f.getStatus());
    }

    @Test
    void removeFriendship_whenDeleted_shouldReturnDaXoaQuanHe() {
        Friendship f = new Friendship();
        f.setStatus("DELETED"); // Cố tình xóa một quan hệ đã xóa
        when(friendshipRepository.findFriendship(1, 2)).thenReturn(Optional.of(f));

        String result = friendshipService.removeFriendship(1, 2);

        assertEquals("Đã xóa quan hệ", result);
    }

    // ==========================================
    // TEST: CÁC HÀM GET & TÌM KIẾM
    // ==========================================

    @Test
    void getFriendshipStatus_whenNotFoundOrDeleted_shouldReturnNone() {
        when(friendshipRepository.findFriendship(1, 2)).thenReturn(Optional.empty());
        FriendshipDTO dto1 = friendshipService.getFriendshipStatus(1, 2);
        assertEquals("NONE", dto1.getStatus());

        Friendship f = new Friendship();
        f.setStatus("DELETED");
        when(friendshipRepository.findFriendship(1, 3)).thenReturn(Optional.of(f));
        FriendshipDTO dto2 = friendshipService.getFriendshipStatus(1, 3);
        assertEquals("NONE", dto2.getStatus());
    }

    @Test
    void getFriendshipStatus_whenExists_shouldReturnStatusAndActionUser() {
        Friendship f = new Friendship();
        f.setStatus("PENDING");
        f.setActionUserId(1);
        when(friendshipRepository.findFriendship(1, 2)).thenReturn(Optional.of(f));

        FriendshipDTO dto = friendshipService.getFriendshipStatus(1, 2);
        assertEquals("PENDING", dto.getStatus());
        assertEquals(1, dto.getActionUserId());
    }

    @Test
    void getSuggestedFriendsList_shouldFilterOutSelfAndExistingRelations() {
        User u3 = new User(); u3.setId(3);
        User u4 = new User(); u4.setId(4); // User 4 không có quan hệ gì, sẽ được gợi ý

        when(userRepository.findAll()).thenReturn(List.of(user1, user2, u3, u4));

        Friendship f1 = new Friendship(); // user1 vs user2 (ACCEPTED)
        f1.setUser1Id(1); f1.setUser2Id(2); f1.setStatus("ACCEPTED");

        Friendship f2 = new Friendship(); // user1 vs u3 (DELETED) -> Bị xóa rồi nên U3 vẫn hiển thị gợi ý
        f2.setUser1Id(1); f2.setUser2Id(3); f2.setStatus("DELETED");

        when(friendshipRepository.findAll()).thenReturn(List.of(f1, f2));

        List<User> suggestions = friendshipService.getSuggestedFriends(1);

        // Kết quả mong muốn: Chỉ còn lại u3 và u4 (Loại trừ bản thân = 1, và bạn bè = 2)
        assertEquals(2, suggestions.size());
        assertTrue(suggestions.stream().anyMatch(u -> u.getId() == 3));
        assertTrue(suggestions.stream().anyMatch(u -> u.getId() == 4));
    }

    @Test
    void getSuggestedFriendsPage_shouldCallRepository() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<User> mockPage = new PageImpl<>(List.of(user2));
        when(userRepository.findSuggestedFriends(1, pageable)).thenReturn(mockPage);

        Page<User> result = friendshipService.getSuggestedFriends(1, 0, 10);
        assertEquals(1, result.getTotalElements());
        verify(userRepository).findSuggestedFriends(1, pageable);
    }

    @Test
    void getFriendRequests_and_getUserFriends_shouldCallRepository() {
        when(friendshipRepository.findPendingRequests(1)).thenReturn(List.of(user2));
        when(friendshipRepository.findAllFriends(1)).thenReturn(List.of(user2));

        List<User> requests = friendshipService.getFriendRequests(1);
        List<User> friends = friendshipService.getUserFriends(1);

        assertEquals(1, requests.size());
        assertEquals(1, friends.size());
        verify(friendshipRepository).findPendingRequests(1);
        verify(friendshipRepository).findAllFriends(1);
    }
    // ==========================================
    // 🟢 BỔ SUNG: CÁC NHÁNH CÒN THIẾU CỦA SEND REQUEST VÀ ACCEPT REQUEST
    // ==========================================

    @Test
    void sendRequest_whenAlreadyPending_shouldThrow() {
        // Lấp nhánh f.getStatus().equals("PENDING") trong điều kiện ||
        when(userRepository.findById(1)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2)).thenReturn(Optional.of(user2));

        Friendship existing = new Friendship();
        existing.setStatus("PENDING");
        when(friendshipRepository.findFriendship(1, 2)).thenReturn(Optional.of(existing));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> friendshipService.sendRequest(1, 2));
        assertEquals("Đã tồn tại mối quan hệ", ex.getMessage());
    }

    @Test
    void acceptRequest_whenUserNotFoundInDb_shouldThrow() {
        Friendship f = new Friendship();
        f.setActionUserId(2); // User 2 gửi lời mời
        when(friendshipRepository.findFriendship(1, 2)).thenReturn(Optional.of(f));
        
        // 🟢 Ép lỗi khi lấy thông tin user từ DB
        when(userRepository.findById(1)).thenReturn(Optional.empty()); 

        assertThrows(Exception.class, () -> friendshipService.acceptRequest(1, 2));
    }

    // ==========================================
    // 🟢 BỔ SUNG: NHÁNH TOÁN TỬ 3 NGÔI TRONG GET SUGGESTED FRIENDS
    // ==========================================

    @Test
    void getSuggestedFriendsList_whenMyIdIsUser2_shouldFilterProperly() {
        User u3 = new User(); 
        u3.setId(3);
        when(userRepository.findAll()).thenReturn(List.of(user1, u3));

        Friendship f1 = new Friendship();
        // 🟢 Cố tình đặt mình (1) ở vị trí User2Id để chạy qua nhánh còn lại của toán tử 3 ngôi
        f1.setUser1Id(3); 
        f1.setUser2Id(1); 
        f1.setStatus("ACCEPTED");

        when(friendshipRepository.findAll()).thenReturn(List.of(f1));

        List<User> suggestions = friendshipService.getSuggestedFriends(1);
        
        // Cả user1 (bản thân) và u3 (đã là bạn bè) đều bị lọc mất
        assertEquals(0, suggestions.size()); 
    }

    // ==========================================
    // 🟢 BỔ SUNG: HÀM GET FRIENDS BY STUDENT CODE (CHƯA TỪNG ĐƯỢC TEST)
    // ==========================================

    @Test
    void getFriendsByStudentCode_whenUserNotFound_shouldThrow() {
        when(userRepository.findByStudentCode("GHOST")).thenReturn(Optional.empty());
        PageRequest pageable = PageRequest.of(0, 10);
        
        RuntimeException ex = assertThrows(RuntimeException.class, 
                () -> friendshipService.getFriendsByStudentCode("GHOST", pageable));
        assertEquals("User with studentCode GHOST not found", ex.getMessage());
    }

    @Test
    void getFriendsByStudentCode_whenValid_shouldReturnDTOPage() {
        when(userRepository.findByStudentCode("1412")).thenReturn(Optional.of(user1));
        PageRequest pageable = PageRequest.of(0, 10);
        
        // Tạo một user bạn bè giả mạo
        User friend = new User();
        friend.setId(2);
        friend.setStudentCode("FRIEND_CODE");
        friend.setFullName("Bạn Tốt");
        friend.setAvatarUrl("avatar.png");
        
        Page<User> friendPage = new PageImpl<>(List.of(friend));
        when(friendshipRepository.findFriends(user1.getId(), pageable)).thenReturn(friendPage);
        
        // Gọi hàm (sẽ tự động gọi luôn hàm private toFriendResponseDTO)
        Page<FriendResponseDTO> result = friendshipService.getFriendsByStudentCode("1412", pageable);
        
        // Xác minh kết quả ánh xạ DTO
        assertEquals(1, result.getTotalElements());
        assertEquals(2, result.getContent().get(0).getId());
        assertEquals("FRIEND_CODE", result.getContent().get(0).getStudentCode());
        assertEquals("Bạn Tốt", result.getContent().get(0).getFullName());
        assertEquals("avatar.png", result.getContent().get(0).getAvatarUrl());
    }
}