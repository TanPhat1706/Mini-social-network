package com.example.backend.repository; // Package chính xác theo yêu cầu

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.model.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * Tìm kiếm một User dựa trên student_code.
     * Cần thiết cho chức năng Đăng nhập.
     *
     * @param studentCode Mã sinh viên
     * @return Optional<User>
     */
    Optional<User> findByStudentCode(String studentCode);

    /**
     * Tìm kiếm một User dựa trên email.
     *
     * @param email Địa chỉ email
     * @return Optional<User>
     */
    Optional<User> findByEmail(String email);

    /**
     * Kiểm tra xem student_code đã tồn tại trong hệ thống chưa.
     * Cần thiết cho chức năng Đăng ký để tránh trùng lặp.
     *
     * @param studentCode Mã sinh viên
     * @return Boolean (true nếu tồn tại)
     */
    Boolean existsByStudentCode(String studentCode);

    /**
     * Kiểm tra xem email đã tồn tại trong hệ thống chưa.
     * Cần thiết cho chức năng Đăng ký.
     *
     * @param email Địa chỉ email
     * @return Boolean (true nếu tồn tại)
     */
    Boolean existsByEmail(String email);

    // Spring Data JPA sẽ tự động tạo các câu truy vấn SQL (Query) từ tên phương
    // thức.
}