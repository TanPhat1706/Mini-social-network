package com.example.backend.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByStudentCodeOrEmail(String studentCode, String email);
    Optional<User> findByStudentCode(String studentCode); // Dùng cho security load user

    boolean existsByEmail(String email);

    boolean existsByStudentCode(String studentCode);
    //  Đếm số lượng user theo trạng thái Active (True/False)
    long countByActive(Boolean active);

    List<User> findByRoleIn(List<String> roles);

    User getReferenceById(Long userId);

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE (u.fullName LIKE %:query% OR u.studentCode LIKE %:query%) and u.active = true")
    List<User> searchUsers(String query);
}