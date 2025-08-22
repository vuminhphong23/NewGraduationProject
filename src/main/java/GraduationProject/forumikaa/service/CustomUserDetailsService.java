package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private UserDao userDao;

    @Autowired
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//        User user = userDao.findByUsername(username)
//                .orElseThrow(() -> new   UsernameNotFoundException("User not found with username: " + username));
//
//        List<GrantedAuthority> authorities = user.getRoles().stream()
//                .map(role -> new SimpleGrantedAuthority(role.getName()))
//                .collect(Collectors.toList());
//
//        // Kiểm tra nếu là OAuth2 user (có password đặc biệt)
//        boolean isOAuth2User = "OAUTH2_USER".equals(user.getPassword());
//
//        return new org.springframework.security.core.userdetails.User(
//                user.getUsername(),        // tên đăng nhập
//                user.getPassword(),        // mật khẩu đã mã hoá
//                user.isEnabled(),          // tài khoản có đang hoạt động không
//                true,                      // tài khoản có hết hạn không
//                true,                      // thông tin đăng nhập có hết hạn không
//                true,                      // tài khoản có bị khoá không
//                authorities                // danh sách quyền (roles)
//        );

        return userDao.findByUsername(username)
                .map(u -> new UserDetails() {
                    @Override
                    public Collection<? extends GrantedAuthority> getAuthorities() {
                        return u.getRoles()
                                .stream()
                                .map(r -> new SimpleGrantedAuthority(r.getName()))
                                .toList();
                    }

                    @Override
                    public String getPassword() {
                        return u.getPassword();
                    }

                    @Override
                    public String getUsername() {
                        return u.getUsername();
                    }

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
                        return u.isEnabled();
                    }
                })
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

} 