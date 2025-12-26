package com.example.backend.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.ArrayList;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String studentCode) throws UsernameNotFoundException {
        // Ở đây token lưu studentCode, nên ta tìm theo studentCode
        User user = userRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new org.springframework.security.core.userdetails.User(
                user.getStudentCode(),
                user.getPassword(),
                new ArrayList<>() // Nếu có quyền (Role) thì thêm vào đây
        );
    }
}