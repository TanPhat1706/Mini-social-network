package com.example.backend.User;

import lombok.RequiredArgsConstructor;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String viewerStudentCode, String targetStudentCode) {
        User targetUser = userRepository.findByStudentCodeAndActive(targetStudentCode, true)
                .orElseThrow(() -> new UserProfileNotFoundException("Target user not found or inactive"));

        boolean isSelfProfile = viewerStudentCode != null && viewerStudentCode.equals(targetUser.getStudentCode());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yyyy");
        String createdAt = targetUser.getCreatedAt() != null ? targetUser.getCreatedAt().toString() : null;
        String joinedAt = targetUser.getCreatedAt() != null ? targetUser.getCreatedAt().format(formatter) : null;

        return new UserProfileResponse(
                targetUser.getId(),
                targetUser.getStudentCode(),
                targetUser.getFullName(),
                targetUser.getEmail(),
                targetUser.getAvatarUrl(),
                targetUser.getCoverPhotoUrl(),
                targetUser.getBio(),
                targetUser.getClassName(),
                createdAt,
                joinedAt,
                isSelfProfile,
                targetUser.getCurrentAvatarFrame(), // Truyền viền vào
                targetUser.getCurrentNameColor() // Truyền màu tên vào
        );
    }
}
