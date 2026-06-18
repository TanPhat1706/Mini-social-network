package com.example.backend.Item;

import lombok.Data;

@Data
public class ItemRequest {
    private String name;
    private String type; // Ví dụ: "AVATAR_FRAME"
    private String imageUrl; // Link ảnh viền trong suốt
    private Integer price; // Giá bán (VPTL points)
    private String description;
}