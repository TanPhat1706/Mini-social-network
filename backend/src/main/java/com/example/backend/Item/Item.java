package com.example.backend.Item;

import com.example.backend.BaseEntity.BaseEntity;
import com.example.backend.Enum.CosmeticRarity;
import com.example.backend.Enum.CosmeticTheme;
import com.example.backend.Enum.CosmeticType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Nationalized
    @Column(nullable = false, unique = true, columnDefinition = "nvarchar(100)")
    private String code;

    @Nationalized
    @Column(nullable = false, columnDefinition = "nvarchar(100)")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CosmeticType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CosmeticTheme theme;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CosmeticRarity rarity;

    @Column(name = "effect_key", nullable = false, length = 100)
    private String effectKey;

    @Column(nullable = false)
    private Integer price;

    @Nationalized
    @Column(columnDefinition = "nvarchar(500)")
    private String description;

    @Column(name = "display_order")
    private Integer displayOrder;

    private Boolean active;

    @PrePersist
    protected void onCreate() {
        if (active == null) {
            active = true;
        }
        if (displayOrder == null) {
            displayOrder = 0;
        }
    }
}
