package com.example.backend.Chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Query lấy lịch sử chat giữa 2 người
    List<ChatMessage> findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByTimestampAsc(
            Integer senderId1, Integer receiverId1,
            Integer senderId2, Integer receiverId2);

    // Query MỚI: Lấy tất cả tin nhắn liên quan đến 1 user (để lọc ra Inbox)
    @Query("SELECT m FROM ChatMessage m WHERE (m.senderId = :userId OR m.receiverId = :userId) ORDER BY m.timestamp DESC")
    List<ChatMessage> findRecentMessages(@Param("userId") Integer userId);
}