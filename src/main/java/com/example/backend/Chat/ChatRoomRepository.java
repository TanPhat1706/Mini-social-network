package com.example.backend.Chat;

import com.example.backend.Chat.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findBySenderIdAndRecipientId(Integer senderId, Integer recipientId);
}