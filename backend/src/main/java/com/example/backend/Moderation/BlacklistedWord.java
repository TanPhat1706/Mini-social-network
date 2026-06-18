package com.example.backend.Moderation;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "blacklisted_words")
@Data
@NoArgsConstructor
public class BlacklistedWord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 🟢 Dùng columnDefinition để ép kiểu NVARCHAR khi tạo bảng tự động (DDL)
    // 🟢 Điều này giúp AWS RDS khởi tạo đúng ngay từ đầu mà không cần lệnh SQL tay
    @Column(nullable = false, unique = true, columnDefinition = "NVARCHAR(255) COLLATE Vietnamese_CI_AS")
    private String word;

    public BlacklistedWord(String word) {
        this.word = word.toLowerCase().trim();
    }
}