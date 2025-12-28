package com.example.backend.FriendRequest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.backend.User.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Integer> {

    // Query cũ (giữ nguyên)
    @Query("SELECT f FROM Friendship f WHERE (f.user1Id = :uid1 AND f.user2Id = :uid2) OR (f.user1Id = :uid2 AND f.user2Id = :uid1)")
    Optional<Friendship> findFriendship(Integer uid1, Integer uid2);

    // 1. Tìm những lời mời kết bạn gửi ĐẾN tôi (Tôi là User2, status = PENDING)
    // Join bảng User để lấy thông tin người gửi (User1)
    @Query("SELECT u FROM Friendship f JOIN User u ON f.user1Id = u.id WHERE f.user2Id = :myId AND f.status = 'PENDING'")
    List<User> findPendingRequests(Integer myId);

    // 2. Tìm danh sách bạn bè (Status = ACCEPTED)
    // Phức tạp hơn chút vì bạn bè có thể là User1 hoặc User2
    @Query("SELECT u FROM Friendship f JOIN User u ON (f.user1Id = u.id OR f.user2Id = u.id) " +
            "WHERE (f.user1Id = :myId OR f.user2Id = :myId) " +
            "AND u.id != :myId " + // Loại bỏ chính mình
            "AND f.status = 'ACCEPTED'")
    List<User> findAllFriends(Integer myId);
}