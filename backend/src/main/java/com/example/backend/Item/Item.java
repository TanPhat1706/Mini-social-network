package com.example.backend.Item;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "items")
@Data
public class Item {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Nationalized
    @Column(nullable = false, columnDefinition = "nvarchar(100)")
    private String name; // Tên vật phẩm (VD: "Viền Rồng Vàng", "Viền Neon")

    @Nationalized
    @Column(nullable = false, columnDefinition = "nvarchar(50)")
    private String type; // Phân loại (VD: "AVATAR_FRAME", "TITLE", "NAME_COLOR")

    @Column(name = "image_url", length = 255)
    private String imageUrl; // Link chứa ảnh viền (ảnh PNG trong suốt)

    @Column(nullable = false)
    private Integer price; // Giá bán tính bằng điểm VPTL

    @Nationalized
    @Column(columnDefinition = "nvarchar(500)")
    private String description; // Mô tả vật phẩm

    private Boolean active; // Bật/tắt việc bày bán trên Shop

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (active == null) active = true; // Mặc định tạo ra là được bán luôn
    }
}