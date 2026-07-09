package com.example.backend.Gacha;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class GachaHistoryItemDTO {
    private Integer itemId;
    private String name;
    private String type;
    private String rarity;
    private String effectKey;
    private LocalDateTime purchasedAt; // Thời gian quay trúng
}