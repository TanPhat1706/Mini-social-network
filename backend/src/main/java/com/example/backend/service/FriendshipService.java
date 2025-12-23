package com.example.backend.service;

import com.example.backend.dto.FriendshipDTO;
import com.example.backend.entity.Friendship;
import com.example.backend.entity.User;
import com.example.backend.repository.FriendshipRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FriendshipService {
    @Autowired FriendshipRepository friendshipRepository;
    @Autowired UserRepository userRepository;

    // Gửi lời mời (Xử lý cả việc khôi phục DELETED)
    public String sendRequest(Integer senderId, Integer receiverId) {
        if (senderId.equals(receiverId)) throw new RuntimeException("Không thể kết bạn với chính mình");

        Optional<Friendship> existing = friendshipRepository.findFriendship(senderId, receiverId);

        if (existing.isPresent()) {
            Friendship f = existing.get();
            // Nếu đã là bạn hoặc đang chờ -> Lỗi
            if (f.getStatus().equals("ACCEPTED") || f.getStatus().equals("PENDING")) {
                throw new RuntimeException("Đã tồn tại mối quan hệ");
            }
            // Nếu status là DELETED -> Khôi phục lại thành PENDING
            f.setStatus("PENDING");
            f.setActionUserId(senderId);
            friendshipRepository.save(f);
            return "Đã gửi lại lời mời";
        }

        // Tạo mới hoàn toàn
        Friendship f = new Friendship();
        f.setUser1Id(senderId);
        f.setUser2Id(receiverId);
        f.setStatus("PENDING");
        f.setActionUserId(senderId);
        friendshipRepository.save(f);
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
        return "Đã trở thành bạn bè";
    }

    // Xóa bạn / Hủy lời mời -> SOFT DELETE
    public String removeFriendship(Integer userId, Integer targetId) {
        Friendship f = friendshipRepository.findFriendship(userId, targetId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mối quan hệ"));
        
        f.setStatus("DELETED"); // Chỉ đánh dấu là đã xóa
        f.setActionUserId(userId);
        friendshipRepository.save(f);
        return "Đã xóa quan hệ";
    }

    // Gợi ý bạn bè (Trừ bản thân, bạn bè, và những người đang chờ)
    public List<User> getSuggestedFriends(Integer myId) {
        List<User> allUsers = userRepository.findAll();
        List<Friendship> myRelations = friendshipRepository.findAll();

        // Lấy danh sách ID những người cần loại bỏ (ACCEPTED hoặc PENDING)
        // Những người DELETED thì không loại bỏ -> Vẫn gợi ý lại
        Set<Integer> excludeIds = myRelations.stream()
            .filter(f -> (f.getUser1Id().equals(myId) || f.getUser2Id().equals(myId)))
            .filter(f -> !f.getStatus().equals("DELETED")) 
            .map(f -> f.getUser1Id().equals(myId) ? f.getUser2Id() : f.getUser1Id())
            .collect(Collectors.toSet());

        excludeIds.add(myId); // Loại bỏ chính mình

        return allUsers.stream()
                .filter(u -> !excludeIds.contains(u.getId()))
                .collect(Collectors.toList());
    }

    public FriendshipDTO getFriendshipStatus(Integer myId, Integer targetId) {
        Optional<Friendship> fOpt = friendshipRepository.findFriendship(myId, targetId);
        // Nếu không tìm thấy hoặc đã DELETED -> Coi như chưa kết bạn (NONE)
        if (fOpt.isEmpty() || fOpt.get().getStatus().equals("DELETED")) {
            return new FriendshipDTO("NONE", null);
        }
        Friendship f = fOpt.get();
        return new FriendshipDTO(f.getStatus(), f.getActionUserId());
    }

    // Lấy danh sách lời mời kết bạn
    public List<User> getFriendRequests(Integer myId) {
        return friendshipRepository.findPendingRequests(myId);
    }

    // Lấy danh sách bạn bè
    public List<User> getUserFriends(Integer myId) {
        return friendshipRepository.findAllFriends(myId);
    }
}