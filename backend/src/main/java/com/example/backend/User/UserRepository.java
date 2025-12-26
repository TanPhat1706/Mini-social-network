package com.example.backend.User;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByStudentCodeOrEmail(String studentCode, String email);

    Optional<User> findByStudentCode(String studentCode); // Dùng cho security load user

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByStudentCode(String studentCode);
}