package com.example.backend.User;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByHashedToken(String hashedToken);
    List<PasswordResetToken> findByUserId(Integer userId);
    void deleteByUserId(Integer userId);
}
