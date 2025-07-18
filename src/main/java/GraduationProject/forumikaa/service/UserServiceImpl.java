package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dao.RoleDao;
import GraduationProject.forumikaa.dao.UserDao;
import GraduationProject.forumikaa.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
        //truy vấn động
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
    public Optional<User> findByUsername(String username) {
        return userDao.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userDao.findByEmail(email);
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
    public void save(User user) {
        if (user.getId() != null) {
            User existingUser = userDao.findById(user.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            existingUser.setUsername(user.getUsername());
            existingUser.setEmail(user.getEmail());
            existingUser.setFirstName(user.getFirstName());
            existingUser.setLastName(user.getLastName());
            existingUser.setEnabled(user.isEnabled());
            existingUser.setGender(user.getGender());
            existingUser.setPhone(user.getPhone());
            existingUser.setAddress(user.getAddress());
            existingUser.setBirthDate(user.getBirthDate());
            existingUser.setProfileInfo(user.getProfileInfo());
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
            }
            if (user.getRoles() != null) {
                existingUser.setRoles(user.getRoles());
            }
            userDao.save(existingUser);
        } else {
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }
            userDao.save(user);
        }
    }

    @Override
    public List<User> findAll() {
        return userDao.findAll();
    }

    @Override
    public boolean existsByUsername(String username, Long userId) {
        Optional<User> existingUser = userDao.findByUsername(username);
        return existingUser.isPresent() && !existingUser.get().getId().equals(userId);
    }

    @Override
    public boolean existsByEmail(String email, Long userId) {
        Optional<User> existingUser = userDao.findByEmail(email);
        return existingUser.isPresent() && !existingUser.get().getId().equals(userId);
    }

    @Override
    public boolean existsPhone(String phone, Long userId) {
        Optional<User> existingUser = userDao.findByPhone(phone);
        return existingUser.isPresent() && !existingUser.get().getId().equals(userId);
    }

    @Override
    public boolean checkPassword(String password) {
        return password != null && password.length() >= 6;
    }
} 