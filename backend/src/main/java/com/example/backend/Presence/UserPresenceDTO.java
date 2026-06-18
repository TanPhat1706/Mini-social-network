package com.example.backend.Presence;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserPresenceDTO {
    private String studentCode;
    private boolean isOnline;
    private LocalDateTime lastSeen;
}