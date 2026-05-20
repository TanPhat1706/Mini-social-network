package com.example.backend.User;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {
    private final UserProfileService userProfileService;

    @GetMapping("/{studentCode}/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @PathVariable("studentCode") String studentCode,
            Authentication authentication
    ) {
        String viewerStudentCode = authentication == null ? null : authentication.getName();
        UserProfileResponse profile = userProfileService.getProfile(viewerStudentCode, studentCode);
        return ResponseEntity.ok(profile);
    }
}
