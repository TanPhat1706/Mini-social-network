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

        return new UserProfileResponse(
                targetUser.getId(),
                targetUser.getStudentCode(),
                targetUser.getFullName(),
                targetUser.getEmail(),
                targetUser.getAvatarUrl(),
                targetUser.getBio(),
                targetUser.getCreatedAt().format(formatter),
                isSelfProfile);
    }
}
