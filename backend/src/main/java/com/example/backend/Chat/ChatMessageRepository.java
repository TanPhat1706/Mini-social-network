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

       // 🟢 ĐÃ CẬP NHẬT: Lọc bỏ những tin nhắn mà requester (người gọi API) đã xoá 1
       // chiều
       @Query("SELECT m FROM ChatMessage m " +
                     "WHERE ((m.senderId = :u1 AND m.receiverId = :u2) " +
                     "   OR (m.senderId = :u2 AND m.receiverId = :u1)) " +
                     "AND (m.deletedBySenderId IS NULL OR m.deletedBySenderId != :requesterId) " +
                     "ORDER BY m.timestamp ASC")
       List<ChatMessage> getHistoryWithRevokeFilter(
                     @Param("u1") Integer u1,
                     @Param("u2") Integer u2,
                     @Param("requesterId") Integer requesterId);

       // 🟢 ĐÃ NÂNG CẤP LOGIC: Tự động "bảo kê" tin nhắn do chính mình gửi thành ĐÃ ĐỌC (true)
       @Query("SELECT new com.example.backend.Chat.ChatConversationDTO(" +
                     "  CASE WHEN m.senderId = :userId THEN m.receiverId ELSE m.senderId END, " +
                     "  u.studentCode, " +
                     "  u.fullName, " +
                     "  u.avatarUrl, " +
                     "  m.content, " +
                     "  m.timestamp, " +
                     "  CASE WHEN m.senderId = :userId THEN true ELSE m.isRead END, " + // 🟢 PHÉP THUẬT NẰM Ở DÒNG NÀY
                     "  u.currentAvatarFrame, " +
                     "  u.currentNameColor) " +
                     "FROM ChatMessage m " +
                     "JOIN User u ON u.id = (CASE WHEN m.senderId = :userId THEN m.receiverId ELSE m.senderId END) " +
                     "WHERE (m.senderId = :userId OR m.receiverId = :userId) " +
                     "AND m.id IN (SELECT MAX(m2.id) FROM ChatMessage m2 WHERE (m2.senderId = :userId OR m2.receiverId = :userId) GROUP BY CASE WHEN m2.senderId = :userId THEN m2.receiverId ELSE m2.senderId END) "
                     +
                     "ORDER BY m.timestamp DESC")
       List<ChatConversationDTO> findRecentMessages(@Param("userId") Integer userId);

       @Modifying
       @Transactional
       @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.senderId = :senderId AND m.receiverId = :receiverId")
       void markMessagesAsRead(@Param("senderId") Integer senderId, @Param("receiverId") Integer receiverId);
}