package com.example.backend.Event; 
import lombok.Data;

@Data
public class TypingEvent {
    private Integer senderId;
    private Integer receiverId;
    private boolean isTyping; // true: đang gõ, false: ngừng gõ
}