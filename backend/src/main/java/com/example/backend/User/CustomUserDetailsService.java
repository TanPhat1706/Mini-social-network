package com.example.backend.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String studentCode) throws UsernameNotFoundException {
        User user = userRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // 🟢 CẬP NHẬT: Map role từ DB (STUDENT/ADMIN) thành Quyền của Spring
        // Lưu ý: Spring thường dùng tiền tố "ROLE_" cho các vai trò
        return new org.springframework.security.core.userdetails.User(
                user.getStudentCode(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
    }
}