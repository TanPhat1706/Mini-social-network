package com.example.backend.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByStudentCodeOrEmail(String studentCode, String email);

    Optional<User> findByStudentCode(String studentCode); // Dùng cho security load user

    Optional<User> findByStudentCodeAndActive(String studentCode, Boolean active);

    boolean existsByEmail(String email);

    boolean existsByStudentCode(String studentCode);

    // Đếm số lượng user theo trạng thái Active (True/False)
    long countByActive(Boolean active);

    // --- MỚI: TÌM KIẾM NGƯỜI DÙNG ---
    @Query("SELECT u FROM User u WHERE (u.fullName LIKE %:query% OR u.studentCode LIKE %:query%) AND u.active = true")
    List<User> searchUsers(@Param("query") String query);

    List<User> findByRoleIn(List<String> roles);

    User getReferenceById(Long userId);

    Optional<User> findByEmail(String email);

    // Bên trong interface UserRepository:
    @Query("SELECT u FROM User u WHERE u.id != :myId " +
            "AND u.id NOT IN (SELECT f.user1Id FROM Friendship f WHERE f.user2Id = :myId AND f.status != 'DELETED') " +
            "AND u.id NOT IN (SELECT f.user2Id FROM Friendship f WHERE f.user1Id = :myId AND f.status != 'DELETED')")
    Page<User> findSuggestedFriends(@Param("myId") Integer myId, Pageable pageable);
}