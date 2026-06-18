package com.example.backend.Item;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_inventory")
@Data
public class UserInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "item_id", nullable = false)
    private Integer itemId;

    @Column(name = "purchased_at")
    private LocalDateTime purchasedAt;

    @PrePersist
    protected void onCreate() {
        purchasedAt = LocalDateTime.now();
    }
}