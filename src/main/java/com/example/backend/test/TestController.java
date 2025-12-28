package com.example.backend.test;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.User.User;
import com.example.backend.User.UserResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test")
public class TestController {
    @PostMapping("/principal")
    public void testConnection(Principal principal) {
        System.out.println(principal);
    }

    @GetMapping
    public UserResponse testUser(@AuthenticationPrincipal User user) {
        return UserResponse.builder()
            .id(user.getId())
            .fullName(user.getFullName())
            .avatarUrl(user.getAvatarUrl())
            .studentCode(user.getStudentCode())
            .build();
    }
}
