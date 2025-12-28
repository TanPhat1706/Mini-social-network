package com.example.backend.FriendRequest;

import com.example.backend.Enum.NotificationType;
import com.example.backend.Event.NotificationEvent; // Import Event
import com.example.backend.User.User;
import com.example.backend.User.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher; // Import Publisher
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FriendshipService {
    @Autowired
    private FriendshipRepository friendshipRepository;
    @Autowired
    private UserRepository userRepository;
    
    // --- THAY VÌ Inject Repo/Socket, TA DÙNG EVENT PUBLISHER ---
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // Gửi lời mời (Xử lý cả việc khôi phục DELETED)
    public String sendRequest(Integer senderId, Integer receiverId) {
        if (senderId.equals(receiverId))
            throw new RuntimeException("Không thể kết bạn với chính mình");

        User sender = userRepository.findById(senderId).orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = userRepository.findById(receiverId).orElseThrow(() -> new RuntimeException("Receiver not found"));

        Optional<Friendship> existing = friendshipRepository.findFriendship(senderId, receiverId);

        if (existing.isPresent()) {
            Friendship f = existing.get();
            if (f.getStatus().equals("ACCEPTED") || f.getStatus().equals("PENDING")) {
                throw new RuntimeException("Đã tồn tại mối quan hệ");
            }
            // Khôi phục DELETED -> PENDING
            f.setStatus("PENDING");
            f.setActionUserId(senderId);
            friendshipRepository.save(f);

            // ---> BẮN EVENT (Giống PostService)
            // NotificationService sẽ bắt sự kiện này -> Lưu DB -> Gửi Socket
            eventPublisher.publishEvent(new NotificationEvent(
                sender,          // Người gửi (Sender)
                receiver,        // Người nhận (Receiver)
                NotificationType.FRIEND_REQUEST,
                Long.valueOf(sender.getId()), // EntityID (Click vào sẽ dẫn tới trang cá nhân người gửi)
                "USER",          // EntityType
                "đã gửi lại lời mời kết bạn." // Message
            ));
            
            return "Đã gửi lại lời mời";
        }

        // Tạo mới hoàn toàn
        Friendship f = new Friendship();
        f.setUser1Id(senderId);
        f.setUser2Id(receiverId);
        f.setStatus("PENDING");
        f.setActionUserId(senderId);
        friendshipRepository.save(f);

        // ---> BẮN EVENT
        eventPublisher.publishEvent(new NotificationEvent(
            sender,
            receiver,
            NotificationType.FRIEND_REQUEST,
            Long.valueOf(sender.getId()), 
            "USER",
            "đã gửi lời mời kết bạn."
        ));

        return "Đã gửi lời mời";
    }

    public String acceptRequest(Integer userId, Integer targetId) {
        Friendship f = friendshipRepository.findFriendship(userId, targetId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lời mời"));

        if (f.getActionUserId().equals(userId)) {
            throw new RuntimeException("Không thể tự chấp nhận lời mời của chính mình");
        }

        f.setStatus("ACCEPTED");
        f.setActionUserId(userId);
        friendshipRepository.save(f);

        // Lấy thông tin user
        User me = userRepository.findById(userId).orElseThrow();        // Người chấp nhận
        User friend = userRepository.findById(targetId).orElseThrow();  // Người gửi lời mời trước đó

        // ---> BẮN EVENT: Thông báo cho người kia biết
        eventPublisher.publishEvent(new NotificationEvent(
            me,              // Sender (là mình)
            friend,          // Receiver (là bạn)
            NotificationType.ACCEPT_FRIEND,
            Long.valueOf(me.getId()), 
            "USER",
            "đã chấp nhận lời mời kết bạn."
        ));

        return "Đã trở thành bạn bè";
    }

    public String removeFriendship(Integer userId, Integer targetId) {
        Friendship f = friendshipRepository.findFriendship(userId, targetId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mối quan hệ"));

        // Kiểm tra xem đang làm hành động gì để log hoặc xử lý logic phụ (nếu cần)
        String oldStatus = f.getStatus();
        
        f.setStatus("DELETED");
        f.setActionUserId(userId);
        friendshipRepository.save(f);

        // TUYỆT ĐỐI KHÔNG BẮN EVENT ACCEPT Ở ĐÂY
        // Nếu muốn, em có thể bắn event REJECT (nhưng thường MXH không làm vậy)
        
        if (oldStatus.equals("ACCEPTED")) {
            return "Đã hủy kết bạn";
        } else if (oldStatus.equals("PENDING")) {
            // userId là người gửi -> Đang Hủy lời mời
            // userId là người nhận -> Đang Từ chối
            return "Đã hủy lời mời"; 
        }
        
        return "Đã xóa quan hệ";
    }

    public List<User> getSuggestedFriends(Integer myId) {
        List<User> allUsers = userRepository.findAll();
        List<Friendship> myRelations = friendshipRepository.findAll();
        Set<Integer> excludeIds = myRelations.stream()
                .filter(f -> (f.getUser1Id().equals(myId) || f.getUser2Id().equals(myId)))
                .filter(f -> !f.getStatus().equals("DELETED"))
                .map(f -> f.getUser1Id().equals(myId) ? f.getUser2Id() : f.getUser1Id())
                .collect(Collectors.toSet());
        excludeIds.add(myId); 
        return allUsers.stream().filter(u -> !excludeIds.contains(u.getId())).collect(Collectors.toList());
    }

    public FriendshipDTO getFriendshipStatus(Integer myId, Integer targetId) {
        Optional<Friendship> fOpt = friendshipRepository.findFriendship(myId, targetId);
        if (fOpt.isEmpty() || fOpt.get().getStatus().equals("DELETED")) {
            return new FriendshipDTO("NONE", null);
        }
        Friendship f = fOpt.get();
        return new FriendshipDTO(f.getStatus(), f.getActionUserId());
    }

    public List<User> getFriendRequests(Integer myId) {
        return friendshipRepository.findPendingRequests(myId);
    }

    public List<User> getUserFriends(Integer myId) {
        return friendshipRepository.findAllFriends(myId);
    }
}