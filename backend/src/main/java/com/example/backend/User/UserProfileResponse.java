package com.example.backend.User;

public record UserProfileResponse(
        Integer userId,
        String username,
        String fullName,
        String email,
        String avatarUrl,
        String coverPhotoUrl,
        String bio,
        String className,
        String createdAt,
        String joinedAt,
        boolean isSelfProfile,
        String currentAvatarFrame,
        String currentNameColor
) {
}
