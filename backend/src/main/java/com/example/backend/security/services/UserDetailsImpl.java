package com.example.backend.security.services;

import com.example.backend.model.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UserDetailsImpl implements UserDetails {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String studentCode;
    private String email;
    private String fullName;
    private String role;

    @JsonIgnore // Không hiển thị mật khẩu trong JSON trả về
    private String passwordHash; // Tên trường phù hợp với Entity User

    private Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(Integer id, String studentCode, String email, String fullName, String role,
            String passwordHash,
            Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.studentCode = studentCode;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.passwordHash = passwordHash;
        this.authorities = authorities;
    }

    /**
     * Hàm xây dựng (Builder) tĩnh để chuyển đổi Entity User sang UserDetailsImpl.
     */
    public static UserDetailsImpl build(User user) {
        // Xử lý vai trò (Role)
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(user.getRole()));

        return new UserDetailsImpl(
                user.getId(),
                user.getStudentCode(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getPasswordHash(),
                authorities);
    }

    // --- Triển khai các phương thức của UserDetails ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return studentCode; // Sử dụng studentCode làm Username cho Spring Security
    }

    // Các getters bổ sung (cần thiết để lấy ID, Email khi tạo JWT/Refresh Token)
    public Integer getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    // Các phương thức kiểm tra trạng thái
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true; // Bạn có thể dùng user.getActive() ở đây nếu muốn kiểm tra trạng thái active
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserDetailsImpl user = (UserDetailsImpl) o;
        return Objects.equals(id, user.id);
    }
}