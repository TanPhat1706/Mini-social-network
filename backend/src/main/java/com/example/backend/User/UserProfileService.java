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

        // 🟢 CẬP NHẬT: Truyền thêm 2 tham số vào constructor
        return new UserProfileResponse(
                targetUser.getId(),
                targetUser.getStudentCode(),
                targetUser.getFullName(),
                targetUser.getEmail(),
                targetUser.getAvatarUrl(),
                targetUser.getBio(),
                targetUser.getCreatedAt().format(formatter),
                isSelfProfile,
                targetUser.getCurrentAvatarFrame(), // Truyền viền vào
                targetUser.getCurrentNameColor() // Truyền màu tên vào
        );
    }
}
