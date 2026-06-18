package com.example.backend.Event;

import lombok.Data;

@Data
public class ReadEvent {
    private Integer senderId;   // Người vừa xem tin nhắn
    private Integer receiverId; // Người gửi tin nhắn (người cần nhận thông báo "Đã xem")
}