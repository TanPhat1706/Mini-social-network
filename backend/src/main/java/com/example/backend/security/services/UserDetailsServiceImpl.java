package com.example.backend.security.services;

import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    UserRepository userRepository;

    /**
     * Tải thông tin người dùng từ DB dựa trên studentCode.
     * Đây là phương thức cốt lõi được gọi bởi Spring Security khi xác thực.
     * * @param studentCode Mã sinh viên dùng để đăng nhập.
     * 
     * @return Đối tượng UserDetails chứa thông tin user và quyền hạn.
     * @throws UsernameNotFoundException Nếu không tìm thấy user.
     */
    @Override
    @Transactional // Đảm bảo giao dịch DB được quản lý
    public UserDetails loadUserByUsername(String studentCode) throws UsernameNotFoundException {

        // Tìm kiếm User trong DB
        User user = userRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with studentCode: " + studentCode));

        // Chuyển đổi đối tượng User (Entity) thành đối tượng UserDetails (Spring
        // Security)
        return UserDetailsImpl.build(user);
    }
}