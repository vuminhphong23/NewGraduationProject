package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserService {
    Page<User> findPaginated(String keyword, String status, Pageable pageable);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    void updateUserEnabledStatus(Long userId, boolean enabled);
    void deleteUser(Long userId);
    Optional<User> findById(Long userId);
    void save(User user);
    List<User> findAll();
} 