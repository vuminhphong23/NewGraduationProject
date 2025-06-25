package GraduationProject.forumikaa.service;

import GraduationProject.forumikaa.dto.AdminUserDto;
import GraduationProject.forumikaa.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserService {
    Page<User> findPaginated(String keyword, String status, Pageable pageable);
    void updateUserEnabledStatus(Long userId, boolean enabled);
    void deleteUser(Long userId);
    Optional<User> findById(Long userId);
    void save(AdminUserDto userDto);
} 