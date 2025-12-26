package com.example.backend.User;

import lombok.Data;

@Data
public class RegisterRequest {
    // Các trường bắt buộc nhập từ Form
    private String studentCode;
    private String fullName;
    private String email;
    private String password;

    // Trường tùy chọn (có thể null)
    private String className;

    // Nếu bạn muốn cho phép đăng ký kèm Bio hoặc Avatar ngay từ đầu thì thêm vào
    // đây,
    // nhưng thường thì đăng ký chỉ cần các thông tin cơ bản trên.
}