package com.example.backend.Item;

import com.example.backend.Enum.CosmeticRarity;
import com.example.backend.Enum.CosmeticTheme;
import com.example.backend.Enum.CosmeticType;
import lombok.Data;

@Data
public class ItemRequest {
    private String code;
    private String name;
    private CosmeticType type;
    private CosmeticTheme theme;
    private CosmeticRarity rarity;
    private String effectKey;
    private Integer price;
    private String description;
    private Integer displayOrder;
}
