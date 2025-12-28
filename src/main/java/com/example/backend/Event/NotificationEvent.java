package com.example.backend.Event;

import com.example.backend.Enum.NotificationType;
import com.example.backend.User.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationEvent {
    private User sender;
    private User receiver;
    private NotificationType type;
    private Long entityId;
    private String entityType;
    private String message;
}
