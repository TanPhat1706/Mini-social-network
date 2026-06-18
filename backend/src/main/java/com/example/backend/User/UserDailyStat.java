package com.example.backend.User;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "user_daily_stats")
@Data
public class UserDailyStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết với user (Lưu ID cho nhẹ, không cần join bảng User ở đây)
    // Lưu ý: User ID của bạn là Integer nên ở đây mình để Integer cho khớp
    @Column(nullable = false)
    private Integer userId;

    @Column(nullable = false)
    private LocalDate date; // Chỉ lưu ngày (yyyy-MM-dd), không lưu giờ

    // Các bộ đếm hoạt động trong ngày
    private int postCount = 0;    // Số bài đã đăng
    private int likeCount = 0;    // Số bài đã like
    private int commentCount = 0; // Số comment đã viết
    private int shareCount = 0;   // Số bài đã share

    // Constructor mặc định
    public UserDailyStat() {}

    // Constructor tiện lợi để tạo mới nhanh
    public UserDailyStat(Integer userId, LocalDate date) {
        this.userId = userId;
        this.date = date;
        this.postCount = 0;
        this.likeCount = 0;
        this.commentCount = 0;
        this.shareCount = 0;
    }
}
