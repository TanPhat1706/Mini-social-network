package com.example.backend.FriendRequest;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FriendshipDTO {
    private String status;
    private Integer actionUserId;
}