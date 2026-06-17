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

    // 🟢 ĐÃ CẬP NHẬT: Lọc bỏ những tin nhắn mà requester (người gọi API) đã xoá 1 chiều
    @Query("SELECT m FROM ChatMessage m " +
           "WHERE ((m.senderId = :u1 AND m.receiverId = :u2) " +
           "   OR (m.senderId = :u2 AND m.receiverId = :u1)) " +
           "AND (m.deletedBySenderId IS NULL OR m.deletedBySenderId != :requesterId) " +
           "ORDER BY m.timestamp ASC")
    List<ChatMessage> getHistoryWithRevokeFilter(
            @Param("u1") Integer u1, 
            @Param("u2") Integer u2, 
            @Param("requesterId") Integer requesterId);

    @Query("SELECT m FROM ChatMessage m WHERE (m.senderId = :userId OR m.receiverId = :userId) ORDER BY m.timestamp DESC")
    List<ChatMessage> findRecentMessages(@Param("userId") Integer userId);

    @Modifying
    @Transactional
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.senderId = :senderId AND m.receiverId = :receiverId")
    void markMessagesAsRead(@Param("senderId") Integer senderId, @Param("receiverId") Integer receiverId);
}