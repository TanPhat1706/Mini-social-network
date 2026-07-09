package com.example.backend.Gacha;

import com.example.backend.Item.Item;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GachaResponse {
    private String message;
    private Item wonItem;
    private int remainingPoints;
}