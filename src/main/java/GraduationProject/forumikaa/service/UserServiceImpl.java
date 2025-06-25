package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.RoleDao;
import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.dto.AdminUserDto;
import GraduationProject.forumikaa.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;
    
    @Autowired
    private RoleDao roleDao;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Page<User> findPaginated(String keyword, String status, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> cb.conjunction();

        if (keyword != null && !keyword.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(root.get("email"), "%" + keyword + "%"),
                            cb.like(root.get("username"), "%" + keyword + "%"),
                            cb.like(root.get("firstName"), "%" + keyword + "%"),
                            cb.like(root.get("lastName"), "%" + keyword + "%")
                    )
            );
        }

        if (status != null && !status.isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                if ("active".equalsIgnoreCase(status)) {
                    return cb.isTrue(root.get("enabled"));
                } else if ("banned".equalsIgnoreCase(status)) {
                    return cb.isFalse(root.get("enabled"));
                }
                return cb.conjunction();
            });
        }
        
        return userDao.findAll(spec, pageable);
    }

    @Override
    public void updateUserEnabledStatus(Long userId, boolean enabled) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        user.setEnabled(enabled);
        userDao.save(user);
    }

    @Override
    public void deleteUser(Long userId) {
        userDao.deleteById(userId);
    }

    @Override
    public Optional<User> findById(Long userId) {
        return userDao.findById(userId);
    }

    @Override
    public void save(AdminUserDto userDto) {
        User user;
        if (userDto.getId() != null) {
            user = userDao.findById(userDto.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
        } else {
            user = new User();
        }

        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setEnabled(userDto.isEnabled());
        
        if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        }

        if (userDto.getRoles() != null) {
            user.setRoles(userDto.getRoles());
        }
        
        userDao.save(user);
    }
} 