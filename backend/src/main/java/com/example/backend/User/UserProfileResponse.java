package com.example.backend.User;

public record UserProfileResponse(
        Integer userId,
        String username,
        String fullName,
        String email,
        String avatarUrl,
        String bio,
        String joinedAt,
        boolean isSelfProfile
) {
}
