package com.example.backend.User;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Table(name = "security_history")
@Data
public class SecurityHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "browser", length = 100)
    private String browser;

    @Column(name = "device", length = 100)
    private String device;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    @Column(name = "login_time")
    private LocalDateTime loginTime;

    @Column(name = "status", length = 20)
    private String status; // Vd: "SUCCESS", "FAILED"

    @PrePersist
    protected void onCreate() {
        loginTime = LocalDateTime.now();
    }
    // Thêm mã phiên làm việc để đối chiếu với Token
    @Column(name = "session_id", length = 100)
    private String sessionId;

    // Cờ đánh dấu thiết bị này còn được phép hoạt động không
    @Column(name = "is_active")
    private Boolean isActive = true;
}