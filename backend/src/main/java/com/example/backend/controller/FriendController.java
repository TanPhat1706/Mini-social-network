package com.example.backend.controller;

import com.example.backend.dto.FriendshipDTO;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.FriendshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/api/auth/friends") // Khớp với cấu hình axios cũ của bạn
public class FriendController {
    @Autowired
    FriendshipService friendshipService;
    @Autowired
    UserRepository userRepository;

    private Integer getCurrentUserId() {
        String studentCode = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByStudentCode(studentCode).get().getId();
    }

    @PostMapping("/add/{targetId}")
    public ResponseEntity<?> send(@PathVariable Integer targetId) {
        return ResponseEntity
                .ok(Collections.singletonMap("message", friendshipService.sendRequest(getCurrentUserId(), targetId)));
    }

    @PostMapping("/accept/{targetId}")
    public ResponseEntity<?> accept(@PathVariable Integer targetId) {
        return ResponseEntity
                .ok(Collections.singletonMap("message", friendshipService.acceptRequest(getCurrentUserId(), targetId)));
    }

    @DeleteMapping("/remove/{targetId}")
    public ResponseEntity<?> remove(@PathVariable Integer targetId) {
        return ResponseEntity.ok(
                Collections.singletonMap("message", friendshipService.removeFriendship(getCurrentUserId(), targetId)));
    }

    @GetMapping("/status/{targetId}")
    public ResponseEntity<?> getStatus(@PathVariable Integer targetId) {
        return ResponseEntity.ok(friendshipService.getFriendshipStatus(getCurrentUserId(), targetId));
    }

    @GetMapping("/suggested")
    public ResponseEntity<?> getSuggested() {
        return ResponseEntity.ok(friendshipService.getSuggestedFriends(getCurrentUserId()));
    }

    @GetMapping("/requests")
    public ResponseEntity<?> getRequests() {
        return ResponseEntity.ok(friendshipService.getFriendRequests(getCurrentUserId()));
    }

    @GetMapping("/list")
    public ResponseEntity<?> getFriendsList() {
        return ResponseEntity.ok(friendshipService.getUserFriends(getCurrentUserId()));
    }
}