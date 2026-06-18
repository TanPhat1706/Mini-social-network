package com.example.backend.Item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemResponse {
    private Integer id;
    private String name;
    private String type;
    private String imageUrl;
    private Integer price;
    private String description;
    private Boolean active;
}