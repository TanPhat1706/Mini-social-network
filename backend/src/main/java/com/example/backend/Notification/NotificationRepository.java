package com.example.backend.Notification;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByReceiverId(Long receiverId, Pageable pageable);

    List<Notification> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.receiver.id = :receiverId AND n.isRead = false")
    void markAllAsReadByReceiverId(@Param("receiverId") Long receiverId);
}
