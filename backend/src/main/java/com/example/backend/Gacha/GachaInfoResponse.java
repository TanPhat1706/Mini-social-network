package com.example.backend.Gacha;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class GachaInfoResponse {
    private String theme;
    private int totalPoolSize;       // Tổng số vật phẩm trong rương (vd: 60)
    private int ownedCount;          // Số vật phẩm user đã sở hữu
    private int remainingForGuarantee; // Còn bao nhiêu lượt chắc chắn nổ hết (Pity)
    private Map<String, Double> currentRates; // Tỷ lệ % thực tế ở thời điểm hiện tại
}