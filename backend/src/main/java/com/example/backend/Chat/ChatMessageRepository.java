package com.example.backend.Chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Lấy lịch sử chat
    List<ChatMessage> findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByTimestampAsc(
            Integer senderId1, Integer receiverId1,
            Integer senderId2, Integer receiverId2);

    // Lấy tin nhắn gần đây cho Inbox
    @Query("SELECT m FROM ChatMessage m WHERE (m.senderId = :userId OR m.receiverId = :userId) ORDER BY m.timestamp DESC")
    List<ChatMessage> findRecentMessages(@Param("userId") Integer userId);

    // --- HÀM MỚI: Đánh dấu tin nhắn từ Partner gửi cho Mình là ĐÃ ĐỌC ---
    @Modifying
    @Transactional
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.senderId = :senderId AND m.receiverId = :receiverId")
    void markMessagesAsRead(@Param("senderId") Integer senderId, @Param("receiverId") Integer receiverId);
}